import copy
import json
import os
import tempfile
import unittest
from urllib.parse import urlsplit, parse_qs
from concurrent.futures import ThreadPoolExecutor
from loaded_proxy import Workflow, RequestError, response, payload, BASE

LINE = dict(shipmentId=71, orderId=72, orderLineId=73, ordered=10, toShip=8, shortQuantity=2)
CONFIRM = dict(physicallyLoaded=True, acceptShort=True, lines=[LINE])


class Native:
    def __init__(self):
        self.status = "PLANNED"
        self.visit = dict(visit_id=1, trailer_id=2, load_id=3, state="AT_DOCK",
                          revision=1, load_status="PLANNED")
        self.writes = []
        self.lines = [copy.deepcopy(LINE)]
        self.fail_departure = False
        self.lose_shipping = False
        self.denied = False
        self.pending = []
        self.inventory = 8

    def __call__(self, method, url, headers, body):
        if self.denied:
            return 403, {}, b'{"message":"Access denied"}'
        path = urlsplit(url).path
        if method == "GET":
            if path == "/api/layout/locationgroups":
                return response([{"id":10,"name":"SHIP_DCK","locationGroupType":{"dock":True}}])
            if path == "/api/layout/locations":
                return response([{"id":20,"code":"Dock 10","enabled":True}])
            if path.endswith("/yard-visits"):
                history = parse_qs(urlsplit(url).query).get("history") == ["true"]
                visit = dict(self.visit, load_status=self.status)
                return response([visit] if history == (visit["state"] == "DISPATCHED") else [])
            if path.endswith("/shipping"):
                return response(dict(loadId=3, status=self.status, lines=self.lines, pendingOrders=self.pending))
            if path.endswith("/trailer-appointments"):
                return response([dict(id=3, status=self.status)])
            if path.endswith("/stops"):
                return response([dict(shipments=[dict(id=71, status="DISPATCHED" if self.status == "COMPLETED" else "STAGED")])])
        if method == "POST":
            self.writes.append(path)
            if path.endswith("/confirm-loaded"):
                if self.status != "COMPLETED":
                    self.inventory -= 8
                    self.status = "COMPLETED"
                if self.lose_shipping:
                    self.lose_shipping = False
                    raise TimeoutError()
                return response(dict(status="COMPLETED"))
            if path.endswith("/dispatch"):
                if self.fail_departure:
                    self.fail_departure = False
                    return 409, {}, b'{"message":"Departure failed"}'
                if self.status != "COMPLETED":
                    return 409, {}, b'{"message":"Not shipped"}'
                self.visit["state"] = "DISPATCHED"
                self.visit["revision"] += 1
                return response(self.visit)
        return response({})


class Tests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = os.path.join(self.temp.name, "state.sqlite3")
        self.native = Native()
        self.workflow = Workflow(self.db, self.native)

    def tearDown(self):
        self.temp.cleanup()

    def request(self, method, path, body=None):
        return self.workflow.handle(method, path + "?warehouseId=9", {}, json.dumps(body).encode() if body else b"")

    def confirm(self, body=None):
        return self.request("POST", BASE + "/3/shipping/confirm-loaded", body or CONFIRM)

    def dispatch(self):
        return self.request("POST", BASE + "/yard-visits/1/dispatch", dict(revision=1, physicallyDeparted=True))

    def test_loading_is_durable_and_does_not_ship(self):
        self.assertEqual(payload(self.confirm())["status"], "LOADED")
        self.assertEqual(self.native.writes, [])
        self.assertEqual(self.native.inventory, 8)
        self.workflow = Workflow(self.db, self.native)
        for path in (BASE + "/yard-visits", "/api/common/trailer-appointments"):
            data = payload(self.request("GET", path))[0]
            self.assertEqual(data.get("load_status", data.get("status")), "LOADED")
        self.assertEqual(payload(self.request("GET", "/api/outbound/stops"))[0]["shipments"][0]["status"], "LOADED")
        self.assertEqual(self.native.status, "PLANNED")

    def test_dispatch_ships_once_then_departs(self):
        self.confirm()
        payload(self.dispatch())
        self.assertEqual(self.native.inventory, 0)
        self.assertEqual(self.native.visit["state"], "DISPATCHED")
        payload(self.dispatch())
        self.assertEqual(len(self.native.writes), 2)

    def test_restart_after_unknown_shipping_result(self):
        self.confirm()
        self.native.lose_shipping = True
        with self.assertRaises(TimeoutError):
            self.dispatch()
        self.workflow = Workflow(self.db, self.native)
        payload(self.dispatch())
        self.assertEqual(self.native.inventory, 0)
        self.assertEqual(len(self.native.writes), 2)

    def test_retry_after_departure_failure(self):
        self.confirm()
        self.native.fail_departure = True
        with self.assertRaises(RequestError):
            self.dispatch()
        self.workflow = Workflow(self.db, self.native)
        payload(self.dispatch())
        self.assertEqual(self.native.inventory, 0)
        self.assertEqual(sum(p.endswith("confirm-loaded") for p in self.native.writes), 1)

    def test_concurrent_dispatch_is_serialized(self):
        self.confirm()
        with ThreadPoolExecutor(2) as executor:
            results = list(executor.map(lambda _: self.dispatch(), range(2)))
        self.assertEqual(len(results), 2)
        self.assertEqual(len(self.native.writes), 2)

    def test_legacy_completed_load_does_not_ship_again(self):
        self.native.status = "COMPLETED"
        self.native.inventory = 0
        payload(self.dispatch())
        self.assertEqual(self.native.writes, [BASE + "/yard-visits/1/dispatch"])
        self.assertEqual(self.workflow.records("9"), {})

    def test_legacy_get_preserves_data_without_creating_records(self):
        self.native.status = "COMPLETED"
        before = self.native("GET", BASE + "/yard-visits?warehouseId=9", {}, b"")
        after = self.request("GET", BASE + "/yard-visits")
        self.assertEqual(payload(before), payload(after))
        self.assertEqual(self.workflow.records("9"), {})
        self.assertEqual(self.native.writes, [])

    def test_no_confirmation_no_shipping(self):
        with self.assertRaises(RequestError):
            self.dispatch()
        self.assertEqual(self.native.writes, [])

    def test_load_changes_prevent_shipping(self):
        self.confirm()
        self.native.lines[0].update(toShip=7, shortQuantity=3)
        with self.assertRaises(RequestError):
            self.dispatch()
        self.assertEqual(self.native.writes, [])
        updated = dict(CONFIRM, lines=self.native.lines)
        self.confirm(updated)
        payload(self.dispatch())

    def test_permission_revoked_blocks_dispatch(self):
        self.confirm()
        self.native.denied = True
        with self.assertRaises(RequestError):
            self.dispatch()
        self.assertEqual(self.native.writes, [])

    def test_stale_visit_and_missing_departure_consent(self):
        self.confirm()
        for body in (dict(revision=99, physicallyDeparted=True), dict(revision=1, physicallyDeparted=False)):
            with self.assertRaises(RequestError):
                self.request("POST", BASE + "/yard-visits/1/dispatch", body)
        self.assertEqual(self.native.writes, [])

    def test_loaded_assignments_locked(self):
        self.confirm()
        for path in (BASE + "/3/shipping/shipments", BASE + "/yard-visits/1/assign-load"):
            with self.assertRaises(RequestError):
                self.request("POST", path, {"loadId": 8})
        self.assertEqual(self.native.writes, [])

    def test_plain_text_authentication_failure_is_preserved(self):
        self.workflow.upstream = lambda *args: (403, {"Content-Type": "text/plain"}, b"Forbidden")
        self.assertEqual(self.request("GET", BASE + "/yard-visits")[0], 403)
        with self.assertRaises(RequestError) as caught:
            self.confirm()
        self.assertEqual(caught.exception.status, 403)
        self.assertEqual(self.workflow.records("9"), {})

    def test_dock_maintenance_persists_and_blocks_both_assignment_paths(self):
        before = copy.deepcopy(self.native.visit)
        result = self.request("POST", BASE + "/dock-availability/20",
                              {"revision":0,"unavailable":True,"reason":"Door repair"})
        self.assertEqual(payload(result)["revision"], 1)
        self.assertEqual(self.native.visit, before)
        self.workflow = Workflow(self.db, self.native)
        records = payload(self.request("GET", BASE + "/dock-availability"))
        self.assertEqual(records[0]["reason"], "Door repair")
        for path in (BASE + "/yard-visits/1/assign-dock", BASE + "/yard-visits/1/assign-and-arrive", BASE + "/3/dock-assignment"):
            with self.assertRaises(RequestError):
                self.request("POST", path, {"dockId":20,"revision":1})
        self.assertEqual(self.native.writes, [])
        self.request("POST", BASE + "/dock-availability/20",
                     {"revision":1,"unavailable":False,"reason":""})
        self.workflow.require_available("9",20)
        self.assertEqual(self.workflow.dock_records("9")[0]["unavailable"], 0)

    def test_dock_maintenance_rejects_stale_and_wrong_warehouse_docks(self):
        self.request("POST", BASE + "/dock-availability/20",
                     {"revision":0,"unavailable":True,"reason":"Repair"})
        with self.assertRaises(RequestError):
            self.request("POST", BASE + "/dock-availability/20",
                         {"revision":0,"unavailable":False,"reason":""})
        with self.assertRaises(RequestError):
            self.request("POST", BASE + "/dock-availability/999",
                         {"revision":0,"unavailable":True,"reason":""})
        self.assertEqual(self.workflow.dock_records("10"), [])
        self.assertEqual(len(self.workflow.dock_records("9")), 1)

    def test_dock_maintenance_requires_permission(self):
        self.native.denied = True
        with self.assertRaises(RequestError):
            self.request("POST", BASE + "/dock-availability/20",
                         {"revision":0,"unavailable":True,"reason":""})
        self.assertEqual(self.workflow.dock_records("9"), [])

    def test_invalid_loading_confirmation(self):
        for body in (dict(CONFIRM, physicallyLoaded=False), dict(CONFIRM, acceptShort=False),
                     dict(CONFIRM, lines=[LINE, LINE]), dict(CONFIRM, lines=[])):
            with self.assertRaises(RequestError):
                self.confirm(body)
        self.native.pending = [{"orderId": 72}]
        with self.assertRaises(RequestError):
            self.confirm()
        self.assertEqual(self.workflow.records("9"), {})

if __name__ == "__main__":
    unittest.main()

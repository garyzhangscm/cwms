"""Durable loading acknowledgements and deferred native shipping."""
import copy
import fcntl
import http.client
import json
import os
import re
import sqlite3
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlencode, urlsplit

BASE = "/api/outbound/loads"
LINE_KEYS = ("shipmentId", "orderId", "orderLineId", "ordered", "toShip", "shortQuantity")


class RequestError(Exception):
    def __init__(self, message, status=409):
        self.message, self.status = message, status


def payload(response):
    status, _, body = response
    try:
        value = json.loads(body)
    except (ValueError, TypeError):
        raise RequestError("Authentication or upstream request failed." if status >= 400 else "The upstream service returned an unreadable response.", status if status >= 400 else 502)
    if not 200 <= status < 300:
        raise RequestError(value.get("message", "The upstream request failed."), status)
    if isinstance(value, dict) and value.get("result") not in (None, 0):
        raise RequestError(value.get("message", "The upstream request failed."))
    return value.get("data", value) if isinstance(value, dict) else value


def response(data):
    return 200, {"Content-Type": "application/json"}, json.dumps({"result": 0, "data": data}).encode()


def replace_payload(original, data):
    value = json.loads(original[2])
    if isinstance(value, dict) and "data" in value:
        value["data"] = data
    else:
        value = data
    return original[0], {"Content-Type": "application/json"}, json.dumps(value).encode()


def canonical_lines(lines):
    if not isinstance(lines, list) or not 1 <= len(lines) <= 500:
        raise RequestError("Review the Load's shipment quantities before confirming.")
    values = []
    seen = set()
    for line in lines:
        if not isinstance(line, dict) or any(type(line.get(k)) is not int for k in LINE_KEYS):
            raise RequestError("Invalid shipment quantities.")
        row = tuple(line[k] for k in LINE_KEYS)
        shipment, order, order_line, ordered, to_ship, short = row
        if min(shipment, order, order_line, ordered, to_ship) <= 0 or short < 0 or to_ship + short != ordered or order_line in seen:
            raise RequestError("Invalid or duplicate shipment quantities.")
        seen.add(order_line)
        values.append(row)
    return sorted(values)


class Workflow:
    def __init__(self, database, upstream):
        self.database = database
        self.upstream = upstream
        self.lock = threading.RLock()
        self.lock_path = database + ".lock"
        with self.connect() as db:
            db.execute("""CREATE TABLE IF NOT EXISTS loading (
                warehouse TEXT NOT NULL, load_id TEXT NOT NULL, visit_id TEXT NOT NULL,
                trailer_id TEXT NOT NULL, confirmation TEXT NOT NULL,
                phase TEXT NOT NULL, confirmed_at TEXT NOT NULL, updated_at TEXT NOT NULL,
                PRIMARY KEY (warehouse, load_id))""")
            db.execute("""CREATE TABLE IF NOT EXISTS dock_availability (
                warehouse TEXT NOT NULL, dock_id TEXT NOT NULL, unavailable INTEGER NOT NULL,
                reason TEXT NOT NULL, revision INTEGER NOT NULL, updated_at TEXT NOT NULL,
                PRIMARY KEY (warehouse,dock_id))""")

    def dock_records(self, warehouse):
        with self.connect() as db:
            return [dict(row) for row in db.execute(
                "SELECT dock_id, unavailable, reason, revision FROM dock_availability WHERE warehouse=?",
                (warehouse,))]

    def require_available(self, warehouse, dock_id):
        record = next((r for r in self.dock_records(warehouse) if str(r["dock_id"]) == str(dock_id)), None)
        if record and record["unavailable"]:
            raise RequestError("This Dock is temporarily out of service. Choose another Dock.")

    def availability(self, warehouse, dock_id, headers, body):
        # Reuse the Yard's authenticated allocation permission.
        self.read(BASE + "/yard-visits", warehouse, headers)
        if dock_id is None:
            return response(self.dock_records(warehouse))
        submitted = json.loads(body)
        unavailable = submitted.get("unavailable")
        reason = submitted.get("reason", "")
        revision = submitted.get("revision")
        if type(unavailable) is not bool or not isinstance(reason, str) or len(reason) > 200 or type(revision) is not int or revision < 0:
            raise RequestError("Choose a valid Dock availability setting.", 400)
        groups = self.read("/api/layout/locationgroups", warehouse, headers)
        group_ids = [str(g["id"]) for g in groups if g.get("name") == "SHIP_DCK" and g.get("locationGroupType", {}).get("dock")]
        if not group_ids:
            raise RequestError("No outbound Dock group is available.")
        docks = self.read("/api/layout/locations", warehouse, headers,
                          locationGroupIds=",".join(group_ids), includeEmptyLocation="true", includeDisabledLocation="true")
        if not any(str(d["id"]) == dock_id for d in docks):
            raise RequestError("This Dock does not belong to this warehouse's outbound Docks.", 404)
        with self.connect() as db:
            db.execute("BEGIN IMMEDIATE")
            current = db.execute("SELECT revision FROM dock_availability WHERE warehouse=? AND dock_id=?",
                                 (warehouse, dock_id)).fetchone()
            if revision != (current["revision"] if current else 0):
                raise RequestError("Dock availability changed. Refresh and try again.")
            db.execute("""INSERT INTO dock_availability VALUES (?,?,?,?,?,?)
                ON CONFLICT(warehouse,dock_id) DO UPDATE SET unavailable=excluded.unavailable,
                reason=excluded.reason, revision=excluded.revision, updated_at=excluded.updated_at""",
                (warehouse, dock_id, int(unavailable), reason.strip() if unavailable else "",
                 revision + 1, datetime.now(timezone.utc).isoformat()))
        return response({"dockId": int(dock_id), "unavailable": unavailable, "revision": revision + 1})

    def connect(self):
        db = sqlite3.connect(self.database, timeout=30)
        db.row_factory = sqlite3.Row
        return db

    def records(self, warehouse):
        with self.connect() as db:
            return {row["load_id"]: dict(row) for row in db.execute(
                "SELECT * FROM loading WHERE warehouse=?", (warehouse,))}

    def save(self, warehouse, load_id, visit, confirmation, phase):
        now = datetime.now(timezone.utc).isoformat()
        with self.connect() as db:
            db.execute("""INSERT INTO loading VALUES (?,?,?,?,?,?,?,?)
                ON CONFLICT(warehouse,load_id) DO UPDATE SET
                visit_id=excluded.visit_id,trailer_id=excluded.trailer_id,
                confirmation=excluded.confirmation,phase=excluded.phase,
                confirmed_at=CASE WHEN excluded.phase='LOADED' THEN excluded.confirmed_at ELSE loading.confirmed_at END,
                updated_at=excluded.updated_at""",
                (warehouse, str(load_id), str(visit["visit_id"]), str(visit["trailer_id"]),
                 json.dumps(confirmation), phase, now, now))

    def read(self, path, warehouse, headers, **params):
        query = urlencode({"warehouseId": warehouse, **params})
        return payload(self.upstream("GET", path + "?" + query, headers, b""))

    def visit(self, warehouse, visit_id, headers, history=False):
        visits = self.read(BASE + "/yard-visits", warehouse, headers, history=str(history).lower())
        return next((v for v in visits if str(v["visit_id"]) == str(visit_id)), None)

    def preview(self, warehouse, load_id, headers):
        return self.read(BASE + "/" + str(load_id) + "/shipping", warehouse, headers)

    def confirm(self, warehouse, load_id, headers, body):
        submitted = json.loads(body)
        if submitted.get("physicallyLoaded") is not True:
            raise RequestError("Confirm the goods are physically loaded.", 400)
        requested = canonical_lines(submitted.get("lines"))
        preview = self.preview(warehouse, load_id, headers)
        if preview.get("status") != "PLANNED":
            raise RequestError("This Load has already shipped or cannot be confirmed.")
        if preview.get("pendingOrders") or requested != canonical_lines(preview.get("lines")):
            raise RequestError("Load contents changed or picking is incomplete. Refresh and review again.")
        if any(row[-1] for row in requested) and submitted.get("acceptShort") is not True:
            raise RequestError("Confirm the remaining quantity will close as short shipped.")
        visits = self.read(BASE + "/yard-visits", warehouse, headers)
        matches = [v for v in visits if str(v.get("load_id")) == load_id]
        if len(matches) != 1 or matches[0].get("state") != "AT_DOCK":
            raise RequestError("Assign this Load to a checked-in Trailer at a Dock before confirming.")
        visit = matches[0]
        previous = self.records(warehouse).get(load_id)
        if previous and previous["phase"] != "LOADED":
            raise RequestError("Dispatch is already in progress. Retry Dispatch Trailer to finish.")
        confirmation = {"physicallyLoaded": True, "acceptShort": submitted.get("acceptShort") is True,
                        "lines": preview["lines"]}
        self.save(warehouse, load_id, visit, confirmation, "LOADED")
        return response({"loadId": int(load_id), "status": "LOADED", "shipped": False})

    def dispatch(self, warehouse, visit_id, path, headers, body):
        submitted = json.loads(body)
        if submitted.get("physicallyDeparted") is not True:
            raise RequestError("Confirm the Trailer is leaving the yard.", 400)
        visit = self.visit(warehouse, visit_id, headers)
        if visit is None:
            # A lost success response can be retried without another native write.
            departed = self.visit(warehouse, visit_id, headers, history=True)
            if departed and departed.get("state") == "DISPATCHED":
                record = self.records(warehouse).get(str(departed.get("load_id")))
                if record and record["visit_id"] == visit_id:
                    self.save(warehouse, record["load_id"], departed,
                              json.loads(record["confirmation"]), "DISPATCHED")
                    return response(departed)
            raise RequestError("This Yard visit is no longer active. Refresh the board.")
        if visit.get("revision") != submitted.get("revision"):
            raise RequestError("This visit changed. Refresh before dispatching.")
        load_id = str(visit.get("load_id") or "")
        record = self.records(warehouse).get(load_id)
        if not load_id:
            return self.upstream("POST", path, headers, body)
        # Already-shipped loads, including B3, keep their existing departure workflow.
        if record is None:
            if visit.get("load_status") != "COMPLETED":
                raise RequestError("Confirm Loaded before dispatching this Trailer.")
            return self.upstream("POST", path, headers, body)
        if record["visit_id"] != visit_id or record["trailer_id"] != str(visit["trailer_id"]):
            raise RequestError("Trailer / Load association changed. Review the Load before dispatching.")
        preview = self.preview(warehouse, load_id, headers)
        confirmation = json.loads(record["confirmation"])
        if preview.get("status") != "COMPLETED":
            if preview.get("pendingOrders") or canonical_lines(preview.get("lines")) != canonical_lines(confirmation["lines"]):
                raise RequestError("Load contents changed after confirmation. Review and confirm loading again.")
            self.save(warehouse, load_id, visit, confirmation, "DISPATCHING")
            shipping_path = BASE + "/" + load_id + "/shipping/confirm-loaded?" + urlencode({"warehouseId": warehouse})
            payload(self.upstream("POST", shipping_path, headers, json.dumps(confirmation).encode()))
            preview = self.preview(warehouse, load_id, headers)
            if preview.get("status") != "COMPLETED":
                raise RequestError("Shipping completion is not confirmed. Refresh and retry Dispatch Trailer.")
        self.save(warehouse, load_id, visit, confirmation, "SHIPPED")
        result = self.upstream("POST", path, headers, body)
        payload(result)
        self.save(warehouse, load_id, visit, confirmation, "DISPATCHED")
        return result

    def overlay(self, path, warehouse, original):
        data = copy.deepcopy(payload(original))
        records = self.records(warehouse)
        def status(load_id, native):
            record = records.get(str(load_id))
            if record and record["phase"] != "DISPATCHED" and native != "COMPLETED":
                return "LOADED" if record["phase"] == "LOADED" else "DISPATCHING"
            return native
        if path == BASE + "/yard-visits" and isinstance(data, list):
            for visit in data:
                visit["load_status"] = status(visit.get("load_id"), visit.get("load_status"))
        elif path == "/api/common/trailer-appointments" and isinstance(data, list):
            for load in data:
                load["status"] = status(load.get("id"), load.get("status"))
        elif re.fullmatch(BASE + r"/\d+/shipping", path) and isinstance(data, dict):
            data["status"] = status(data.get("loadId"), data.get("status"))

        elif path in ("/api/outbound/stops", "/api/outbound/shipments") or re.fullmatch(r"/api/outbound/shipments/\d+", path):
            loaded_shipments = {}
            for record in records.values():
                if record["phase"] in ("LOADED", "DISPATCHING"):
                    for line in json.loads(record["confirmation"])["lines"]:
                        loaded_shipments[str(line["shipmentId"])] = record["phase"]
            if path == "/api/outbound/stops" and isinstance(data, list):
                shipments = [shipment for stop in data for shipment in stop.get("shipments", [])]
            else:
                shipments = data if isinstance(data, list) else data.get("content", [data])
            for shipment in shipments:
                if str(shipment.get("id")) in loaded_shipments and shipment.get("status") != "DISPATCHED":
                    shipment["status"] = "LOADED"
        return replace_payload(original, data)

    def handle(self, method, url, headers, body):
        parsed = urlsplit(url)
        path = parsed.path.rstrip("/")
        params = parse_qs(parsed.query)
        warehouse = params.get("warehouseId", [""])[0]
        if not warehouse.isdecimal():
            # Retain normal upstream validation for requests outside warehouse scope.
            if method == "POST" and (path.endswith("/confirm-loaded") or path.endswith("/dispatch")):
                raise RequestError("A valid warehouseId is required.", 400)
            return self.upstream(method, url, headers, body)
        # Serialize across overlapping pods sharing the same host-local state.
        with self.lock, open(self.lock_path, "a") as lock_file:
            fcntl.flock(lock_file, fcntl.LOCK_EX)
            availability = re.fullmatch(BASE + r"/dock-availability(?:/(\d+))?", path)
            if availability and method in ("GET", "POST"):
                if method == "GET" and availability[1] is None:
                    return self.availability(warehouse, None, headers, body)
                if method == "POST" and availability[1]:
                    return self.availability(warehouse, availability[1], headers, body)
                raise RequestError("Invalid Dock availability request.", 400)
            assignment = re.fullmatch(BASE + r"/(\d+)/dock-assignment", path)
            if method == "POST" and assignment:
                self.preview(warehouse, assignment[1], headers)
                self.require_available(warehouse, json.loads(body).get("dockId"))
            match = re.fullmatch(BASE + r"/(\d+)/shipping/confirm-loaded", path)
            if method == "POST" and match:
                return self.confirm(warehouse, match[1], headers, body)
            match = re.fullmatch(BASE + r"/yard-visits/(\d+)/([^/]+)", path)
            if method == "POST" and match:
                if match[2] == "dispatch":
                    return self.dispatch(warehouse, match[1], url, headers, body)
                visit = self.visit(warehouse, match[1], headers)
                submitted = json.loads(body)
                if match[2] in ("assign-dock", "assign-and-arrive"):
                    self.require_available(warehouse, submitted.get("dockId"))
                records = self.records(warehouse)
                if visit and (str(visit.get("load_id")) in records or str(submitted.get("loadId")) in records):
                    raise RequestError("This Load has been confirmed. Dispatch it before changing its assignment.")
            match = re.fullmatch(BASE + r"/(\d+)(?:/.*)?", path)
            if method in ("POST", "PUT", "DELETE", "PATCH") and match and match[1] in self.records(warehouse):
                # Access is checked upstream before revealing the recorded state.
                self.preview(warehouse, match[1], headers)
                raise RequestError("This Load has been confirmed. Shipment assignments are locked.")
            original = self.upstream(method, url, headers, body)
            if not 200 <= original[0] < 300:
                return original
            if method == "GET" and (path in (BASE + "/yard-visits", "/api/common/trailer-appointments", "/api/outbound/stops", "/api/outbound/shipments")
                                   or re.fullmatch(r"/api/outbound/shipments/\d+", path)
                                   or re.fullmatch(BASE + r"/\d+/shipping", path)):
                return self.overlay(path, warehouse, original)
            return original


def upstream(method, url, headers, body):
    clean = {k: v for k, v in headers.items()
             if k.lower() not in ("host", "content-length", "connection", "accept-encoding", "transfer-encoding")}
    clean["Accept-Encoding"] = "identity"
    connection = http.client.HTTPConnection("apigateway", 5555, timeout=120)
    try:
        connection.request(method, url, body=body or None, headers=clean)
        result = connection.getresponse()
        return result.status, dict(result.getheaders()), result.read()
    finally:
        connection.close()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass  # Authorization and request bodies never enter logs.

    def process(self):
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 0 or size > 1_000_000 or self.headers.get("Transfer-Encoding"):
                raise RequestError("Invalid request size.", 400)
            result = self.server.workflow.handle(self.command, self.path, dict(self.headers), self.rfile.read(size))
        except RequestError as error:
            result = error.status, {"Content-Type": "application/json"}, json.dumps({"message": error.message}).encode()
        except (ValueError, TypeError, KeyError):
            result = 400, {"Content-Type": "application/json"}, b'{"message":"Invalid request. Refresh and try again."}'
        except Exception:
            result = 502, {"Content-Type": "application/json"}, b'{"message":"Request outcome could not be verified. Refresh and retry the same operation."}'
        status, headers, body = result
        self.send_response(status)
        for key, value in headers.items():
            if key.lower() not in ("content-length", "connection", "transfer-encoding", "content-encoding", "server", "date"):
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = process


if __name__ == "__main__":
    os.umask(0o077)
    database = os.environ.get("YARD_LOADING_DB", os.path.join(os.path.dirname(__file__), "loading.sqlite3"))
    server = ThreadingHTTPServer(("127.0.0.1", 18816), Handler)
    server.workflow = Workflow(database, upstream)
    server.serve_forever()

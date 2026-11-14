package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

final class LoadOrderPlanStore {
    private final JdbcTemplate db;
    private final LoadShipmentLinkService planningLinks;

    LoadOrderPlanStore(JdbcTemplate db, LoadShipmentLinkService planningLinks) {
        this.db = db;
        this.planningLinks = planningLinks;
    }

    List<Map<String, Object>> list(long warehouse, long load) {
        return db.queryForList("""
            SELECT p.order_id AS orderId, p.shipment_id AS shipmentId, o.number AS orderNumber, o.status AS orderStatus
            FROM load_order_plan p LEFT JOIN outbound_order o
              ON o.warehouse_id=p.warehouse_id AND o.outbound_order_id=p.order_id
            WHERE p.warehouse_id=? AND p.load_id=? ORDER BY o.number, p.order_id
            """, warehouse, load);
    }

    Long findOrder(long warehouse, String number) {
        var matches = db.queryForList("""
            SELECT outbound_order_id FROM outbound_order WHERE warehouse_id=? AND number=?
            """, Long.class, warehouse, number);
        require(matches.size() == 1, "Enter an exact, unique Sales Order number in this warehouse.");
        return matches.get(0);
    }

    void add(long warehouse, long load, long order, String actor) {
        var states = db.queryForList("""
            SELECT status FROM outbound_order WHERE warehouse_id=? AND outbound_order_id=? FOR UPDATE
            """, String.class, warehouse, order);
        require(states.size() == 1 && List.of("OPEN", "INPROCESS").contains(states.get(0)),
                "Only open or in-progress Sales Orders can be planned.");
        var owner = db.queryForList("""
            SELECT load_id FROM load_order_plan WHERE warehouse_id=? AND order_id=? FOR UPDATE
            """, Long.class, warehouse, order);
        require(owner.isEmpty() || owner.get(0) == load, "This Sales Order is already planned on another Load.");
        if (owner.isEmpty()) {
            db.update("INSERT INTO load_order_plan(warehouse_id,order_id,load_id,created_by) VALUES(?,?,?,?)",
                    warehouse, order, load, actor);
        }
        reconcile(warehouse, load, order, false);
    }

    void remove(long warehouse, long load, long order, String actor) {
        var plans = db.queryForList("""
            SELECT shipment_id FROM load_order_plan WHERE warehouse_id=? AND load_id=? AND order_id=? FOR UPDATE
            """, warehouse, load, order);
        require(plans.size() == 1, "Sales Order is no longer planned on this Load.");
        Object shipmentId = plans.get(0).get("shipment_id");
        if (shipmentId != null) {
            var current = linkedShipments(warehouse, load);
            long shipment = ((Number) shipmentId).longValue();
            if (current.contains(shipment)) {
                var wanted = current.stream().filter(id -> id != shipment).toList();
                planningLinks.replace(warehouse, load, current, wanted, actor);
            }
        }
        db.update("DELETE FROM load_order_plan WHERE warehouse_id=? AND load_id=? AND order_id=?",
                warehouse, load, order);
    }

    boolean isPlannedShipment(long warehouse, long load, long shipment) {
        return db.queryForObject("""
            SELECT COUNT(*) FROM load_order_plan WHERE warehouse_id=? AND load_id=? AND shipment_id=?
            """, Long.class, warehouse, load, shipment) > 0;
    }

    void requireShipmentAssignment(long warehouse, long load, long shipment, boolean wanted) {
        var plans = db.queryForList("""
            SELECT DISTINCT p.load_id FROM shipment_line sl
            JOIN outbound_order_line ol ON ol.warehouse_id=sl.warehouse_id AND ol.outbound_order_line_id=sl.outbound_order_line_id
            JOIN load_order_plan p ON p.warehouse_id=ol.warehouse_id AND p.order_id=ol.outbound_order_id
            WHERE sl.warehouse_id=? AND sl.shipment_id=?
            """, Long.class, warehouse, shipment);
        require(plans.isEmpty() || wanted && plans.stream().allMatch(owner -> owner == load),
                "Manage this Shipment from its Sales Order plan.");
    }

    List<Map<String, Object>> due() {
        return db.queryForList("""
            SELECT p.warehouse_id, p.load_id, p.order_id
            FROM load_order_plan p JOIN trailer_appointment a
              ON a.warehouse_id=p.warehouse_id AND a.trailer_appointment_id=p.load_id
            WHERE p.shipment_id IS NULL AND p.next_check_at <= CURRENT_TIMESTAMP(3)
              AND a.type='SHIPPING' AND a.status='PLANNED'
            ORDER BY p.next_check_at, p.warehouse_id, p.order_id LIMIT 50
            """);
    }

    void defer(long warehouse, long order) {
        db.update("""
            UPDATE load_order_plan SET next_check_at=CURRENT_TIMESTAMP(3) + INTERVAL 15 SECOND
            WHERE warehouse_id=? AND order_id=? AND shipment_id IS NULL
            """, warehouse, order);
    }

    void reconcile(long warehouse, long load, long order, boolean deferWhenMissing) {
        var plans = db.queryForList("""
            SELECT shipment_id, created_by FROM load_order_plan
            WHERE warehouse_id=? AND load_id=? AND order_id=? FOR UPDATE
            """, warehouse, load, order);
        if (plans.isEmpty() || plans.get(0).get("shipment_id") != null) return;
        var shipments = db.queryForList("""
            SELECT DISTINCT s.shipment_id
            FROM outbound_order_line ol JOIN shipment_line sl
              ON sl.warehouse_id=ol.warehouse_id AND sl.outbound_order_line_id=ol.outbound_order_line_id
            JOIN shipment s ON s.warehouse_id=sl.warehouse_id AND s.shipment_id=sl.shipment_id
            WHERE ol.warehouse_id=? AND ol.outbound_order_id=? AND s.status<>'CANCELLED'
            """, Long.class, warehouse, order);
        if (shipments.isEmpty()) {
            if (deferWhenMissing) defer(warehouse, order);
            return;
        }
        require(shipments.size() == 1, "Split orders require the existing Shipment workflow.");
        long shipment = shipments.get(0);
        var orders = db.queryForList("""
            SELECT DISTINCT ol.outbound_order_id FROM shipment_line sl JOIN outbound_order_line ol
              ON ol.warehouse_id=sl.warehouse_id AND ol.outbound_order_line_id=sl.outbound_order_line_id
            WHERE sl.warehouse_id=? AND sl.shipment_id=?
            """, Long.class, warehouse, shipment);
        require(orders.size() == 1 && orders.get(0) == order, "Shipment contains multiple Sales Orders.");
        var current = linkedShipments(warehouse, load);
        if (!current.contains(shipment)) {
            var wanted = new java.util.ArrayList<>(current);
            wanted.add(shipment);
            planningLinks.replace(warehouse, load, current, wanted, (String) plans.get(0).get("created_by"));
        }
        db.update("""
            UPDATE load_order_plan SET shipment_id=? WHERE warehouse_id=? AND order_id=? AND load_id=?
            """, shipment, warehouse, order, load);
    }

    private List<Long> linkedShipments(long warehouse, long load) {
        return db.queryForList("""
            SELECT s.shipment_id FROM shipment s JOIN stop t
              ON t.warehouse_id=s.warehouse_id AND t.stop_id=s.stop_id
            WHERE t.warehouse_id=? AND t.trailer_appointment_id=? ORDER BY s.shipment_id
            """, Long.class, warehouse, load);
    }

    static void validatePlanningShipment(JdbcTemplate db, long warehouse, long shipment) {
        var status = db.queryForObject("SELECT status FROM shipment WHERE warehouse_id=? AND shipment_id=?",
                String.class, warehouse, shipment);
        require(List.of("PENDING", "INPROCESS", "STAGED").contains(status),
                "This Shipment has already started loading, shipped, or been cancelled.");
        long invalid = db.queryForObject("""
            SELECT COUNT(*) FROM shipment_line WHERE warehouse_id=? AND shipment_id=?
              AND (COALESCE(loaded_quantity,0)>0 OR COALESCE(shipped_quantity,0)>0)
            """, Long.class, warehouse, shipment);
        require(invalid == 0, "This Shipment already has loaded or shipped quantities.");
        long started = db.queryForObject("""
            SELECT COUNT(*) FROM shipment_inventory_batch WHERE warehouse_id=? AND shipment_id=?
            """, Long.class, warehouse, shipment);
        require(started == 0, "Shipping is already in progress for this Shipment.");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}

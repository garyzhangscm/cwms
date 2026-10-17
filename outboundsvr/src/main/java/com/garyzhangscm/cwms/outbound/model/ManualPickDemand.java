package com.garyzhangscm.cwms.outbound.model;

import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import java.util.*;

/** Read remaining demand without changing order, shipment or inventory quantities. */
public final class ManualPickDemand {
    private ManualPickDemand() {}

    public static Map<Long,Long> byOrderLine(Order order, List<Shipment> shipments) {
        Map<Long,Long> result = new LinkedHashMap<>();
        for (OrderLine line : order.getOrderLines()) result.put(line.getId(), 0L);
        if (!Boolean.TRUE.equals(order.getAllowForManualPick()) ||
                order.getStatus() == OrderStatus.COMPLETE || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.ALLOCATING ||
                Boolean.TRUE.equals(order.getCancelRequested())) return result;
        List<Shipment> active = new ArrayList<>();
        for (Shipment shipment : shipments) {
            if (shipment.getStatus() != ShipmentStatus.CANCELLED && shipment.getStatus() != ShipmentStatus.DISPATCHED) active.add(shipment);
        }
        // The existing manual-pick writer supports one active shipment. Do not advertise ambiguous demand.
        if (active.size() > 1) throw OrderOperationException.raiseException("Multiple active shipments exist; review the order before manual picking");
        if (active.isEmpty()) {
            for (OrderLine line : order.getOrderLines()) result.put(line.getId(), positive(line.getOpenQuantity()));
        } else {
            Shipment shipment = active.get(0);
            if (shipment.getStatus() != ShipmentStatus.PENDING && shipment.getStatus() != ShipmentStatus.INPROCESS) return result;
            for (ShipmentLine line : shipment.getShipmentLines()) {
                Long id = line.getOrderLine() == null ? null : line.getOrderLine().getId();
                if (id == null || !result.containsKey(id)) throw OrderOperationException.raiseException("Shipment line does not match the order; refresh before picking");
                result.put(id, Math.addExact(result.get(id), positive(line.getOpenQuantity())));
            }
        }
        return result;
    }

    public static long remaining(Order order, OrderLine line, List<Shipment> shipments) {
        return byOrderLine(order, shipments).getOrDefault(line.getId(), 0L);
    }

    private static long positive(Long quantity) { return quantity == null ? 0L : Math.max(0L, quantity); }
}

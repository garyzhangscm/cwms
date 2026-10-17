package com.garyzhangscm.cwms.outbound.controller;

import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
public class ManualPickAvailabilityController {
    @Autowired private OrderService orders;
    @Autowired private ShipmentService shipments;

    @GetMapping("/orders/{id}/manual-pick-availability")
    @ClientValidationEndpoint
    @Transactional(readOnly = true)
    public Map<String,Object> availability(@PathVariable Long id, @RequestParam Long warehouseId, ClientRestriction restriction) {
        Order order = orders.findById(id);
        if (!Objects.equals(order.getWarehouseId(), warehouseId) ||
                !accessible(order.getClientId(), restriction))
            throw OrderOperationException.raiseException("Order not found in the selected warehouse");
        Map<Long,Long> quantities = ManualPickDemand.byOrderLine(order, shipments.findByOrder(order, false));
        List<Map<String,Object>> lines = new ArrayList<>(); long total = 0;
        for (OrderLine line : order.getOrderLines()) {
            long quantity = quantities.getOrDefault(line.getId(), 0L); total = Math.addExact(total, quantity);
            Map<String,Object> row = new LinkedHashMap<>(); row.put("orderLineId", line.getId()); row.put("number", line.getNumber()); row.put("remainingQuantity", quantity); lines.add(row);
        }
        Map<String,Object> result = new LinkedHashMap<>(); result.put("orderId", order.getId()); result.put("canPick", total > 0); result.put("remainingQuantity", total); result.put("lines", lines);
        return result;
    }

    private boolean accessible(Long clientId, ClientRestriction restriction) {
        if (restriction == null || !Boolean.TRUE.equals(restriction.getThreePartyLogisticsFlag()) || Boolean.TRUE.equals(restriction.getAllClientAccess())) return true;
        if (clientId == null) return Boolean.TRUE.equals(restriction.getNonClientDataAccessible());
        String ids = restriction.getClientAccesses();
        return ids != null && Arrays.stream(ids.split(",")).anyMatch(id -> id.trim().equals(clientId.toString()));
    }
}

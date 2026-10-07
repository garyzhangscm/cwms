package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Load allocation references before any Outbound request can create picks. */
@Service
public class WorkOrderAllocationDataService {
    private final InventoryServiceRestemplateClient inventory;
    private final WarehouseLayoutServiceRestemplateClient layout;
    private final ObjectMapper mapper;

    public WorkOrderAllocationDataService(InventoryServiceRestemplateClient inventory,
                                          WarehouseLayoutServiceRestemplateClient layout, ObjectMapper mapper) {
        this.inventory = inventory;
        this.layout = layout;
        this.mapper = mapper;
    }

    public void prepare(WorkOrder order) {
        String context = "Cannot allocate work order " + order.getNumber() + ": ";
        if (order.getWarehouseId() == null || order.getWarehouseId() <= 0)
            fail(context + "warehouse is missing.");
        Warehouse warehouse = layout.getWarehouseById(order.getWarehouseId());
        if (warehouse == null || !Objects.equals(warehouse.getId(), order.getWarehouseId()))
            fail(context + "warehouse could not be loaded.");
        order.setWarehouse(warehouse);
        Map<Long, Item> items = new HashMap<>();
        Map<Long, InventoryStatus> statuses = new HashMap<>();
        order.setItem(item(order.getItemId(), order.getWarehouseId(), items, context + "finished item "));
        for (WorkOrderLine line : order.getWorkOrderLines()) {
            String lineContext = context + "material line " + line.getNumber() + " (" + line.getId() + ") ";
            if (line.getOpenQuantity() == null) fail(lineContext + "has no open quantity.");
            if (line.getOpenQuantity() <= 0) continue;
            line.setItem(item(line.getItemId(), order.getWarehouseId(), items, lineContext + "item "));
            if (line.getInventoryStatusId() != null) {
                Long id = line.getInventoryStatusId();
                InventoryStatus status = statuses.computeIfAbsent(id, inventory::getInventoryStatusById);
                if (status == null || !Objects.equals(status.getId(), id))
                    fail(lineContext + "inventory status " + id + " could not be loaded.");
                line.setInventoryStatus(status);
            }
        }
    }

    private Item item(Long id, Long warehouseId, Map<Long, Item> items, String context) {
        if (id == null || id <= 0) fail(context + "ID is missing.");
        Item item = items.computeIfAbsent(id, inventory::getItemById);
        if (item == null || !Objects.equals(item.getId(), id) || item.getName() == null || item.getName().isBlank())
            fail(context + id + " could not be loaded.");
        if (!Objects.equals(item.getWarehouseId(), warehouseId))
            fail(context + id + " does not belong to the work order warehouse.");
        return item;
    }

    /** WorkOrder hides warehouse in normal responses; allocation needs an explicit transport contract. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> requestBody(WorkOrder order) {
        if (order.getWarehouse() == null || !Objects.equals(order.getWarehouse().getId(), order.getWarehouseId()))
            fail("Cannot allocate work order " + order.getNumber() + ": warehouse could not be loaded.");
        Map<String, Object> body = mapper.convertValue(order, Map.class);
        body.put("warehouse", mapper.convertValue(order.getWarehouse(), Map.class));
        for (Map<String, Object> line : (List<Map<String, Object>>) body.get("workOrderLines"))
            line.put("warehouseId", order.getWarehouseId());
        return body;
    }

    private static void fail(String message) { throw WorkOrderException.raiseException(message); }
}

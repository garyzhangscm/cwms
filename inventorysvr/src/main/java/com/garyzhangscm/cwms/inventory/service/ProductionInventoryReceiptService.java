package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryQuantityChangeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionInventoryReceiptService {
    private final JdbcTemplate jdbc;
    private final InventoryService inventoryService;

    public ProductionInventoryReceiptService(JdbcTemplate jdbc, InventoryService inventoryService) {
        this.jdbc = jdbc;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Inventory receive(Long productionDetailId, Inventory inventory, String documentNumber, String comment, Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {
        Long matchingDetails = this.jdbc.queryForObject("SELECT COUNT(*)\nFROM work_order_produced_inventory d\nJOIN work_order_produce_transaction t\n    ON t.work_order_produce_transaction_id = d.work_order_produce_transaction_id\nJOIN work_order w ON w.work_order_id = t.work_order_id\nWHERE d.work_order_produced_inventory_id = ?\n    AND t.work_order_produce_transaction_id = ?\n    AND w.warehouse_id = ? AND w.work_order_id = ?\n    AND w.number = ? AND d.lpn = ? AND d.quantity = ? AND w.item_id = ?\n", Long.class, new Object[]{productionDetailId, inventory.getCreateInventoryTransactionId(), inventory.getWarehouseId(), inventory.getWorkOrderId(), documentNumber, inventory.getLpn(), inventory.getQuantity(), inventory.getItemId()});
        if (matchingDetails == null || matchingDetails != 1L) {
            throw RequestValidationFailException.raiseException("Production detail does not match the inventory receipt");
        }
        this.jdbc.update("INSERT INTO production_inventory_receipt (warehouse_id, detail_id)\nVALUES (?, ?)\nON DUPLICATE KEY UPDATE detail_id = detail_id\n", new Object[]{inventory.getWarehouseId(), productionDetailId});
        Long existingInventoryId = this.jdbc.queryForObject("SELECT inventory_id FROM production_inventory_receipt\nWHERE warehouse_id = ? AND detail_id = ? FOR UPDATE\n", Long.class, new Object[]{inventory.getWarehouseId(), productionDetailId});
        if (existingInventoryId != null) {
            return this.inventoryService.findById(existingInventoryId);
        }
        Inventory receivedInventory = this.inventoryService.addInventory(inventory, InventoryQuantityChangeType.PRODUCING, documentNumber, comment, reasonCodeId, kitInventoryUseDefaultAttribute);
        if (receivedInventory.getId() == null) {
            throw new IllegalStateException("Production receipt did not create inventory");
        }
        this.jdbc.update("UPDATE production_inventory_receipt SET inventory_id = ?\nWHERE warehouse_id = ? AND detail_id = ?\n", new Object[]{receivedInventory.getId(), inventory.getWarehouseId(), productionDetailId});
        return receivedInventory;
    }
}

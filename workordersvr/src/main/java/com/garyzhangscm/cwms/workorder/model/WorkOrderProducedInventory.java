package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_produced_inventory")
public class WorkOrderProducedInventory extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_produced_inventory_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_produce_transaction_id")
    @JsonIgnore
    private WorkOrderProduceTransaction workOrderProduceTransaction;



    @Column(name = "lpn")
    private String lpn;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @Column(name = "item_package_type_id")
    private Long itemPackageTypeId;

    @Transient
    private ItemPackageType itemPackageType;


    public Inventory createInventory(WorkOrder workOrder,
                                     WorkOrderProduceTransaction workOrderProduceTransaction) {

        Inventory inventory = new Inventory();
        inventory.setLpn(getLpn());
        inventory.setLocationId(workOrderProduceTransaction.getProductionLine().getOutboundStageLocation().getId());
        inventory.setLocation(workOrderProduceTransaction.getProductionLine().getOutboundStageLocation());
        inventory.setItem(workOrder.getItem());
        inventory.setItemPackageType(getItemPackageType());

        inventory.setQuantity(getQuantity());
        inventory.setVirtual(false);
        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setWorkOrderId(workOrder.getId());
        inventory.setWarehouseId(workOrder.getWarehouseId());

        inventory.setCreateInventoryTransactionId(workOrderProduceTransaction.getId());
        return inventory;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderProduceTransaction getWorkOrderProduceTransaction() {
        return workOrderProduceTransaction;
    }

    public void setWorkOrderProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {
        this.workOrderProduceTransaction = workOrderProduceTransaction;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public Long getItemPackageTypeId() {
        return itemPackageTypeId;
    }

    public void setItemPackageTypeId(Long itemPackageTypeId) {
        this.itemPackageTypeId = itemPackageTypeId;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }
}

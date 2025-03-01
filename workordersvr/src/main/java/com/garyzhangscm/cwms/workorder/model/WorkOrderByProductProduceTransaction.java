package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "work_order_by_product_produce_transaction")
public class WorkOrderByProductProduceTransaction extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_by_product_produce_transaction_id")
    @JsonProperty(value="id")
    private Long id;


    /**
     * A work order by product produce transaction can be
     * 1. standalone
     * 2. Alone with the work order produce transaction
     * 3. Alone with the work order complete transaction
     */
    @ManyToOne
    @JoinColumn(name = "work_order_complete_transaction_id")
    @JsonIgnore
    private WorkOrderCompleteTransaction workOrderCompleteTransaction;

    @ManyToOne
    @JoinColumn(name = "work_order_produce_transaction_id")
    @JsonIgnore
    private WorkOrderProduceTransaction workOrderProduceTransaction;


    @ManyToOne
    @JoinColumn(name = "work_order_by_product_id")
    private WorkOrderByProduct workOrderByProduct;

    @Column(name = "lpn")
    private String lpn;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @Column(name = "item_package_type_id")
    private Long itemPackageTypeId;

    @Transient
    private ItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Inventory createInventory(Location location) {

        WorkOrder workOrder = workOrderByProduct.getWorkOrder();
        Inventory inventory = new Inventory();
        inventory.setLpn(getLpn());
        if (Objects.nonNull(locationId)) {
            inventory.setLocationId(locationId);

        }
        else {
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }
        if (Objects.nonNull(location)) {
            inventory.setLocation(location);
        }

        inventory.setItem(workOrderByProduct.getItem());
        inventory.setItemPackageType(getItemPackageType());
        inventory.setQuantity(getQuantity());
        inventory.setVirtual(false);
        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setWarehouseId(workOrder.getWarehouseId());
        inventory.setWorkOrderByProductId(workOrderByProduct.getId());

        return inventory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getItemPackageTypeId() {
        return itemPackageTypeId;
    }

    public void setItemPackageTypeId(Long itemPackageTypeId) {
        this.itemPackageTypeId = itemPackageTypeId;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public WorkOrderCompleteTransaction getWorkOrderCompleteTransaction() {
        return workOrderCompleteTransaction;
    }

    public void setWorkOrderCompleteTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction) {
        this.workOrderCompleteTransaction = workOrderCompleteTransaction;
    }

    public WorkOrderByProduct getWorkOrderByProduct() {
        return workOrderByProduct;
    }

    public void setWorkOrderByProduct(WorkOrderByProduct workOrderByProduct) {
        this.workOrderByProduct = workOrderByProduct;
    }

    public WorkOrderProduceTransaction getWorkOrderProduceTransaction() {
        return workOrderProduceTransaction;
    }

    public void setWorkOrderProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {
        this.workOrderProduceTransaction = workOrderProduceTransaction;
    }
}

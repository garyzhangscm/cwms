package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "return_material_request")
public class ReturnMaterialRequest extends AuditibleEntity<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_material_request_id")
    @JsonProperty(value="id")
    private Long id;

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

    @ManyToOne
    @JoinColumn(name = "work_order_line_complete_transaction_id")
    @JsonIgnore
    private WorkOrderLineCompleteTransaction workOrderLineCompleteTransaction;

    public Inventory createInventory(WorkOrder workOrder,
                                     WorkOrderLine workOrderLine) {

        Inventory inventory = new Inventory();
        inventory.setLpn(getLpn());
        if (Objects.nonNull(locationId)) {
            inventory.setLocationId(locationId);

        }
        else {
            inventory.setLocationId(workOrder.getProductionLine().getOutboundStageLocationId());
            inventory.setLocation(workOrder.getProductionLine().getOutboundStageLocation());
        }
        if (Objects.nonNull(location)) {
            inventory.setLocation(location);
        }

        inventory.setItem(workOrderLine.getItem());
        inventory.setItemPackageType(getItemPackageType());
        inventory.setQuantity(getQuantity());
        inventory.setVirtual(false);
        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setWarehouseId(workOrder.getWarehouseId());
        inventory.setWorkOrderLineId(workOrderLine.getId());

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

    public WorkOrderLineCompleteTransaction getWorkOrderLineCompleteTransaction() {
        return workOrderLineCompleteTransaction;
    }

    public void setWorkOrderLineCompleteTransaction(WorkOrderLineCompleteTransaction workOrderLineCompleteTransaction) {
        this.workOrderLineCompleteTransaction = workOrderLineCompleteTransaction;
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
}

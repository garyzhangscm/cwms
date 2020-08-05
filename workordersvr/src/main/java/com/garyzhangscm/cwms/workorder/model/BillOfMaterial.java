package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill_of_material")
public class BillOfMaterial extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_of_material_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<BillOfMaterialLine> billOfMaterialLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderInstructionTemplate> workOrderInstructionTemplates = new ArrayList<>();

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }


    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public List<BillOfMaterialLine> getBillOfMaterialLines() {
        return billOfMaterialLines;
    }

    public void setBillOfMaterialLines(List<BillOfMaterialLine> billOfMaterialLines) {
        this.billOfMaterialLines = billOfMaterialLines;
    }

    public List<WorkOrderInstructionTemplate> getWorkOrderInstructionTemplates() {
        return workOrderInstructionTemplates;
    }

    public void setWorkOrderInstructionTemplates(List<WorkOrderInstructionTemplate> workOrderInstructionTemplates) {
        this.workOrderInstructionTemplates = workOrderInstructionTemplates;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}

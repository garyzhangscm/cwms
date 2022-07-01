package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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


    @Column(name = "description")
    private String description;

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = {CascadeType.PERSIST,CascadeType.REMOVE},
            orphanRemoval = true
    )
    List<BillOfMaterialLine> billOfMaterialLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = {CascadeType.PERSIST,CascadeType.REMOVE},
            orphanRemoval = true
    )
    List<BillOfMaterialByProduct> billOfMaterialByProducts = new ArrayList<>();

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = {CascadeType.PERSIST,CascadeType.REMOVE},
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
    private Double expectedQuantity;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Double getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Double expectedQuantity) {
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

    public List<BillOfMaterialByProduct> getBillOfMaterialByProducts() {
        return billOfMaterialByProducts;
    }

    public void setBillOfMaterialByProducts(List<BillOfMaterialByProduct> billOfMaterialByProducts) {
        this.billOfMaterialByProducts = billOfMaterialByProducts;
    }
}

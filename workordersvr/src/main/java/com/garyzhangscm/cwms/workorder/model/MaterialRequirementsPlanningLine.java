package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "material_requirements_planning_line")
public class MaterialRequirementsPlanningLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_requirements_planning_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "parent_mrp_line_id")
    private Long parentMRPLineId;

    @ManyToOne
    @JoinColumn(name = "material_requirements_planning_id")
    @JsonIgnore
    private MaterialRequirementsPlanning materialRequirementsPlanning;


    @ManyToOne
    @JoinColumn(name = "bill_of_material_id")
    private BillOfMaterial billOfMaterial;

    @Transient
    private List<MaterialRequirementsPlanningLine> children;

    // overall required quantity, calculated from the MPS
    @Column(name = "total_required_quantity")
    private Long totalRequiredQuantity;

    // required quantity to be produced
    // requiredQuantity = totalRequiredQuantity - expectedInventoryOnHand
    //              - expectedReceiveQuantity + expectedOrderQuantity + expectedWorkOrderQuantity
    @Column(name = "required_quantity")
    private Long requiredQuantity;

    @Column(name = "expected_inventory_on_hand")
    private Long expectedInventoryOnHand;

    @Column(name = "expected_receive_quantity")
    private Long expectedReceiveQuantity;
    @Column(name = "expected_order_quantity")
    private Long expectedOrderQuantity;
    @Column(name = "expected_work_order_quantity")
    private Long expectedWorkOrderQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public Long getParentMRPLineId() {
        return parentMRPLineId;
    }

    public void setParentMRPLineId(Long parentMRPLineId) {
        this.parentMRPLineId = parentMRPLineId;
    }

    public MaterialRequirementsPlanning getMaterialRequirementsPlanning() {
        return materialRequirementsPlanning;
    }

    public void setMaterialRequirementsPlanning(MaterialRequirementsPlanning materialRequirementsPlanning) {
        this.materialRequirementsPlanning = materialRequirementsPlanning;
    }

    public BillOfMaterial getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(BillOfMaterial billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public List<MaterialRequirementsPlanningLine> getChildren() {
        return children;
    }

    public void setChildren(List<MaterialRequirementsPlanningLine> children) {
        this.children = children;
    }

    public Long getTotalRequiredQuantity() {
        return totalRequiredQuantity;
    }

    public void setTotalRequiredQuantity(Long totalRequiredQuantity) {
        this.totalRequiredQuantity = totalRequiredQuantity;
    }

    public Long getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Long requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public Long getExpectedInventoryOnHand() {
        return expectedInventoryOnHand;
    }

    public void setExpectedInventoryOnHand(Long expectedInventoryOnHand) {
        this.expectedInventoryOnHand = expectedInventoryOnHand;
    }

    public Long getExpectedReceiveQuantity() {
        return expectedReceiveQuantity;
    }

    public void setExpectedReceiveQuantity(Long expectedReceiveQuantity) {
        this.expectedReceiveQuantity = expectedReceiveQuantity;
    }

    public Long getExpectedOrderQuantity() {
        return expectedOrderQuantity;
    }

    public void setExpectedOrderQuantity(Long expectedOrderQuantity) {
        this.expectedOrderQuantity = expectedOrderQuantity;
    }

    public Long getExpectedWorkOrderQuantity() {
        return expectedWorkOrderQuantity;
    }

    public void setExpectedWorkOrderQuantity(Long expectedWorkOrderQuantity) {
        this.expectedWorkOrderQuantity = expectedWorkOrderQuantity;
    }
}

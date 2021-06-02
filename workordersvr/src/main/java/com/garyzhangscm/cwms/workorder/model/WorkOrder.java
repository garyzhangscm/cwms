package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "work_order")
public class WorkOrder extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderLine> workOrderLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderInstruction> workOrderInstructions = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderKPI> workOrderKPIs = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderByProduct> workOrderByProducts = new ArrayList<>();

    /***
        @ManyToOne
        @JoinColumn(name = "production_line_id")
        private ProductionLine productionLine;

     We will switch to a 1 to Many relationship between
     work order and production line so that one work order
     can have multiple production line. Some warehouse will use
     ***/

    @OneToMany(mappedBy = "workOrder")
    private List<ProductionLineAssignment> productionLineAssignments = new ArrayList<>();

    @Column(name = "item_id")
    private Long itemId;


    // When the work order is created by
    // following a specific BOM
    @ManyToOne
    @JoinColumn(name = "bill_of_material_id")
    private BillOfMaterial billOfMaterial;

    // When the work order is created from a specific
    // production plan
    @ManyToOne
    @JoinColumn(name = "production_plan_line_id")
    private ProductionPlanLine productionPlanLine;


    @Transient
    private Item item;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @Column(name = "produced_quantity")
    private Long producedQuantity;



    @OneToMany(mappedBy = "workOrder")
    private List<WorkOrderAssignment> assignments = new ArrayList<>();

    @Column(name = "status")
    private WorkOrderStatus status;


    // Some statistics numbers that we can show
    // in the frontend
    @Transient
    private Integer totalLineCount;
    @Transient
    private Integer totalItemCount;
    @Transient
    private Long totalExpectedQuantity;
    @Transient
    private Long totalOpenQuantity; // Open quantity that is not allocated yet
    @Transient
    private Long totalOpenPickQuantity;
    @Transient
    private Long totalPickedQuantity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrder workOrder = (WorkOrder) o;
        if (Objects.equals(id, workOrder.id)) {
            return true;
        }
        return Objects.equals(number, workOrder.number) &&
                Objects.equals(warehouseId, workOrder.warehouseId);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, warehouseId);
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

    public List<WorkOrderLine> getWorkOrderLines() {
        return workOrderLines;
    }

    public void setWorkOrderLines(List<WorkOrderLine> workOrderLines) {
        this.workOrderLines = workOrderLines;
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

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public List<WorkOrderInstruction> getWorkOrderInstructions() {
        return workOrderInstructions;
    }

    public void setWorkOrderInstructions(List<WorkOrderInstruction> workOrderInstructions) {
        this.workOrderInstructions = workOrderInstructions;
    }

    public List<ProductionLineAssignment> getProductionLineAssignments() {
        return productionLineAssignments;
    }

    public void setProductionLineAssignments(List<ProductionLineAssignment> productionLineAssignments) {
        this.productionLineAssignments = productionLineAssignments;
    }

    public List<WorkOrderAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<WorkOrderAssignment> assignments) {
        this.assignments = assignments;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public BillOfMaterial getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(BillOfMaterial billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public List<WorkOrderKPI> getWorkOrderKPIs() {
        return workOrderKPIs;
    }

    public void setWorkOrderKPIs(List<WorkOrderKPI> workOrderKPIs) {
        this.workOrderKPIs = workOrderKPIs;
    }

    public List<WorkOrderByProduct> getWorkOrderByProducts() {
        return workOrderByProducts;
    }

    public void setWorkOrderByProducts(List<WorkOrderByProduct> workOrderByProducts) {
        this.workOrderByProducts = workOrderByProducts;
    }

    public ProductionPlanLine getProductionPlanLine() {
        return productionPlanLine;
    }

    public void setProductionPlanLine(ProductionPlanLine productionPlanLine) {
        this.productionPlanLine = productionPlanLine;
    }

    public Integer getTotalLineCount() {
        return totalLineCount;
    }

    public void setTotalLineCount(Integer totalLineCount) {
        this.totalLineCount = totalLineCount;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public Long getTotalExpectedQuantity() {
        return totalExpectedQuantity;
    }

    public void setTotalExpectedQuantity(Long totalExpectedQuantity) {
        this.totalExpectedQuantity = totalExpectedQuantity;
    }

    public Long getTotalOpenQuantity() {
        return totalOpenQuantity;
    }

    public void setTotalOpenQuantity(Long totalOpenQuantity) {
        this.totalOpenQuantity = totalOpenQuantity;
    }

    public Long getTotalOpenPickQuantity() {
        return totalOpenPickQuantity;
    }

    public void setTotalOpenPickQuantity(Long totalOpenPickQuantity) {
        this.totalOpenPickQuantity = totalOpenPickQuantity;
    }

    public Long getTotalPickedQuantity() {
        return totalPickedQuantity;
    }

    public void setTotalPickedQuantity(Long totalPickedQuantity) {
        this.totalPickedQuantity = totalPickedQuantity;
    }
}

package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    @Column(name = "item_id")
    private Long itemId;


    @ManyToOne
    @JoinColumn(name = "bill_of_material_id")
    private BillOfMaterial billOfMaterial;

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

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
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
}

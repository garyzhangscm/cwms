package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "work_order_produced_inventory_result")
public class WorkOrderProducedInventoryResult extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_produced_inventory_result_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_produced_inventory_id")
    @JsonIgnore
    private WorkOrderProducedInventory workOrderProducedInventory;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "lpn")
    private String lpn;

    @Column(name = "result")
    private Boolean result;

    @Column(name = "error_message")
    private String errorMessage;

    public WorkOrderProducedInventoryResult(){}

    public WorkOrderProducedInventoryResult(Long warehouseId, WorkOrderProducedInventory workOrderProducedInventory){
        this.workOrderProducedInventory = workOrderProducedInventory;
        this.lpn = workOrderProducedInventory.getLpn();
        this.warehouseId = warehouseId;

    }
    public WorkOrderProducedInventoryResult(Long warehouseId,
                                            WorkOrderProducedInventory workOrderProducedInventory,
                                            boolean result, String errorMessage){
        this.warehouseId = warehouseId;
        this.workOrderProducedInventory = workOrderProducedInventory;
        this.lpn = workOrderProducedInventory.getLpn();
        this.result = result;
        this.errorMessage = errorMessage;

    }

    public void produceInventorySuccess() {
        setResult(true);
        setErrorMessage("");
    }
    public void produceInventoryFail(String errorMessage) {
        setResult(false);
        setErrorMessage(errorMessage);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderProducedInventory getWorkOrderProducedInventory() {
        return workOrderProducedInventory;
    }

    public void setWorkOrderProducedInventory(WorkOrderProducedInventory workOrderProducedInventory) {
        this.workOrderProducedInventory = workOrderProducedInventory;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}

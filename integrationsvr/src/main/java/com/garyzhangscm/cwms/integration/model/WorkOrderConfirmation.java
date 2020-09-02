package com.garyzhangscm.cwms.integration.model;

import java.util.ArrayList;
import java.util.List;

public class WorkOrderConfirmation  {

    private String number;

    List<WorkOrderLineConfirmation> workOrderLineConfirmations = new ArrayList<>();
    List<WorkOrderByProductConfirmation> workOrderByProductConfirmations = new ArrayList<>();

    private String productionLineName;


    private Long itemId;
    private String itemName;


    private Long warehouseId;
    private String warehouseName;

    private String billOfMaterialName;


    private Long expectedQuantity;

    private Long producedQuantity;


    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<WorkOrderLineConfirmation> getWorkOrderLines() {
        return workOrderLineConfirmations;
    }

    public void setWorkOrderLines(List<WorkOrderLineConfirmation> workOrderLines) {
        this.workOrderLineConfirmations = workOrderLines;
    }

    public List<WorkOrderByProductConfirmation> getWorkOrderByProducts() {
        return workOrderByProductConfirmations;
    }

    public void setWorkOrderByProducts(List<WorkOrderByProductConfirmation> workOrderByProductConfirmations) {
        this.workOrderByProductConfirmations = workOrderByProductConfirmations;
    }

    public String getProductionLineName() {
        return productionLineName;
    }

    public void setProductionLineName(String productionLineName) {
        this.productionLineName = productionLineName;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getBillOfMaterialName() {
        return billOfMaterialName;
    }

    public void setBillOfMaterialName(String billOfMaterialName) {
        this.billOfMaterialName = billOfMaterialName;
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
}

package com.garyzhangscm.cwms.workorder.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorkOrderConfirmation  {

    private String number;

    List<WorkOrderLineConfirmation> workOrderLines = new ArrayList<>();

    List<WorkOrderByProductConfirmation> workOrderByProducts = new ArrayList<>();

    private String productionLineName;


    private Long itemId;
    private String itemName;


    private Long warehouseId;
    private String warehouseName;

    private String billOfMaterialName;


    private Long expectedQuantity;

    private Long producedQuantity;

    public WorkOrderConfirmation(){}
    public WorkOrderConfirmation(WorkOrder workOrder){

        setNumber(workOrder.getNumber());


/***
        if (Objects.nonNull(workOrder.getProductionLine())) {
            setProductionLineName(workOrder.getProductionLine().getName());
        }
**/

        setItemId(workOrder.getItemId());
        if (Objects.nonNull(workOrder.getItem())) {
            setItemName(workOrder.getItem().getName());
        }


        setWarehouseId(workOrder.getWarehouseId());
        if (Objects.nonNull(workOrder.getWarehouse())) {
            setWarehouseName(workOrder.getWarehouse().getName());
        }

        if (Objects.nonNull(workOrder.getBillOfMaterial())) {
            setBillOfMaterialName(workOrder.getBillOfMaterial().getNumber());
        }


        setExpectedQuantity(workOrder.getExpectedQuantity());

        setProducedQuantity(workOrder.getProducedQuantity());

        workOrder.getWorkOrderLines().forEach(workOrderLine -> {
            WorkOrderLineConfirmation workOrderLineConfirmation = new WorkOrderLineConfirmation(workOrderLine);
            addWorkOrderLine(workOrderLineConfirmation);
        });

        workOrder.getWorkOrderByProducts().forEach(workOrderByProduct -> {
            WorkOrderByProductConfirmation workOrderByProductConfirmation = new WorkOrderByProductConfirmation(workOrderByProduct);
            addWorkOrderByProduct(workOrderByProductConfirmation);
        });
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<WorkOrderLineConfirmation> getWorkOrderLines() {
        return workOrderLines;
    }

    public void setWorkOrderLines(List<WorkOrderLineConfirmation> workOrderLines) {
        this.workOrderLines = workOrderLines;
    }
    public void addWorkOrderLine(WorkOrderLineConfirmation workOrderLine) {
        this.workOrderLines.add(workOrderLine);
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

    public List<WorkOrderByProductConfirmation> getWorkOrderByProducts() {
        return workOrderByProducts;
    }

    public void setWorkOrderByProducts(List<WorkOrderByProductConfirmation> workOrderByProducts) {
        this.workOrderByProducts = workOrderByProducts;
    }

    public void addWorkOrderByProduct(WorkOrderByProductConfirmation workOrderByProduct) {
        this.workOrderByProducts.add(workOrderByProduct);
    }
}

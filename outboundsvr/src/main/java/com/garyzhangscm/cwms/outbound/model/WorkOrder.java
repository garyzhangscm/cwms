package com.garyzhangscm.cwms.outbound.model;


import java.util.ArrayList;
import java.util.List;


public class WorkOrder {


    private Long id;


    private String number;


    List<WorkOrderLine> workOrderLines = new ArrayList<>();

    private Long itemId;


    private Item item;


    private Long warehouseId;

    private Warehouse warehouse;

    private Long expectedQuantity;

    private Long producedQuantity;

    private ProductionLine productionLine;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("number: ").append(number).append("\n")
                .append("workOrderLines: ").append(workOrderLines).append("\n")
                .append("itemId: ").append(itemId).append("\n")
                .append("item: ").append(item).append("\n")
                .append("warehouseId: ").append(warehouseId).append("\n")
                .append("warehouse: ").append(warehouse).append("\n")
                .append("expectedQuantity: ").append(expectedQuantity).append("\n")
                .append("producedQuantity: ").append(producedQuantity).append("\n")
                .append("productionLine: ").append(productionLine).append("\n")
                .toString();
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

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }
}

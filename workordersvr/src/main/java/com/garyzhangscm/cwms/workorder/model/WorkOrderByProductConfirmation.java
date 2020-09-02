package com.garyzhangscm.cwms.workorder.model;


import java.util.Objects;

public class WorkOrderByProductConfirmation {

    private Long itemId;
    private String itemName;

    private Long expectedQuantity;
    private Long producedQuantity;


    private Long inventoryStatusId;
    private String inventoryStatusName;


    public WorkOrderByProductConfirmation(){}
    public WorkOrderByProductConfirmation(WorkOrderByProduct workOrderByProduct){



        setItemId(workOrderByProduct.getItemId());
        if (Objects.nonNull(workOrderByProduct.getItem())) {
            setItemName(workOrderByProduct.getItem().getName());
        }
        setExpectedQuantity(workOrderByProduct.getExpectedQuantity());
        setProducedQuantity(workOrderByProduct.getProducedQuantity());


        setInventoryStatusId(workOrderByProduct.getInventoryStatusId());
        if (Objects.nonNull(workOrderByProduct.getInventoryStatus())) {

            setInventoryStatusName(workOrderByProduct.getInventoryStatus().getName());
        }

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

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }
}

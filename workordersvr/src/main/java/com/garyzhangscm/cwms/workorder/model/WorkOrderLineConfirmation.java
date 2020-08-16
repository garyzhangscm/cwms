package com.garyzhangscm.cwms.workorder.model;


import java.util.Objects;

public class WorkOrderLineConfirmation  {


    private String number;

    private Long itemId;
    private String itemName;


    private Long expectedQuantity;
    private Long openQuantity;
    private Long inprocessQuantity;
    private Long deliveredQuantity;
    private Long consumedQuantity;
    private Long scrappedQuantity;
    private Long returnedQuantity;


    private Long inventoryStatusId;
    private String inventoryStatusName;


    public WorkOrderLineConfirmation(){}
    public WorkOrderLineConfirmation(WorkOrderLine workOrderLine){


        setNumber(workOrderLine.getNumber());

        setItemId(workOrderLine.getItemId());
        if (Objects.nonNull(workOrderLine.getItem())) {
            setItemName(workOrderLine.getItem().getName());
        }
        setExpectedQuantity(workOrderLine.getExpectedQuantity());
        setOpenQuantity(workOrderLine.getOpenQuantity());
        setInprocessQuantity(workOrderLine.getInprocessQuantity());
        setDeliveredQuantity(workOrderLine.getDeliveredQuantity());
        setConsumedQuantity(workOrderLine.getConsumedQuantity());
        setScrappedQuantity(workOrderLine.getScrappedQuantity());
        setReturnedQuantity(workOrderLine.getReturnedQuantity());


        setInventoryStatusId(workOrderLine.getInventoryStatusId());
        if (Objects.nonNull(workOrderLine.getInventoryStatus())) {

            setInventoryStatusName(workOrderLine.getInventoryStatus().getName());
        }

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

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public Long getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(Long scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }

    public Long getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(Long returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
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

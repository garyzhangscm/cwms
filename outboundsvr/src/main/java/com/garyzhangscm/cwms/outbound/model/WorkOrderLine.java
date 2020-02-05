package com.garyzhangscm.cwms.outbound.model;

import javax.persistence.*;

public class WorkOrderLine {
    private Long id;

    private String number;

    private Long itemId;

    @Transient
    private Item item;

    private Long expectedQuantity;

    private Long openQuantity;

    private Long inprocessQuantity;

    private Long consumedQuantity;
    private Long inventoryStatusId;

    private InventoryStatus inventoryStatus;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("id: ").append(id).append("\n")
                .append("number: ").append(number).append("\n")
                .append("itemId: ").append(itemId).append("\n")
                .append("item: ").append(item).append("\n")
                .append("expectedQuantity: ").append(expectedQuantity).append("\n")
                .append("openQuantity: ").append(openQuantity).append("\n")
                .append("inprocessQuantity: ").append(inprocessQuantity).append("\n")
                .append("consumedQuantity: ").append(consumedQuantity).append("\n")
                .append("inventoryStatusId: ").append(inventoryStatusId).append("\n")
                .append("inventoryStatus: ").append(inventoryStatus).append("\n")
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

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }
}

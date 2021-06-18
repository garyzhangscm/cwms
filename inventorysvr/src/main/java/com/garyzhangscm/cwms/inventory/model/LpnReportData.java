package com.garyzhangscm.cwms.inventory.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LpnReportData {
    String productionLocation;
    String itemName;
    String itemDescription;
    String workOrderNumber;
    String completeDate;
    Long quantity;
    String lpn;
    String poNumber;
    String supervisor;

    public LpnReportData(){};

    public LpnReportData(Inventory inventory, String username){
        this.productionLocation = Objects.nonNull(inventory.getLocation()) ?
                inventory.getLocation().getName() : "";
        this.itemName = inventory.getItem().getName();
        this.itemDescription = inventory.getItem().getDescription();
        this.workOrderNumber = Objects.nonNull(inventory.getWorkOrder()) ?
                inventory.getWorkOrder().getNumber() : "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.completeDate = LocalDateTime.now().format(formatter);
        this.quantity = inventory.getQuantity();
        this.lpn = inventory.getLpn();
        this.poNumber = Objects.nonNull(inventory.getWorkOrder()) ?
                inventory.getWorkOrder().getPoNumber() : "";
        this.supervisor = username;

    };

    public void addQuantity(Long quantity) {
        this.quantity += quantity;
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

    public String getProductionLocation() {
        return productionLocation;
    }

    public void setProductionLocation(String productionLocation) {
        this.productionLocation = productionLocation;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public String getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(String completeDate) {
        this.completeDate = completeDate;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }
}

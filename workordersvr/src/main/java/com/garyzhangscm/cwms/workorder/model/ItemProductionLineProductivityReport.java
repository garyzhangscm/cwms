package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public class ItemProductionLineProductivityReport {

    private long warehouseId;

    private String itemName;
    private String itemFamilyName;

    private String productionLineName;

    private long realTimeGoal;

    private int actualPalletQuantity;

    private long actualQuantity;

    // if the item is still on the production line, then
    // the expected produced quantity is the quantity that
    // is supposed to be produced within a time range
    // otherwise, it is the expected produced quantity for
    // the time when the item is on the production line
    private long expectedProducedQuantity;


    public ItemProductionLineProductivityReport(){}

    public ItemProductionLineProductivityReport(long warehouseId, String itemName, String itemFamilyName, String productionLineName ) {
        this.warehouseId = warehouseId;
        this.itemName = itemName;
        this.itemFamilyName = itemFamilyName;
        this.productionLineName = productionLineName;
    }

    public ItemProductionLineProductivityReport(long warehouseId, String itemName, String itemFamilyName,
                                                String productionLineName, long realTimeGoal, int actualPalletQuantity,
                                                long actualQuantity) {
        this.warehouseId = warehouseId;
        this.itemName = itemName;
        this.itemFamilyName = itemFamilyName;
        this.productionLineName = productionLineName;
        this.realTimeGoal = realTimeGoal;
        this.actualPalletQuantity = actualPalletQuantity;
        this.actualQuantity = actualQuantity;
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

    public long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }



    public long getRealTimeGoal() {
        return realTimeGoal;
    }

    public void setRealTimeGoal(long realTimeGoal) {
        this.realTimeGoal = realTimeGoal;
    }

    public int getActualPalletQuantity() {
        return actualPalletQuantity;
    }

    public void setActualPalletQuantity(int actualPalletQuantity) {
        this.actualPalletQuantity = actualPalletQuantity;
    }

    public long getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(long actualQuantity) {
        this.actualQuantity = actualQuantity;
    }


    public String getItemFamilyName() {
        return itemFamilyName;
    }

    public void setItemFamilyName(String itemFamilyName) {
        this.itemFamilyName = itemFamilyName;
    }

    public long getExpectedProducedQuantity() {
        return expectedProducedQuantity;
    }

    public void setExpectedProducedQuantity(long expectedProducedQuantity) {
        this.expectedProducedQuantity = expectedProducedQuantity;
    }

    public String getProductionLineName() {
        return productionLineName;
    }

    public void setProductionLineName(String productionLineName) {
        this.productionLineName = productionLineName;
    }
}

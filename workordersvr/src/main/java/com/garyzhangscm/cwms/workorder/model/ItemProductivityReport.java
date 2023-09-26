package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemProductivityReport {

    private long warehouseId;

    private String itemName;
    private String itemFamilyName;

    private Set<String> productionLineNames = new HashSet<>();

    private long realTimeGoal;

    private int actualPalletQuantity;

    private long actualQuantity;


    private double finishRate;

    // estimated finish rate based on the estimation
    // of productivity and the hours already passed in this shift
    private double estimatedFinishRate;

    List<ItemProductionLineProductivityReport> itemProductionLineProductivityReports = new ArrayList<>();

    public ItemProductivityReport(){}

    public ItemProductivityReport(long warehouseId, String itemName, String itemFamilyName) {
        this.warehouseId = warehouseId;
        this.itemName = itemName;
        this.itemFamilyName = itemFamilyName;
        this.productionLineNames = new HashSet<>();
    }

    public ItemProductivityReport(long warehouseId, String itemName, String itemFamilyName,
                                  Set<String> productionLineNames, long realTimeGoal, int actualPalletQuantity,
                                  long actualQuantity, double finishRate, double estimatedFinishRate) {
        this.warehouseId = warehouseId;
        this.itemName = itemName;
        this.itemFamilyName = itemFamilyName;
        this.productionLineNames = productionLineNames;
        this.realTimeGoal = realTimeGoal;
        this.actualPalletQuantity = actualPalletQuantity;
        this.actualQuantity = actualQuantity;
        this.finishRate = finishRate;
        this.estimatedFinishRate = estimatedFinishRate;
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

    public Set<String> getProductionLineNames() {
        return productionLineNames;
    }

    public void setProductionLineNames(Set<String> productionLineNames) {
        this.productionLineNames = productionLineNames;
    }
    public void addProductionLineName(String productionLineName) {
        this.productionLineNames.add(productionLineName);
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

    public double getFinishRate() {
        return finishRate;
    }

    public void setFinishRate(double finishRate) {
        this.finishRate = finishRate;
    }

    public double getEstimatedFinishRate() {
        return estimatedFinishRate;
    }

    public void setEstimatedFinishRate(double estimatedFinishRate) {
        this.estimatedFinishRate = estimatedFinishRate;
    }

    public String getItemFamilyName() {
        return itemFamilyName;
    }

    public void setItemFamilyName(String itemFamilyName) {
        this.itemFamilyName = itemFamilyName;
    }


    public List<ItemProductionLineProductivityReport> getItemProductionLineProductivityReports() {
        return itemProductionLineProductivityReports;
    }

    public void setItemProductionLineProductivityReports(List<ItemProductionLineProductivityReport> itemProductionLineProductivityReports) {
        this.itemProductionLineProductivityReports = itemProductionLineProductivityReports;
    }

    public void addItemProductionLineProductivityReport(ItemProductionLineProductivityReport itemProductionLineProductivityReport) {
        this.itemProductionLineProductivityReports.add(itemProductionLineProductivityReport);

        this.productionLineNames.add(itemProductionLineProductivityReport.getProductionLineName());

        // calculate all the quantites
        this.realTimeGoal += itemProductionLineProductivityReport.getRealTimeGoal();

        this.actualPalletQuantity += itemProductionLineProductivityReport.getActualPalletQuantity();

        this.actualQuantity += itemProductionLineProductivityReport.getActualQuantity();

        this.finishRate = this.actualQuantity * 1.0 / this.realTimeGoal;

        long totalExpectedProducedQuantity = this.itemProductionLineProductivityReports.stream().mapToLong(
                ItemProductionLineProductivityReport::getExpectedProducedQuantity
        ).sum();

        this.estimatedFinishRate = this.realTimeGoal * 1.0 / totalExpectedProducedQuantity;
    }
}

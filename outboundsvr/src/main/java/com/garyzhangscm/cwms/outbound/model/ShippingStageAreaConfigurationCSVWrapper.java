package com.garyzhangscm.cwms.outbound.model;




public class ShippingStageAreaConfigurationCSVWrapper {

    private Integer sequence;

    private String locationGroup;

    private String locationReserveStrategy;
    private String warehouse;

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(String locationGroup) {
        this.locationGroup = locationGroup;
    }

    public String getLocationReserveStrategy() {
        return locationReserveStrategy;
    }

    public void setLocationReserveStrategy(String locationReserveStrategy) {
        this.locationReserveStrategy = locationReserveStrategy;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
}

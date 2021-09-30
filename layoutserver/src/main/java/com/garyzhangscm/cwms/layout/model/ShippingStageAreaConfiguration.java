package com.garyzhangscm.cwms.layout.model;

public class ShippingStageAreaConfiguration extends AuditibleEntity<String>{

    private Long id;

    private Integer sequence;

    private Long warehouseId;

    private Warehouse warehouse;


    private Long locationGroupId;

    private LocationGroup locationGroup;

    private ShippingStageLocationReserveStrategy locationReserveStrategy;

    public ShippingStageAreaConfiguration(){}

    public ShippingStageAreaConfiguration(Integer sequence,
                                          Long warehouseId, Warehouse warehouse,
                                          Long locationGroupId, LocationGroup locationGroup,
                                          ShippingStageLocationReserveStrategy locationReserveStrategy) {
        this.sequence = sequence;
        this.warehouseId = warehouseId;
        this.warehouse = warehouse;
        this.locationGroupId = locationGroupId;
        this.locationGroup = locationGroup;
        this.locationReserveStrategy = locationReserveStrategy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getLocationGroupId() {
        return locationGroupId;
    }

    public void setLocationGroupId(Long locationGroupId) {
        this.locationGroupId = locationGroupId;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public ShippingStageLocationReserveStrategy getLocationReserveStrategy() {
        return locationReserveStrategy;
    }

    public void setLocationReserveStrategy(ShippingStageLocationReserveStrategy locationReserveStrategy) {
        this.locationReserveStrategy = locationReserveStrategy;
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
}

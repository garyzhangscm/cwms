package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "shipping_stage_area_configuration")
public class ShippingStageAreaConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_stage_area_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name="sequence")
    private Integer sequence;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    // criteria
    // TO-DO


    // location group
    @Column(name="location_group_id")
    private Long locationGroupId;

    @Transient
    private LocationGroup locationGroup;

    @Column(name="location_reserve_strategy")
    private ShippingStageLocationReserveStrategy locationReserveStrategy;

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

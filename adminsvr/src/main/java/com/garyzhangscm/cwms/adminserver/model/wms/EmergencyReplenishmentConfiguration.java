package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

public class EmergencyReplenishmentConfiguration {

    private Long id;

    private Integer sequence;

    private Long unitOfMeasureId;
    private UnitOfMeasure unitOfMeasure;

    private Long itemId;
    private Item item;

    private Long warehouseId;
    private Warehouse warehouse;

    private Long itemFamilyId;
    private ItemFamily itemFamily;

    private Long sourceLocationId;
    private Location sourceLocation;

    private Long sourceLocationGroupId;
    private LocationGroup sourceLocationGroup;


    private Long destinationLocationId;
    private Location destinationLocation;

    private Long destinationLocationGroupId;
    private LocationGroup destinationLocationGroup;

    public EmergencyReplenishmentConfiguration(){}

    public EmergencyReplenishmentConfiguration(Integer sequence,
                                               Warehouse warehouse,
                                               UnitOfMeasure unitOfMeasure,
                                               ItemFamily itemFamily,
                                               LocationGroup destinationLocationGroup){

        this.sequence = sequence;

        this.unitOfMeasureId = unitOfMeasure.getId();
        this.unitOfMeasure = unitOfMeasure;

        this.warehouseId = warehouse.getId();
        this.warehouse = warehouse;

        this.itemFamilyId = itemFamily.getId();
        this.itemFamily = itemFamily;


        this.destinationLocationGroupId = destinationLocationGroup.getId();
        this.destinationLocationGroup = destinationLocationGroup;
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

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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

    public Long getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(Long itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public Long getSourceLocationGroupId() {
        return sourceLocationGroupId;
    }

    public void setSourceLocationGroupId(Long sourceLocationGroupId) {
        this.sourceLocationGroupId = sourceLocationGroupId;
    }

    public LocationGroup getSourceLocationGroup() {
        return sourceLocationGroup;
    }

    public void setSourceLocationGroup(LocationGroup sourceLocationGroup) {
        this.sourceLocationGroup = sourceLocationGroup;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public Long getDestinationLocationGroupId() {
        return destinationLocationGroupId;
    }

    public void setDestinationLocationGroupId(Long destinationLocationGroupId) {
        this.destinationLocationGroupId = destinationLocationGroupId;
    }

    public LocationGroup getDestinationLocationGroup() {
        return destinationLocationGroup;
    }

    public void setDestinationLocationGroup(LocationGroup destinationLocationGroup) {
        this.destinationLocationGroup = destinationLocationGroup;
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

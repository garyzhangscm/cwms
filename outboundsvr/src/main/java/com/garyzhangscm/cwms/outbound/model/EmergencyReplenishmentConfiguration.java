package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

// This is the table to configuration the destination
// of a emregency replenishment
@Entity
@Table(name = "emergency_replenishment_configuration")
public class EmergencyReplenishmentConfiguration {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emergency_replenishment_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Transient
    private UnitOfMeasure unitOfMeasure;

    // Criteria: Item / Item Family / Source Location / Source Location Group
    // uom level
    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "item_family_id")
    private Long itemFamilyId;

    @Transient
    private ItemFamily itemFamily;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Transient
    private Location sourceLocation;

    @Column(name = "source_location_group_id")
    private Long sourceLocationGroupId;
    @Transient
    private LocationGroup sourceLocationGroup;



    // Destination / Destination Location Group
    // Anything that match the above criteria
    // will go to the destination location / location group

    @Column(name = "destination_location_id")
    private Long destinationLocationId;

    @Transient
    private Location destinationLocation;

    @Column(name = "destination_location_group_id")
    private Long destinationLocationGroupId;
    @Transient
    private LocationGroup destinationLocationGroup;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
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

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}

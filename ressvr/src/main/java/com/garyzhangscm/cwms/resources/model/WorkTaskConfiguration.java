package com.garyzhangscm.cwms.resources.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "work_task_configuration")
public class WorkTaskConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_task_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    // criteria
    // 1. source location group type
    // 2. source location group
    // 3. source location
    // 4. destination location group type
    // 5. destination location group
    // 6. destination location
    // 7. work task type
    @Column(name = "source_location_group_type_id")
    private Long sourceLocationGroupTypeId;
    @Transient
    private LocationGroupType sourceLocationGroupType;

    @Column(name = "source_location_group_id")
    private Long sourceLocationGroupId;
    @Transient
    private LocationGroup sourceLocationGroup;

    @Column(name = "source_location_id")
    private Long sourceLocationId;
    @Transient
    private Location sourceLocation;

    @Column(name = "destination_location_group_type_id")
    private Long destinationLocationGroupTypeId;
    @Transient
    private LocationGroupType destinationLocationGroupType;

    @Column(name = "destination_location_group_id")
    private Long destinationLocationGroupId;
    @Transient
    private LocationGroup destinationLocationGroup;

    @Column(name = "destination_location_id")
    private Long destinationLocationId;
    @Transient
    private Location destinationLocation;

    @Column(name = "work_task_type")
    @Enumerated(EnumType.STRING)
    private WorkTaskType workTaskType;

    // result of the work task
    // 1. operation type
    // 2. priority

    @ManyToOne
    @JoinColumn(name="operation_type_id")
    private OperationType operationType;

    @Column(name = "priority")
    private Integer priority;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getSourceLocationGroupTypeId() {
        return sourceLocationGroupTypeId;
    }

    public void setSourceLocationGroupTypeId(Long sourceLocationGroupTypeId) {
        this.sourceLocationGroupTypeId = sourceLocationGroupTypeId;
    }

    public LocationGroupType getSourceLocationGroupType() {
        return sourceLocationGroupType;
    }

    public void setSourceLocationGroupType(LocationGroupType sourceLocationGroupType) {
        this.sourceLocationGroupType = sourceLocationGroupType;
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

    public Long getDestinationLocationGroupTypeId() {
        return destinationLocationGroupTypeId;
    }

    public void setDestinationLocationGroupTypeId(Long destinationLocationGroupTypeId) {
        this.destinationLocationGroupTypeId = destinationLocationGroupTypeId;
    }

    public LocationGroupType getDestinationLocationGroupType() {
        return destinationLocationGroupType;
    }

    public void setDestinationLocationGroupType(LocationGroupType destinationLocationGroupType) {
        this.destinationLocationGroupType = destinationLocationGroupType;
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

    public WorkTaskType getWorkTaskType() {
        return workTaskType;
    }

    public void setWorkTaskType(WorkTaskType workTaskType) {
        this.workTaskType = workTaskType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}

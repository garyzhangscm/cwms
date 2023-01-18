package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "work_task")
public class WorkTask extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_task_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "number")
    private String number;


    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private WorkType workType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WorkStatus workStatus;


    @Column(name = "source_location_id")
    private Long sourceLocationId;
    @Transient
    private Location sourceLocation;


    @Column(name = "destination_location_id")
    private Long destinationLocationId;
    @Transient
    private Location destinationLocation;


    @Column(name = "inventory_id")
    private Long inventoryId;
    @Transient
    private Inventory inventory;


    @Column(name = "assigned_user_id")
    private Long assignedUserId;
    @Transient
    private User assignedUser;

    @Column(name = "assigned_role_id")
    private Long assignedRoleId;
    @Transient
    private Role assignedRole;

    @Column(name = "assigned_working_team_id")
    private Long assignedWorkingTeamId;
    @Transient
    private WorkingTeam assignedWorkingTeam;


    // User who is working on this work now
    @Column(name = "current_user_id")
    private Long currentUserId;
    @Transient
    private User currentUser;

    // User who complete this work
    @Column(name = "complete_user_id")
    private Long completeUserId;
    @Transient
    private User completeUser;


    // User who complete this work
    @Column(name = "start_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;
    // private LocalDateTime startTime;

    @Column(name = "complete_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime completeTime;
    // private LocalDateTime completeTime;


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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public WorkStatus getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(WorkStatus workStatus) {
        this.workStatus = workStatus;
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

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public Long getAssignedRoleId() {
        return assignedRoleId;
    }

    public void setAssignedRoleId(Long assignedRoleId) {
        this.assignedRoleId = assignedRoleId;
    }

    public Long getAssignedWorkingTeamId() {
        return assignedWorkingTeamId;
    }

    public void setAssignedWorkingTeamId(Long assignedWorkingTeamId) {
        this.assignedWorkingTeamId = assignedWorkingTeamId;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public Long getCompleteUserId() {
        return completeUserId;
    }

    public void setCompleteUserId(Long completeUserId) {
        this.completeUserId = completeUserId;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(ZonedDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public User getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(User assignedUser) {
        this.assignedUser = assignedUser;
    }

    public Role getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(Role assignedRole) {
        this.assignedRole = assignedRole;
    }

    public WorkingTeam getAssignedWorkingTeam() {
        return assignedWorkingTeam;
    }

    public void setAssignedWorkingTeam(WorkingTeam assignedWorkingTeam) {
        this.assignedWorkingTeam = assignedWorkingTeam;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public User getCompleteUser() {
        return completeUser;
    }

    public void setCompleteUser(User completeUser) {
        this.completeUser = completeUser;
    }
}

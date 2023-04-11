package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
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


    @Column(name = "work_task_type")
    @Enumerated(EnumType.STRING)
    private WorkTaskType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WorkTaskStatus status;


    @Column(name = "priority")
    private Integer priority;

    @Column(name = "source_location_id")
    private Long sourceLocationId;
    @Transient
    private Location sourceLocation;


    @Column(name = "destination_location_id")
    private Long destinationLocationId;
    @Transient
    private Location destinationLocation;


    // bulk pick number if the task is bulk pick
    // list pick number if the task is list pick
    // pick number if the task is pick
    // LPN is the task is inventory related
    @Column(name = "reference_number")
    private String referenceNumber;


    @ManyToOne
    @JoinColumn(name="assigned_user_id", referencedColumnName="user_id")
    private User assignedUser;

    @ManyToOne
    @JoinColumn(name="assigned_role_id", referencedColumnName="role_id")
    private Role assignedRole;


    @ManyToOne
    @JoinColumn(name="assigned_working_team_id", referencedColumnName="working_team_id")
    private WorkingTeam assignedWorkingTeam;

    // User who is working on this work now
    @ManyToOne
    @JoinColumn(name="current_user_id", referencedColumnName="user_id")
    private User currentUser;

    // User who complete this work
    @ManyToOne
    @JoinColumn(name="complete_user_id", referencedColumnName="user_id")
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public WorkTaskType getType() {
        return type;
    }

    public void setType(WorkTaskType type) {
        this.type = type;
    }

    public WorkTaskStatus getStatus() {
        return status;
    }

    public void setStatus(WorkTaskStatus status) {
        this.status = status;
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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
}

package com.garyzhangscm.cwms.outbound.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;

public class WorkTask extends AuditibleEntity<String> {

    private Long id;

    private Long warehouseId;

    private String number;

    private WorkTaskType type;

    private WorkTaskStatus status;

    private Long sourceLocationId;

    private Long destinationLocationId;

    // bulk pick number if the task is bulk pick
    // list pick number if the task is list pick
    // pick number if the task is pick
    // LPN is the task is inventory related
    private String referenceNumber;


    private OperationType operationType;

    private User assignedUser;

    private Role assignedRole;

    private User currentUser;

    private User completeUser;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;
    // private LocalDateTime startTime;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime completeTime;

    public WorkTask() {

    }
    public WorkTask(Long warehouseId, String number, WorkTaskType type,
                    WorkTaskStatus status, Long sourceLocationId,
                    Long destinationLocationId, String referenceNumber) {
        this.warehouseId = warehouseId;
        this.number = number;
        this.type = type;
        this.status = status;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.referenceNumber = referenceNumber;
    }


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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
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

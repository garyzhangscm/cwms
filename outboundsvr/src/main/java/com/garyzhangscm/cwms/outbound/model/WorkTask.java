package com.garyzhangscm.cwms.outbound.model;


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
}
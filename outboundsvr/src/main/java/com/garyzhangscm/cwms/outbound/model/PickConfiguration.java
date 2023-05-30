package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "pick_configuration")
public class PickConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "release_to_work_task")
    private Boolean releaseToWorkTask;
    @Column(name = "work_task_priority")
    private Integer workTaskPriority;

    @Column(name = "release_pick_list_to_work_task")
    private Boolean releasePickListToWorkTask;
    @Column(name = "pick_list_work_task_priority")
    private Integer pickListWorkTaskPriority;

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


    public Boolean getReleaseToWorkTask() {
        return releaseToWorkTask;
    }

    public void setReleaseToWorkTask(Boolean releaseToWorkTask) {
        this.releaseToWorkTask = releaseToWorkTask;
    }

    public Integer getWorkTaskPriority() {
        return workTaskPriority;
    }

    public void setWorkTaskPriority(Integer workTaskPriority) {
        this.workTaskPriority = workTaskPriority;
    }

    public Boolean getReleasePickListToWorkTask() {
        return releasePickListToWorkTask;
    }

    public void setReleasePickListToWorkTask(Boolean releasePickListToWorkTask) {
        this.releasePickListToWorkTask = releasePickListToWorkTask;
    }

    public Integer getPickListWorkTaskPriority() {
        return pickListWorkTaskPriority;
    }

    public void setPickListWorkTaskPriority(Integer pickListWorkTaskPriority) {
        this.pickListWorkTaskPriority = pickListWorkTaskPriority;
    }
}

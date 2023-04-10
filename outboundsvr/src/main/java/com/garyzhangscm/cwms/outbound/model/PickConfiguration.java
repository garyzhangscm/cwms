package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.domain.Sort;

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
}

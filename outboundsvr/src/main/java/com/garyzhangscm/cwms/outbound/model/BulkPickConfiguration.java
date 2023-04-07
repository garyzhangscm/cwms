package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.domain.Sort;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bulk_pick_configuration")
public class BulkPickConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bulk_pick_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "enabled_for_outbound")
    private Boolean enabledForOutbound;

    @Column(name = "enabled_for_work_order")
    private Boolean enabledForWorkOrder;

    // when we get a group of candidate picks, how we
    // sort the picks (from biggest pick quantity to smallest
    // or from smallest to biggest) before we can group them into
    // several bulk picks
    @Column(name = "pick_sort_direction")
    @Enumerated(EnumType.STRING)
    private Sort.Direction pickSortDirection;

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

    public Sort.Direction getPickSortDirection() {
        return pickSortDirection;
    }

    public void setPickSortDirection(Sort.Direction pickSortDirection) {
        this.pickSortDirection = pickSortDirection;
    }

    public Boolean getEnabledForOutbound() {
        return enabledForOutbound;
    }

    public void setEnabledForOutbound(Boolean enabledForOutbound) {
        this.enabledForOutbound = enabledForOutbound;
    }

    public Boolean getEnabledForWorkOrder() {
        return enabledForWorkOrder;
    }

    public void setEnabledForWorkOrder(Boolean enabledForWorkOrder) {
        this.enabledForWorkOrder = enabledForWorkOrder;
    }
}

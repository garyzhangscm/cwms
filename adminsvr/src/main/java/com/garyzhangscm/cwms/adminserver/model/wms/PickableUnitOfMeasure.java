package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;

public class PickableUnitOfMeasure implements Serializable {

    private Long id;

    private Long unitOfMeasureId;

    private Long warehouseId;

    private Warehouse warehouse;

    @JsonIgnore
    private AllocationConfiguration allocationConfiguration;

    public PickableUnitOfMeasure() {}

    public PickableUnitOfMeasure(Warehouse warehouse,
                                 Long unitOfMeasureId, AllocationConfiguration allocationConfiguration) {
        this.unitOfMeasureId = unitOfMeasureId;
        this.allocationConfiguration = allocationConfiguration;

        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public PickableUnitOfMeasure(Long warehouseId,
                                 Long unitOfMeasureId, AllocationConfiguration allocationConfiguration) {
        this.unitOfMeasureId = unitOfMeasureId;
        this.allocationConfiguration = allocationConfiguration;
        this.warehouseId = warehouseId;
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

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public AllocationConfiguration getAllocationConfiguration() {
        return allocationConfiguration;
    }

    public void setAllocationConfiguration(AllocationConfiguration allocationConfiguration) {
        this.allocationConfiguration = allocationConfiguration;
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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "pickable_unit_of_measure")
public class PickableUnitOfMeasure  extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pickable_unit_of_measure_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Transient
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "allocation_configuration_id")
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
        return new StringBuilder()
                .append("unitOfMeasureId: ").append(unitOfMeasureId).toString();
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

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}

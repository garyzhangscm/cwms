package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A master table that will create work order from this production plan.
 * Like P.O and Receipt.
 */
@Entity
@Table(name = "production_plan")
public class ProductionPlan extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_plan_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;


    @Column(name = "description")
    private String description;


    @OneToMany(
            mappedBy = "productionPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<ProductionPlanLine> productionPlanLines = new ArrayList<>();

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<ProductionPlanLine> getProductionPlanLines() {
        return productionPlanLines;
    }

    public void setProductionPlanLines(List<ProductionPlanLine> productionPlanLines) {
        this.productionPlanLines = productionPlanLines;
    }
}

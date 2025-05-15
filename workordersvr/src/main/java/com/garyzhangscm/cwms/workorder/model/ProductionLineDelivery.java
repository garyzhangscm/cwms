package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

@Entity
@Table(name = "production_line_delivery")
public class ProductionLineDelivery extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_delivery_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;


    @ManyToOne
    @JoinColumn(name = "work_order_line_id")
    private WorkOrderLine workOrderLine;


    @Column(name = "delivered_quantity")
    private Long deliveredQuantity = 0L;


    @Column(name = "consumed_quantity")
    private Long consumedQuantity = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public WorkOrderLine getWorkOrderLine() {
        return workOrderLine;
    }

    public void setWorkOrderLine(WorkOrderLine workOrderLine) {
        this.workOrderLine = workOrderLine;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }
}

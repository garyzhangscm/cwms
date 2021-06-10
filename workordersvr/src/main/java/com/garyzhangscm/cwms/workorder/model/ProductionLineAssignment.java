package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_line_assignment")
public class ProductionLineAssignment extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_assignment_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;


    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;



    @Column(name = "quantity")
    private Long quantity;


    @Column(name = "start_time")
    private LocalDateTime startTime;


    // time span that will be reserved by the work order
    // this is an estimated timespan calculated automatically
    // and can be override by the user
    // always in second
    @Column(name = "estimated_reserved_timespan")
    private Long estimatedReservedTimespan;

    public ProductionLineAssignment(){}

    public ProductionLineAssignment(WorkOrder workOrder,
                                    ProductionLine productionLine,
                                    Long quantity){
        this.workOrder = workOrder;
        this.productionLine = productionLine;
        this.quantity = quantity;
        this.startTime = LocalDateTime.now();

    }

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

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getEstimatedReservedTimespan() {
        return estimatedReservedTimespan;
    }

    public void setEstimatedReservedTimespan(Long estimatedReservedTimespan) {
        this.estimatedReservedTimespan = estimatedReservedTimespan;
    }
}

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_line_assignment_line")
public class ProductionLineAssignmentLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_assignment_line_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "work_order_line_id")
    private WorkOrderLine workOrderLine;

    @ManyToOne
    @JoinColumn(name = "production_line_assignment_id")
    @JsonIgnore
    private ProductionLineAssignment productionLineAssignment;


    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "open_quantity")
    private Long openQuantity;

    public ProductionLineAssignmentLine() {}
    public ProductionLineAssignmentLine(WorkOrderLine workOrderLine,
                                        ProductionLineAssignment productionLineAssignment,
                                        Long quantity,
                                        Long openQuantity) {
        this.workOrderLine = workOrderLine;
        this.productionLineAssignment = productionLineAssignment;
        this.quantity = quantity;
        this.openQuantity = openQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderLine getWorkOrderLine() {
        return workOrderLine;
    }

    public void setWorkOrderLine(WorkOrderLine workOrderLine) {
        this.workOrderLine = workOrderLine;
    }

    public ProductionLineAssignment getProductionLineAssignment() {
        return productionLineAssignment;
    }

    public void setProductionLineAssignment(ProductionLineAssignment productionLineAssignment) {
        this.productionLineAssignment = productionLineAssignment;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }
}

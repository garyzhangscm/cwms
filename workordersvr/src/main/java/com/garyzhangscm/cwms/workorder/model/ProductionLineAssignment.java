package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Transient
    private Long workOrderId;
    @Transient
    private String workOrderNumber;


    @OneToMany(
            mappedBy = "productionLineAssignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<ProductionLineAssignmentLine> lines = new ArrayList<>();


    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "open_quantity")
    private Long openQuantity;



    @Column(name = "start_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    //@DateTimeFormat(pattern =  "YYYY-MM-DDTHH:mm:ss.SSSZ")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // @JsonFormat(pattern="dd/MM/yyyy hh:mm")
    private LocalDateTime endTime;


    @ManyToOne
    @JoinColumn(name = "mould_id")
    private Mould mould;

    @Transient
    private Long deliveredPercentage;

    @Transient
    private Long consumedPercentage;




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
        this.openQuantity = quantity;
        this.startTime = LocalDateTime.now();
        this.estimatedReservedTimespan = 0L;

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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Mould getMould() {
        return mould;
    }

    public void setMould(Mould mould) {
        this.mould = mould;
    }

    public Long getWorkOrderId() {

        if (Objects.nonNull(workOrderId)) {
            return workOrderId;
        }
        else if (Objects.nonNull(workOrder)) {
            return workOrder.getId();
        }
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getDeliveredPercentage() {
        return deliveredPercentage;
    }

    public void setDeliveredPercentage(Long deliveredPercentage) {
        this.deliveredPercentage = deliveredPercentage;
    }

    public Long getConsumedPercentage() {
        return consumedPercentage;
    }

    public void setConsumedPercentage(Long consumedPercentage) {
        this.consumedPercentage = consumedPercentage;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public List<ProductionLineAssignmentLine> getLines() {
        return lines;
    }

    public void setLines(List<ProductionLineAssignmentLine> lines) {
        this.lines = lines;
    }

    public void addLine(ProductionLineAssignmentLine productionLineAssignmentLine) {
        this.lines.add(productionLineAssignmentLine);
    }

    public String getWorkOrderNumber() {
        if (Strings.isNotBlank(workOrderNumber)) {
            return workOrderNumber;
        }
        else if (Objects.nonNull(workOrder)) {
            return workOrder.getNumber();
        }
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }
}

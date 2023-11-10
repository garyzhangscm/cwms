package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.util.Pair;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "production_line")
public class ProductionLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "inbound_stage_location_id")
    private Long inboundStageLocationId;

    @Transient
    private Location inboundStageLocation;

    @Column(name = "outbound_stage_location_id")
    private Long outboundStageLocationId;

    @Transient
    private Location outboundStageLocation;

    @Column(name = "production_line_location_id")
    private Long productionLineLocationId;

    @Transient
    private Location productionLineLocation;


    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "production_line_type_id")
    private ProductionLineType type;

    @OneToMany(mappedBy = "productionLine")
    @JsonIgnore
    private List<ProductionLineAssignment> productionLineAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "productionLine", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ProductionLineCapacity> productionLineCapacities = new ArrayList<>();

    // Whether there's only one work order can be worked on
    // this production at any time
    @Column(name = "work_order_exclusive_flag")
    private Boolean workOrderExclusiveFlag = true;

    @Column(name = "enabled")
    private Boolean enabled = false;


    @Column(name = "model")
    private String model;


    // whether the production line can be used for
    // all items, or specific for certain items
    @Column(name = "generic_purpose")
    private Boolean genericPurpose = false;


    // default printer to print report for
    // this production line
    @Column(name = "report_printer_name")
    private String reportPrinterName;

    @Column(name = "label_printer_name")
    private String labelPrinterName;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public Long getInboundStageLocationId() {
        return inboundStageLocationId;
    }

    public void setInboundStageLocationId(Long inboundStageLocationId) {
        this.inboundStageLocationId = inboundStageLocationId;
    }

    public Location getInboundStageLocation() {
        return inboundStageLocation;
    }

    public void setInboundStageLocation(Location inboundStageLocation) {
        this.inboundStageLocation = inboundStageLocation;
    }

    public Long getOutboundStageLocationId() {
        return outboundStageLocationId;
    }

    public void setOutboundStageLocationId(Long outboundStageLocationId) {
        this.outboundStageLocationId = outboundStageLocationId;
    }

    public Location getOutboundStageLocation() {
        return outboundStageLocation;
    }

    public void setOutboundStageLocation(Location outboundStageLocation) {
        this.outboundStageLocation = outboundStageLocation;
    }

    public Long getProductionLineLocationId() {
        return productionLineLocationId;
    }

    public void setProductionLineLocationId(Long productionLineLocationId) {
        this.productionLineLocationId = productionLineLocationId;
    }

    public Location getProductionLineLocation() {
        return productionLineLocation;
    }

    public void setProductionLineLocation(Location productionLineLocation) {
        this.productionLineLocation = productionLineLocation;
    }

    public List<ProductionLineAssignment> getProductionLineAssignments() {

        return productionLineAssignments;
    }

    public void setProductionLineAssignments(List<ProductionLineAssignment> productionLineAssignments) {
        this.productionLineAssignments = productionLineAssignments;
    }

    public Boolean getWorkOrderExclusiveFlag() {
        return workOrderExclusiveFlag;
    }

    public void setWorkOrderExclusiveFlag(Boolean workOrderExclusiveFlag) {
        this.workOrderExclusiveFlag = workOrderExclusiveFlag;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<ProductionLineCapacity> getProductionLineCapacities() {
        return productionLineCapacities;
    }

    public void setProductionLineCapacities(List<ProductionLineCapacity> productionLineCapacities) {
        this.productionLineCapacities = productionLineCapacities;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReportPrinterName() {
        return reportPrinterName;
    }

    public void setReportPrinterName(String reportPrinterName) {
        this.reportPrinterName = reportPrinterName;
    }

    public String getLabelPrinterName() {
        return labelPrinterName;
    }

    public void setLabelPrinterName(String labelPrinterName) {
        this.labelPrinterName = labelPrinterName;
    }

    public Boolean getGenericPurpose() {
        return genericPurpose;
    }

    public void setGenericPurpose(Boolean genericPurpose) {
        this.genericPurpose = genericPurpose;
    }

    public ProductionLineType getType() {
        return type;
    }

    public void setType(ProductionLineType type) {
        this.type = type;
    }

    /**
     * return assigned work order's name and finish good name & description
     */
    public List<Triple<String, String, String>> getAssignedWorkOrders() {
        return getProductionLineAssignments().stream()
                .filter(productionLineAssignment -> !Boolean.TRUE.equals(productionLineAssignment.getDeassigned())
                        && Objects.isNull(productionLineAssignment.getDeassignedTime()))  // only return the work order that is not deassigned yet
                .map(productionLineAssignment ->
                        new Triple<String, String, String>(productionLineAssignment.getWorkOrderNumber(),
                                productionLineAssignment.getItemName(), productionLineAssignment.getItemDescription())
        ).collect(Collectors.toList());
    }
}

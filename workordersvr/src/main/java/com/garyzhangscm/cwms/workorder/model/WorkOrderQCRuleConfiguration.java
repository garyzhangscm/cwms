package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "work_order_qc_rule_configuration")
public class WorkOrderQCRuleConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_qc_rule_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name="production_line_id")
    ProductionLine productionLine;

    @ManyToOne
    @JoinColumn(name="work_order_id")
    WorkOrder workOrder;


    @OneToMany(
            mappedBy = "workOrderQCRuleConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderQCRuleConfigurationRule> workOrderQCRuleConfigurationRules = new ArrayList<>();

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

    public List<WorkOrderQCRuleConfigurationRule> getWorkOrderQCRuleConfigurationRules() {
        return workOrderQCRuleConfigurationRules;
    }

    public void setWorkOrderQCRuleConfigurationRules(List<WorkOrderQCRuleConfigurationRule> workOrderQCRuleConfigurationRules) {
        this.workOrderQCRuleConfigurationRules = workOrderQCRuleConfigurationRules;
    }
}

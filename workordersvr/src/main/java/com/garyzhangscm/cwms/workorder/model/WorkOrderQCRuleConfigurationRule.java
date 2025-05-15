package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

/**
 *
 */
@Entity
@Table(name = "work_order_qc_rule_configuration_rule")
public class WorkOrderQCRuleConfigurationRule extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_qc_rule_configuration_rule_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "qc_rule_id")
    private Long qcRuleId;

    @Transient
    private QCRule qcRule;




    @ManyToOne
    @JoinColumn(name="work_order_qc_rule_configuration_id")
    @JsonIgnore
    WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQcRuleId() {
        return qcRuleId;
    }

    public void setQcRuleId(Long qcRuleId) {
        this.qcRuleId = qcRuleId;
    }

    public QCRule getQcRule() {
        return qcRule;
    }

    public void setQcRule(QCRule qcRule) {
        this.qcRule = qcRule;
    }

    public WorkOrderQCRuleConfiguration getWorkOrderQCRuleConfiguration() {
        return workOrderQCRuleConfiguration;
    }

    public void setWorkOrderQCRuleConfiguration(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration) {
        this.workOrderQCRuleConfiguration = workOrderQCRuleConfiguration;
    }
}

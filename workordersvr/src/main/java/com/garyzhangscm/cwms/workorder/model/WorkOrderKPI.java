package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KPI at work order level. This is a simplified version
 * of KPI captured for the whole work order, in case we
 * don't have to capture all the details of how the work
 * order is done, but only want to know the result of
 * who makes how many of the finish product
 */
@Entity
@Table(name = "work_order_kpi")
public class WorkOrderKPI extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_kpi_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;


    @Column(name = "username")
    private String username;

    @Column(name = "working_team_name")
    private String workingTeamName;


    @Column(name = "working_team_member_count")
    private Integer workingTeamMemberCount = 0;

    @Column(name="kpi_measurement")
    @Enumerated(EnumType.STRING)
    private KPIMeasurement kpiMeasurement;

    /**
     * KPI amount
     */
    @Column(name = "amount")
    private double amount;


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

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public KPIMeasurement getKpiMeasurement() {
        return kpiMeasurement;
    }

    public void setKpiMeasurement(KPIMeasurement kpiMeasurement) {
        this.kpiMeasurement = kpiMeasurement;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getWorkingTeamName() {
        return workingTeamName;
    }

    public void setWorkingTeamName(String workingTeamName) {
        this.workingTeamName = workingTeamName;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public Integer getWorkingTeamMemberCount() {
        return workingTeamMemberCount;
    }

    public void setWorkingTeamMemberCount(Integer workingTeamMemberCount) {
        this.workingTeamMemberCount = workingTeamMemberCount;
    }
}

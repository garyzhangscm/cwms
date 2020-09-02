package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.Objects;


@Entity
@Table(name = "work_order_kpi_transaction")
public class WorkOrderKPITransaction extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_kpi_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;

    @Column(name = "username")
    private String username;

    @Column(name = "working_team_name")
    private String workingTeamName;

    @Column(name="kpi_measurement")
    @Enumerated(EnumType.STRING)
    private KPIMeasurement kpiMeasurement;

    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private WorkOrderKPITransactionType type = WorkOrderKPITransactionType.ADD;
    /**
     * KPI amount
     */
    @Column(name = "amount")
    private double amount;


    /**
     * A work order KPI transaction can be
     * 1. standalone
     * 2. Alone with the work order produce transaction
     * 3. Alone with the work order complete transaction
     */
    @ManyToOne
    @JoinColumn(name = "work_order_complete_transaction_id")
    @JsonIgnore
    private WorkOrderCompleteTransaction workOrderCompleteTransaction;

    @ManyToOne
    @JoinColumn(name = "work_order_produce_transaction_id")
    @JsonIgnore
    private WorkOrderProduceTransaction workOrderProduceTransaction;

    public WorkOrderKPI getWorkOrderKPI() {
        WorkOrderKPI workOrderKPI = new WorkOrderKPI();

        if (Objects.nonNull(workOrderCompleteTransaction)) {
            workOrderKPI.setWorkOrder(
                    workOrderCompleteTransaction.getWorkOrder()
            );
        }
        else if (Objects.nonNull(workOrderProduceTransaction)) {
            workOrderKPI.setWorkOrder(
                    workOrderProduceTransaction.getWorkOrder()
            );
        }

        workOrderKPI.setUsername(getUsername());
        workOrderKPI.setWorkingTeamName(getWorkingTeamName());
        workOrderKPI.setKpiMeasurement(getKpiMeasurement());
        workOrderKPI.setAmount(getAmount());
        return workOrderKPI;
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

    public WorkOrderCompleteTransaction getWorkOrderCompleteTransaction() {
        return workOrderCompleteTransaction;
    }

    public void setWorkOrderCompleteTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction) {
        this.workOrderCompleteTransaction = workOrderCompleteTransaction;
    }

    public WorkOrderProduceTransaction getWorkOrderProduceTransaction() {
        return workOrderProduceTransaction;
    }

    public void setWorkOrderProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {
        this.workOrderProduceTransaction = workOrderProduceTransaction;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public WorkOrderKPITransactionType getType() {
        return type;
    }

    public void setType(WorkOrderKPITransactionType type) {
        this.type = type;
    }
}

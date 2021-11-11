package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 *
 */
@Entity
@Table(name = "work_order_qc_result")
public class WorkOrderQCResult extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_qc_result_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name="work_order_qc_sample_id")
    WorkOrderQCSample workOrderQCSample;


    @Column(name = "qc_inspection_result")
    @Enumerated(EnumType.STRING)
    private QCInspectionResult qcInspectionResult;

    @Column(name = "qc_username")
    private String qcUsername;

    @Column(name = "qc_rf_code")
    private String qcRFCode;

    @Column(name = "qc_time")
    private LocalDateTime qcTime;


    // how many actual product we qc this time
    @Column(name = "qc_quantity")
    private Integer qcQuantity;

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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public WorkOrderQCSample getWorkOrderQCSample() {
        return workOrderQCSample;
    }

    public void setWorkOrderQCSample(WorkOrderQCSample workOrderQCSample) {
        this.workOrderQCSample = workOrderQCSample;
    }

    public QCInspectionResult getQcInspectionResult() {
        return qcInspectionResult;
    }

    public void setQcInspectionResult(QCInspectionResult qcInspectionResult) {
        this.qcInspectionResult = qcInspectionResult;
    }

    public String getQcUsername() {
        return qcUsername;
    }

    public void setQcUsername(String qcUsername) {
        this.qcUsername = qcUsername;
    }

    public LocalDateTime getQcTime() {
        return qcTime;
    }

    public void setQcTime(LocalDateTime qcTime) {
        this.qcTime = qcTime;
    }

    public String getQcRFCode() {
        return qcRFCode;
    }

    public void setQcRFCode(String qcRFCode) {
        this.qcRFCode = qcRFCode;
    }

    public Integer getQcQuantity() {
        return qcQuantity;
    }

    public void setQcQuantity(Integer qcQuantity) {
        this.qcQuantity = qcQuantity;
    }
}

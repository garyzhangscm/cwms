package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_configuration")
public class WorkOrderConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "warehouse_id")
    private Long warehouseId;


    // whether we consume the material per transaction
    // or once when the whole work order is closed
    @Column(name = "material_consume_timing")
    @Enumerated(EnumType.STRING)
    private WorkOrderMaterialConsumeTiming materialConsumeTiming;


    @Column(name = "over_consume_is_allowed")
    private Boolean overConsumeIsAllowed;
    @Column(name = "over_produce_is_allowed")
    private Boolean overProduceIsAllowed;

    @Column(name = "auto_record_item_productivity")
    private Boolean autoRecordItemProductivity;


    @Transient
    private List<ProductionShiftSchedule> productionShiftSchedules = new ArrayList<>();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }



    public WorkOrderMaterialConsumeTiming getMaterialConsumeTiming() {
        return materialConsumeTiming;
    }

    public void setMaterialConsumeTiming(WorkOrderMaterialConsumeTiming materialConsumeTiming) {
        this.materialConsumeTiming = materialConsumeTiming;
    }

    public Boolean getOverConsumeIsAllowed() {
        return overConsumeIsAllowed;
    }

    public void setOverConsumeIsAllowed(Boolean overConsumeIsAllowed) {
        this.overConsumeIsAllowed = overConsumeIsAllowed;
    }

    public Boolean getOverProduceIsAllowed() {
        return overProduceIsAllowed;
    }

    public void setOverProduceIsAllowed(Boolean overProduceIsAllowed) {
        this.overProduceIsAllowed = overProduceIsAllowed;
    }

    public List<ProductionShiftSchedule> getProductionShiftSchedules() {
        return productionShiftSchedules;
    }

    public void setProductionShiftSchedules(List<ProductionShiftSchedule> productionShiftSchedules) {
        this.productionShiftSchedules = productionShiftSchedules;
    }

    public Boolean getAutoRecordItemProductivity() {
        return autoRecordItemProductivity;
    }

    public void setAutoRecordItemProductivity(Boolean autoRecordItemProductivity) {
        this.autoRecordItemProductivity = autoRecordItemProductivity;
    }
}

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "work_order_qc_sample")
public class WorkOrderQCSample extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_qc_sample_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name="production_line_assignment_id")
    ProductionLineAssignment productionLineAssignment;

    @Column(name = "image_urls")
    private String imageUrls;

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

    public ProductionLineAssignment getProductionLineAssignment() {
        return productionLineAssignment;
    }

    public void setProductionLineAssignment(ProductionLineAssignment productionLineAssignment) {
        this.productionLineAssignment = productionLineAssignment;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }
}
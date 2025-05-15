package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "trailer")
public class Trailer extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trailer_id")
    @JsonProperty(value="id")
    private Long id;

    // if the container belongs to the company
    @Column(name = "company_id")
    private Long companyId;

    // if the container belongs to the warehouse
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @OneToOne
    @JoinColumn(name="trailer_appointment_id")
    private TrailerAppointment currentAppointment;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "size")
    private Double size;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public TrailerAppointment getCurrentAppointment() {
        return currentAppointment;
    }

    public void setCurrentAppointment(TrailerAppointment currentAppointment) {
        this.currentAppointment = currentAppointment;
    }
}

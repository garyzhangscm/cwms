package com.garyzhangscm.cwms.common.model;

import org.codehaus.jackson.annotate.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "trailer_appointment")
public class TrailerAppointment extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trailer_appointment_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "number")
    private String number;
    @Column(name = "description")
    private String description;


    @OneToOne
    @JoinColumn(name="trailer_id")
    @JsonIgnore
    private Trailer trailer;


    @ManyToOne
    @JoinColumn(name="tractor_id")
    private Tractor tractor;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TrailerAppointmentType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TrailerAppointmentStatus status;

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

    public TrailerAppointmentType getType() {
        return type;
    }

    public void setType(TrailerAppointmentType type) {
        this.type = type;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public TrailerAppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(TrailerAppointmentStatus status) {
        this.status = status;
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

    public Trailer getTrailer() {
        return trailer;
    }

    public void setTrailer(Trailer trailer) {
        this.trailer = trailer;
    }

    public String getCurrentTrailerNumber() {
        return Objects.isNull(trailer) ? "" : trailer.getNumber();
    }

    public Tractor getTractor() {
        return tractor;
    }

    public void setTractor(Tractor tractor) {
        this.tractor = tractor;
    }
}

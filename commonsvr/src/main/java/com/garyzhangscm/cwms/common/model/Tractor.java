package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tractor")
public class Tractor  extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tractor_id")
    @JsonProperty(value="id")
    private Long id;


    // if the trailer is the company's asset
    @Column(name = "company_id")
    private Long companyId;

    // if the trailer is the warehouse's asset
    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "license_plate_number")
    private String licensePlateNumber;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @OneToOne
    @JoinColumn(name="tractor_appointment_id")
    private TractorAppointment currentAppointment;

    // Where the trailer is parking. Normally it is
    // either dock or yard
    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TractorStatus status;


    // only assigned the permanently attached container
    @ManyToMany(cascade = {
            CascadeType.ALL
    })
    @JoinTable(name = "tractor_attached_trailer",
            joinColumns = @JoinColumn(name = "tractor_id"),
            inverseJoinColumns = @JoinColumn(name = "trailer_id")
    )
    private List<Trailer> attachedTrailers = new ArrayList<>();

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

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public void setLicensePlateNumber(String licensePlateNumber) {
        this.licensePlateNumber = licensePlateNumber;
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

    public TractorAppointment getCurrentAppointment() {
        return currentAppointment;
    }

    public void setCurrentAppointment(TractorAppointment currentAppointment) {
        this.currentAppointment = currentAppointment;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public TractorStatus getStatus() {
        return status;
    }

    public void setStatus(TractorStatus status) {
        this.status = status;
    }

    public List<Trailer> getAttachedTrailers() {
        return attachedTrailers;
    }

    public void setAttachedTrailers(List<Trailer> attachedTrailers) {
        this.attachedTrailers = attachedTrailers;
    }
}

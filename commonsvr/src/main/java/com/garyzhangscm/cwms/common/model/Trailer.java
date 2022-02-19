package com.garyzhangscm.cwms.common.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trailer")
public class Trailer  extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trailer_id")
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
    @JoinColumn(name="trailer_appointment_id")
    private TrailerAppointment currentAppointment;

    // Where the trailer is parking. Normally it is
    // either dock or yard
    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "status")
    private TrailerStatus status;


    // only assigned the permanently attached container
    @ManyToMany(cascade = {
            CascadeType.ALL
    })
    @JoinTable(name = "trailer_attached_container",
            joinColumns = @JoinColumn(name = "trailer_id"),
            inverseJoinColumns = @JoinColumn(name = "trailer_container_id")
    )
    private List<TrailerContainer> attachedContainers = new ArrayList<>();

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

    public TrailerAppointment getCurrentAppointment() {
        return currentAppointment;
    }

    public void setCurrentAppointment(TrailerAppointment currentAppointment) {
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

    public TrailerStatus getStatus() {
        return status;
    }

    public void setStatus(TrailerStatus status) {
        this.status = status;
    }

    public List<TrailerContainer> getAttachedContainers() {
        return attachedContainers;
    }

    public void setAttachedContainers(List<TrailerContainer> attachedContainers) {
        this.attachedContainers = attachedContainers;
    }
}

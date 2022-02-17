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

    // the company that the trailer belongs to
    @Column(name = "company_id")
    private Long companyId;

    // the warehouse that the trailer is current in
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "driver_first_name")
    private String driverFirstName;

    @Column(name = "driver_last_name")
    private String driverLastName;

    @Column(name = "driver_phone")
    private String driverPhone;


    @Column(name = "license_plate_number")
    private String licensePlateNumber;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "size")
    private String size;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TrailerType type;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private TrailerCategory category;


    @ManyToOne
    @JoinColumn(name="carrier_id")
    private Carrier carrier;

    @ManyToOne
    @JoinColumn(name="carrier_service_level_id")
    private CarrierServiceLevel carrierServiceLevel;


    @OneToMany(
            mappedBy = "trailer",
            orphanRemoval = true,
            cascade = CascadeType.ALL
    )
    private List<TrailerContainer> containers = new ArrayList<>();

    // Where the trailer is parking. Normally it is
    // either dock or yard
    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "status")
    private TrailerStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getDriverFirstName() {
        return driverFirstName;
    }

    public void setDriverFirstName(String driverFirstName) {
        this.driverFirstName = driverFirstName;
    }

    public String getDriverLastName() {
        return driverLastName;
    }

    public void setDriverLastName(String driverLastName) {
        this.driverLastName = driverLastName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public void setLicensePlateNumber(String licensePlateNumber) {
        this.licensePlateNumber = licensePlateNumber;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public List<TrailerContainer> getContainers() {
        return containers;
    }

    public void setContainers(List<TrailerContainer> containers) {
        this.containers = containers;
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public TrailerType getType() {
        return type;
    }

    public void setType(TrailerType type) {
        this.type = type;
    }


    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }


    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
    }

    public TrailerStatus getStatus() {
        return status;
    }

    public void setStatus(TrailerStatus status) {
        this.status = status;
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

    public TrailerCategory getCategory() {
        return category;
    }

    public void setCategory(TrailerCategory category) {
        this.category = category;
    }
}

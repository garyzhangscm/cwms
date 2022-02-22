package com.garyzhangscm.cwms.common.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tractor_appointment")
public class TractorAppointment extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tractor_appointment_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;



    @Column(name = "driver_first_name")
    private String driverFirstName;

    @Column(name = "driver_last_name")
    private String driverLastName;

    @Column(name = "driver_phone")
    private String driverPhone;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TractorAppointmentType type;

    // container used for current appoinment
    @ManyToMany(cascade = {
            CascadeType.ALL
    })
    @JoinTable(name = "tractor_appointment_trailer",
            joinColumns = @JoinColumn(name = "tractor_appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "trailer_id")
    )
    private List<Trailer> trailers = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name="carrier_id")
    private Carrier carrier;

    @ManyToOne
    @JoinColumn(name="carrier_service_level_id")
    private CarrierServiceLevel carrierServiceLevel;

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

    public TractorAppointmentType getType() {
        return type;
    }

    public void setType(TractorAppointmentType type) {
        this.type = type;
    }

    public List<Trailer> getTrailers() {
        return trailers;
    }

    public void setTrailers(List<Trailer> trailers) {
        this.trailers = trailers;
    }
}

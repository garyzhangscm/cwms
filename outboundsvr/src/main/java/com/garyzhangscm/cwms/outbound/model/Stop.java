package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "stop")
public class Stop  extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToMany(
            mappedBy = "stop",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    private List<Shipment> shipments = new ArrayList<>();

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    @Transient
    private Warehouse warehouse;


    // if we respect the sequence of the stop
    // in the same trailer appointment
    @Column(name = "stop_sequence")
    private Long sequence;

    @Column(name = "trailer_appointment_id")
    private Long trailerAppointmentId;

    @Transient
    private TrailerAppointment trailerAppointment;

    @Column(name = "contactor_firstname")
    private String contactorFirstname;
    @Column(name = "contactor_lastname")
    private String contactorLastname;

    @Column(name = "address_country")
    private String addressCountry;
    @Column(name = "address_state")
    private String addressState;
    @Column(name = "address_county")
    private String addressCounty;
    @Column(name = "address_city")
    private String addressCity;
    @Column(name = "address_district")
    private String addressDistrict;
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    @Column(name = "address_postcode")
    private String addressPostcode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
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

    public Long getTrailerAppointmentId() {
        return trailerAppointmentId;
    }

    public void setTrailerAppointmentId(Long trailerAppointmentId) {
        this.trailerAppointmentId = trailerAppointmentId;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public TrailerAppointment getTrailerAppointment() {
        return trailerAppointment;
    }

    public void setTrailerAppointment(TrailerAppointment trailerAppointment) {
        this.trailerAppointment = trailerAppointment;
    }

    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public void setContactorFirstname(String contactorFirstname) {
        this.contactorFirstname = contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public void setContactorLastname(String contactorLastname) {
        this.contactorLastname = contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public Trailer getTrailer() {
        if (Objects.nonNull(getTrailerAppointment())) {
            return getTrailerAppointment().getTrailer();
        }
        return null;
    }

    public void setTrailer(Trailer trailer) {

        if (Objects.nonNull(getTrailerAppointment())) {
            getTrailerAppointment().setTrailer(trailer);
        }
    }
}

package com.garyzhangscm.cwms.integration.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stop   implements Serializable {


    private Long warehouseId;

    private String number;


    private Long shipToCustomerId;

    private Long sequence;
    private Long trailerAppointmentId;
    private TrailerAppointment trailerAppointment;


    private String contactorFirstname;
    private String contactorLastname;

    private String addressCountry;
    private String addressState;
    private String addressCounty;
    private String addressCity;
    private String addressDistrict;
    private String addressLine1;
    private String addressLine2;
    private String addressPostcode;

    private List<Shipment> shipments = new ArrayList<>();

    private List<TrailerOrderLineAssignment> trailerOrderLineAssignments = new ArrayList<>();

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
    }

    public void addShipment(Shipment shipment) {
        this.shipments.add(shipment);
    }
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<TrailerOrderLineAssignment> getTrailerOrderLineAssignments() {
        return trailerOrderLineAssignments;
    }

    public void setTrailerOrderLineAssignments(List<TrailerOrderLineAssignment> trailerOrderLineAssignments) {
        this.trailerOrderLineAssignments = trailerOrderLineAssignments;
    }

    public void addTrailerOrderLineAssignment(TrailerOrderLineAssignment trailerOrderLineAssignment) {
        this.trailerOrderLineAssignments.add(trailerOrderLineAssignment);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Long getTrailerAppointmentId() {
        return trailerAppointmentId;
    }

    public void setTrailerAppointmentId(Long trailerAppointmentId) {
        this.trailerAppointmentId = trailerAppointmentId;
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

    public TrailerAppointment getTrailerAppointment() {
        return trailerAppointment;
    }

    public void setTrailerAppointment(TrailerAppointment trailerAppointment) {
        this.trailerAppointment = trailerAppointment;
    }

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }
}

package com.garyzhangscm.cwms.outbound.model;

import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "stop")
public class Stop  extends AuditibleEntity<String> {
    private static final Logger logger = LoggerFactory.getLogger(Stop.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToMany(
            mappedBy = "stop",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Shipment> shipments = new ArrayList<>();

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private StopStatus status;

    @Transient
    private Warehouse warehouse;


    // if we respect the sequence of the stop
    // in the same trailer appointment
    @Column(name = "stop_sequence")
    private Integer sequence;

    @Column(name = "trailer_appointment_id")
    private Long trailerAppointmentId;

    @Transient
    private TrailerAppointment trailerAppointment;

    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Transient
    private Customer shipToCustomer;

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

    @Transient
    @OneToMany(
            mappedBy = "stop",
            fetch = FetchType.EAGER
    )
    private List<TrailerOrderLineAssignment> trailerOrderLineAssignments = new ArrayList<>();

    public Stop() {}
    public Stop(Long warehouseId, String number,
                Integer sequence,
                Long trailerAppointmentId,
                Long shipToCustomerId,
                String contactorFirstname,
                String contactorLastname,
                String addressCountry,
                String addressState,
                String addressCounty,
                String addressCity,
                String addressDistrict,
                String addressLine1,
                String addressLine2,
                String addressPostcode) {
        this.warehouseId = warehouseId;
        this.number = number;
        this.sequence = sequence;
        this.trailerAppointmentId = trailerAppointmentId;

        this.shipToCustomerId = shipToCustomerId;
        this.contactorFirstname = contactorFirstname;
        this.contactorLastname = contactorLastname;
        this.addressCountry = addressCountry;
        this.addressState = addressState;
        this.addressCounty = addressCounty;
        this.addressCity = addressCity;
        this.addressDistrict = addressDistrict;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressPostcode = addressPostcode;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void addShipment(Shipment shipment) {
        if (!validateNewShipmentsForStop(shipment)) {
            throw ShippingException.raiseException("can't add the new shipment " + shipment.getNumber() + " into the same stop" +
                    " as they are shipping to different address");
        }
        this.shipments.add(shipment);
        if (shipments.size() == 1) {

            // we will need to setup the address information based on the shipment
            // only when we add the first shipment
            setupAddressInformation(shipments);
        }

    }
    public void setShipments(List<Shipment> shipments) {
        logger.debug("start to setup shipment for the stop {}",
                Strings.isBlank(getNumber()) ? "N/A" : getNumber());
        if (!validateShipmentsForStop(shipments)) {
            throw ShippingException.raiseException("can't group the shipments into the same stop" +
                    " as they are shipping to different address");
        }
        logger.debug(">> passed the validation");
        this.shipments = shipments;
        logger.debug(">> will need to setup the address information");
        setupAddressInformation(shipments);
        logger.debug(">> address information is done");

    }

    public StopStatus getStatus() {
        return status;
    }

    public void setStatus(StopStatus status) {
        this.status = status;
    }

    private void setupAddressInformation(List<Shipment> shipments) {
        if (shipments.size() == 0) {
            return;
        }
        // we know for sure that all the shipments should be shipped to
        // the same address so we will only need to get the address from
        // the first shipment
        Shipment shipment = shipments.get(0);
        setContactorFirstname(shipment.getShipToContactorFirstname());
        setContactorLastname(shipment.getShipToContactorLastname());
        setAddressCountry(shipment.getShipToAddressCountry());
        setAddressState(shipment.getShipToAddressState());
        setAddressCounty(shipment.getShipToAddressCounty());
        setAddressCity(shipment.getShipToAddressCity());
        setAddressDistrict(shipment.getShipToAddressDistrict());
        setAddressLine1(shipment.getShipToAddressLine1());
        setAddressLine2(shipment.getShipToAddressLine2());
        setAddressPostcode(shipment.getShipToAddressPostcode());

    }

    /**
     * Make sure the new shipment is shipping to the same address
     * @param shipment
     * @return
     */
    public boolean validateNewShipmentsForStop(Shipment shipment) {
        if (shipments.size() == 0) {
            // there's no shipment in the stop yet,
            logger.debug("There's no shipment in this stop {} yet, we can accept the new shipment {}",
                    getNumber(),
                    shipment.getNumber());
            return true;
        }
        // we know for sure that all the existing shipments in the same
        // stop will be shipped to the same address so we only need to
        // compare the first shipment in the stop with the new shipment
        return shipmentAddressInformationEquals(shipments.get(0), shipment);
    }

    /**
     * Validate whether the shipment's address are exactly the same.
     * In order to group the shipments into one stop, we needs to make sure
     * the shipments are going to the same address
     * @param shipments
     */
    private boolean validateShipmentsForStop(List<Shipment> shipments) {
        if (shipments.size() <= 1) {
            return true;
        }

        Shipment firstShipment = shipments.get(0);
        for(Shipment shipment : shipments) {
            if (!shipmentAddressInformationEquals(firstShipment, shipment)) {
                return  false;
            }

        }
        return true;


    }

    // check if the order can be group into the stop
    // by compare the order's address to the stop
    // if there's no shipment yet, then the order is valid for the stop
    public boolean orderValidForStop(Order order) {
        if (getShipments().isEmpty()) {
            return true;
        }
        return shipmentAddressInformationEquals("contactorFirstname",
                order.getNumber(), order.getShipToContactorFirstname(),
                getNumber(), getContactorFirstname()) &&
                shipmentAddressInformationEquals("contactorLastname",
                        order.getNumber(), order.getShipToContactorLastname(),
                        getNumber(), getContactorLastname()) &&
                shipmentAddressInformationEquals("addressCountry",
                        order.getNumber(), order.getShipToAddressCountry(),
                        getNumber(), getAddressCountry()) &&
                shipmentAddressInformationEquals("addressState",
                        order.getNumber(), order.getShipToAddressState(),
                        getNumber(), getAddressState()) &&
                shipmentAddressInformationEquals("addressCounty",
                        order.getNumber(), order.getShipToAddressCounty(),
                        getNumber(), getAddressCounty()) &&
                shipmentAddressInformationEquals("addressCity",
                        order.getNumber(), order.getShipToAddressCity(),
                        getNumber(), getAddressCity()) &&
                shipmentAddressInformationEquals("addressDistrict",
                        order.getNumber(), order.getShipToAddressDistrict(),
                        getNumber(), getAddressDistrict()) &&
                shipmentAddressInformationEquals("addressLine1",
                        order.getNumber(), order.getShipToAddressLine1(),
                        getNumber(), getAddressLine1()) &&
                shipmentAddressInformationEquals("addressLine2",
                        order.getNumber(), order.getShipToAddressLine2(),
                        getNumber(), getAddressLine2()) &&
                shipmentAddressInformationEquals("addressPostcode",
                        order.getNumber(), order.getShipToAddressPostcode(),
                        getNumber(), getAddressPostcode());

    }
    private boolean shipmentAddressInformationEquals(
            Shipment shipment1, Shipment shipment2
    ) {


        return shipmentAddressInformationEquals("contactorFirstname",
                    shipment1.getNumber(), shipment1.getShipToContactorFirstname(),
                    shipment2.getNumber(), shipment2.getShipToContactorFirstname()) &&
                shipmentAddressInformationEquals("contactorLastname",
                        shipment1.getNumber(), shipment1.getShipToContactorLastname(),
                        shipment2.getNumber(), shipment2.getShipToContactorLastname()) &&
                shipmentAddressInformationEquals("addressCountry",
                        shipment1.getNumber(), shipment1.getShipToAddressCountry(),
                        shipment2.getNumber(), shipment2.getShipToAddressCountry()) &&
                shipmentAddressInformationEquals("addressState",
                        shipment1.getNumber(), shipment1.getShipToAddressState(),
                        shipment2.getNumber(), shipment2.getShipToAddressState()) &&
                shipmentAddressInformationEquals("addressCounty",
                        shipment1.getNumber(), shipment1.getShipToAddressCounty(),
                        shipment2.getNumber(), shipment2.getShipToAddressCounty()) &&
                shipmentAddressInformationEquals("addressCity",
                        shipment1.getNumber(), shipment1.getShipToAddressCity(),
                        shipment2.getNumber(), shipment2.getShipToAddressCity()) &&
                shipmentAddressInformationEquals("addressDistrict",
                        shipment1.getNumber(), shipment1.getShipToAddressDistrict(),
                        shipment2.getNumber(), shipment2.getShipToAddressDistrict()) &&
                shipmentAddressInformationEquals("addressLine1",
                        shipment1.getNumber(), shipment1.getShipToAddressLine1(),
                        shipment2.getNumber(), shipment2.getShipToAddressLine1()) &&
                shipmentAddressInformationEquals("addressLine2",
                        shipment1.getNumber(), shipment1.getShipToAddressLine2(),
                        shipment2.getNumber(), shipment2.getShipToAddressLine2()) &&
                shipmentAddressInformationEquals("addressPostcode",
                        shipment1.getNumber(), shipment1.getShipToAddressPostcode(),
                        shipment2.getNumber(), shipment2.getShipToAddressPostcode());


    }
    private boolean shipmentAddressInformationEquals(
            String fieldName,
            String shipmentNumber1, String value1,
            String shipmentNumber2, String value2
    ) {

        // escape the null value
        value1 = Strings.isBlank(value1) ? "" : value1;
        value2 = Strings.isBlank(value2) ? "" : value2;
        logger.debug("Compare field {} between shipment {} and {}, {} vs {}",
                fieldName, shipmentNumber1, shipmentNumber2,
                value1, value2);
        if (!value1.equals(value2)) {

            logger.debug("shipment {}'s {}: {} is different from " +
                            " shipment {}'s {}: {}, we can't group them into same stop",
                    shipmentNumber1,
                    fieldName,
                    value1,
                    shipmentNumber2,
                    fieldName,
                    value2);
            return false;
        }
        logger.debug("The 2 shipments has the same value in this field {}",
                fieldName);
        return true;
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
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

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public Customer getShipToCustomer() {
        return shipToCustomer;
    }

    public void setShipToCustomer(Customer shipToCustomer) {
        this.shipToCustomer = shipToCustomer;
    }

    public List<TrailerOrderLineAssignment> getTrailerOrderLineAssignments() {
        return trailerOrderLineAssignments;
    }

    public void setTrailerOrderLineAssignments(List<TrailerOrderLineAssignment> trailerOrderLineAssignments) {
        this.trailerOrderLineAssignments = trailerOrderLineAssignments;
    }
}

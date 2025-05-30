/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.garyzhangscm.cwms.outbound.model.hualei.ShipmentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "outbound_order")
public class Order  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_order_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;


    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Column(name = "quickbook_customer_list_id")
    private String quickbookCustomerListId;

    @Transient
    private Customer shipToCustomer;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private OrderCategory category = OrderCategory.SALES_ORDER;

    @Column(name = "transfer_receipt_number")
    private String transferReceiptNumber;
    @Column(name = "transfer_receipt_warehouse_id")
    private Long transferReceiptWarehouseId;


    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderBillableActivity> orderBillableActivities = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.OPEN;

    @Transient
    private Warehouse warehouse;

    // whether the order allows for manual pick
    @Column(name = "allow_for_manual_pick")
    private Boolean allowForManualPick;

    @Column(name = "bill_to_customer_id")
    private Long billToCustomerId;

    @Transient
    private Customer billToCustomer;

    // Ship to Address
    @Column(name = "ship_to_contactor_firstname")
    private String shipToContactorFirstname;
    @Column(name = "ship_to_contactor_lastname")
    private String shipToContactorLastname;
    @Column(name = "ship_to_contactor_phone_number")
    private String shipToContactorPhoneNumber;

    @Column(name = "ship_to_address_country")
    private String shipToAddressCountry;
    @Column(name = "ship_to_address_state")
    private String shipToAddressState;
    @Column(name = "ship_to_address_county")
    private String shipToAddressCounty;
    @Column(name = "ship_to_address_city")
    private String shipToAddressCity;
    @Column(name = "ship_to_address_district")
    private String shipToAddressDistrict;
    @Column(name = "ship_to_address_line1")
    private String shipToAddressLine1;
    @Column(name = "ship_to_address_line2")
    private String shipToAddressLine2;
    @Column(name = "ship_to_address_line3")
    private String shipToAddressLine3;
    @Column(name = "ship_to_address_postcode")
    private String shipToAddressPostcode;


    // Bill to Address
    @Column(name = "bill_to_contactor_firstname")
    private String billToContactorFirstname;
    @Column(name = "bill_to_contactor_lastname")
    private String billToContactorLastname;

    @Column(name = "bill_to_address_country")
    private String billToAddressCountry;
    @Column(name = "bill_to_address_state")
    private String billToAddressState;
    @Column(name = "bill_to_address_county")
    private String billToAddressCounty;
    @Column(name = "bill_to_address_city")
    private String billToAddressCity;
    @Column(name = "bill_to_address_district")
    private String billToAddressDistrict;
    @Column(name = "bill_to_address_line1")
    private String billToAddressLine1;
    @Column(name = "bill_to_address_line2")
    private String billToAddressLine2;
    @Column(name = "bill_to_address_line3")
    private String billToAddressLine3;
    @Column(name = "bill_to_address_postcode")
    private String billToAddressPostcode;


    @Column(name = "complete_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime completeTime;


    // used by out sourcing orders
    // when the order is completed by a 3rd party
    // the supplier will be the party that
    // fulfill the order
    @Column(name = "supplier_id")
    private Long supplierId;

    @Transient
    private Supplier supplier;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Transient
    private CarrierServiceLevel carrierServiceLevel;


    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<OrderLine> orderLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ParcelPackage> parcelPackages = new ArrayList<>();

    // hualei related field
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdTime desc")
    private List<ShipmentRequest> hualeiShipmentRequests = new ArrayList<>();


    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderDocument> orderDocuments = new ArrayList<>();


    // staging location group
    @Column(name="stage_location_group_id")
    private Long stageLocationGroupId;

    @Transient
    private LocationGroup stageLocationGroup;

    // staging location group
    @Column(name="stage_location_id")
    private Long stageLocationId;

    @Transient
    private Location stageLocation;


    @Column(name = "quickbook_txnid")
    private String quickbookTxnID;


    // if there's a cancel requested
    // and record the request by user and timing
    @Column(name = "cancel_requested")
    private Boolean cancelRequested;

    @Column(name = "cancel_requested_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime cancelRequestedTime;

    @Column(name = "cancel_requested_username")
    private String cancelRequestedUsername;

    @Column(name = "po_number")
    private String poNumber;


    // Some statistics numbers that we can show
    // in the frontend
    @Transient
    private Integer totalLineCount;
    @Transient
    private Integer totalItemCount;
    @Transient
    private Long totalExpectedQuantity;
    @Transient
    private Long totalOpenQuantity; // Open quantity that is not in shipment yet
    @Transient
    private Long totalInprocessQuantity; // Total quantity that is in shipment
    // totalInprocessQuantity = totalPendingAllocationQuantity + totalOpenPickQuantity + totalPickedQuantity
    @Transient
    private Long totalPendingAllocationQuantity;
    @Transient
    private Long totalOpenPickQuantity;
    @Transient
    private Long totalPickedQuantity;
    @Transient
    private Long totalShippedQuantity;

    @Override
    public boolean equals(Object anotherObj){
        if (this == anotherObj) {
            return true;
        }
        if (!(anotherObj instanceof Order)) {
            return false;
        }
        Order anotherOrder = (Order)anotherObj;
        return getWarehouseId().equals(anotherOrder.getWarehouseId()) &&
                Objects.equals(client, anotherOrder.getClient()) &&
                getNumber().equals(anotherOrder.getNumber());
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, warehouseId);
    }

    public ZonedDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(ZonedDateTime completeTime) {
        this.completeTime = completeTime;
    }

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

    public Long getBillToCustomerId() {
        return billToCustomerId;
    }

    public void setBillToCustomerId(Long billToCustomerId) {
        this.billToCustomerId = billToCustomerId;
    }

    public Customer getBillToCustomer() {
        return billToCustomer;
    }

    public void setBillToCustomer(Customer billToCustomer) {
        this.billToCustomer = billToCustomer;
    }

    public String getShipToContactorFirstname() {
        return shipToContactorFirstname;
    }

    public void setShipToContactorFirstname(String shipToContactorFirstname) {
        this.shipToContactorFirstname = shipToContactorFirstname;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getShipToContactorLastname() {
        return shipToContactorLastname;
    }

    public void setShipToContactorLastname(String shipToContactorLastname) {
        this.shipToContactorLastname = shipToContactorLastname;
    }

    public String getShipToAddressCountry() {
        return shipToAddressCountry;
    }

    public void setShipToAddressCountry(String shipToAddressCountry) {
        this.shipToAddressCountry = shipToAddressCountry;
    }

    public String getShipToAddressLine3() {
        return shipToAddressLine3;
    }

    public void setShipToAddressLine3(String shipToAddressLine3) {
        this.shipToAddressLine3 = shipToAddressLine3;
    }

    public String getBillToAddressLine3() {
        return billToAddressLine3;
    }

    public void setBillToAddressLine3(String billToAddressLine3) {
        this.billToAddressLine3 = billToAddressLine3;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getShipToAddressState() {
        return shipToAddressState;
    }

    public void setShipToAddressState(String shipToAddressState) {
        this.shipToAddressState = shipToAddressState;
    }

    public String getShipToAddressCounty() {
        return shipToAddressCounty;
    }

    public void setShipToAddressCounty(String shipToAddressCounty) {
        this.shipToAddressCounty = shipToAddressCounty;
    }

    public String getShipToAddressCity() {
        return shipToAddressCity;
    }

    public void setShipToAddressCity(String shipToAddressCity) {
        this.shipToAddressCity = shipToAddressCity;
    }

    public String getShipToAddressDistrict() {
        return shipToAddressDistrict;
    }

    public void setShipToAddressDistrict(String shipToAddressDistrict) {
        this.shipToAddressDistrict = shipToAddressDistrict;
    }

    public String getShipToAddressLine1() {
        return shipToAddressLine1;
    }

    public void setShipToAddressLine1(String shipToAddressLine1) {
        this.shipToAddressLine1 = shipToAddressLine1;
    }

    public String getShipToAddressLine2() {
        return shipToAddressLine2;
    }

    public void setShipToAddressLine2(String shipToAddressLine2) {
        this.shipToAddressLine2 = shipToAddressLine2;
    }

    public String getShipToAddressPostcode() {
        return shipToAddressPostcode;
    }

    public void setShipToAddressPostcode(String shipToAddressPostcode) {
        this.shipToAddressPostcode = shipToAddressPostcode;
    }

    public String getBillToContactorFirstname() {
        return billToContactorFirstname;
    }

    public void setBillToContactorFirstname(String billToContactorFirstname) {
        this.billToContactorFirstname = billToContactorFirstname;
    }

    public String getBillToContactorLastname() {
        return billToContactorLastname;
    }

    public void setBillToContactorLastname(String billToContactorLastname) {
        this.billToContactorLastname = billToContactorLastname;
    }

    public String getBillToAddressCountry() {
        return billToAddressCountry;
    }

    public void setBillToAddressCountry(String billToAddressCountry) {
        this.billToAddressCountry = billToAddressCountry;
    }

    public String getBillToAddressState() {
        return billToAddressState;
    }

    public void setBillToAddressState(String billToAddressState) {
        this.billToAddressState = billToAddressState;
    }

    public String getBillToAddressCounty() {
        return billToAddressCounty;
    }

    public void setBillToAddressCounty(String billToAddressCounty) {
        this.billToAddressCounty = billToAddressCounty;
    }

    public String getBillToAddressCity() {
        return billToAddressCity;
    }

    public void setBillToAddressCity(String billToAddressCity) {
        this.billToAddressCity = billToAddressCity;
    }

    public String getBillToAddressDistrict() {
        return billToAddressDistrict;
    }

    public void setBillToAddressDistrict(String billToAddressDistrict) {
        this.billToAddressDistrict = billToAddressDistrict;
    }

    public String getBillToAddressLine1() {
        return billToAddressLine1;
    }

    public void setBillToAddressLine1(String billToAddressLine1) {
        this.billToAddressLine1 = billToAddressLine1;
    }

    public String getBillToAddressLine2() {
        return billToAddressLine2;
    }

    public void setBillToAddressLine2(String billToAddressLine2) {
        this.billToAddressLine2 = billToAddressLine2;
    }

    public String getBillToAddressPostcode() {
        return billToAddressPostcode;
    }

    public void setBillToAddressPostcode(String billToAddressPostcode) {
        this.billToAddressPostcode = billToAddressPostcode;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
    }

    public Long getStageLocationGroupId() {
        return stageLocationGroupId;
    }

    public void setStageLocationGroupId(Long stageLocationGroupId) {
        this.stageLocationGroupId = stageLocationGroupId;
    }

    public LocationGroup getStageLocationGroup() {
        return stageLocationGroup;
    }

    public void setStageLocationGroup(LocationGroup stageLocationGroup) {
        this.stageLocationGroup = stageLocationGroup;
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

    public Integer getTotalLineCount() {
        return totalLineCount;
    }

    public void setTotalLineCount(Integer totalLineCount) {
        this.totalLineCount = totalLineCount;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public Long getTotalExpectedQuantity() {
        return totalExpectedQuantity;
    }

    public void setTotalExpectedQuantity(Long totalExpectedQuantity) {
        this.totalExpectedQuantity = totalExpectedQuantity;
    }

    public Long getTotalOpenQuantity() {
        return totalOpenQuantity;
    }

    public void setTotalOpenQuantity(Long totalOpenQuantity) {
        this.totalOpenQuantity = totalOpenQuantity;
    }

    public Long getTotalInprocessQuantity() {
        return totalInprocessQuantity;
    }

    public void setTotalInprocessQuantity(Long totalInprocessQuantity) {
        this.totalInprocessQuantity = totalInprocessQuantity;
    }

    public Long getTotalPendingAllocationQuantity() {
        return totalPendingAllocationQuantity;
    }

    public void setTotalPendingAllocationQuantity(Long totalPendingAllocationQuantity) {
        this.totalPendingAllocationQuantity = totalPendingAllocationQuantity;
    }

    public Long getTotalOpenPickQuantity() {
        return totalOpenPickQuantity;
    }

    public void setTotalOpenPickQuantity(Long totalOpenPickQuantity) {
        this.totalOpenPickQuantity = totalOpenPickQuantity;
    }

    public Long getTotalPickedQuantity() {
        return totalPickedQuantity;
    }

    public void setTotalPickedQuantity(Long totalPickedQuantity) {
        this.totalPickedQuantity = totalPickedQuantity;
    }

    public Long getTotalShippedQuantity() {
        return totalShippedQuantity;
    }

    public void setTotalShippedQuantity(Long totalShippedQuantity) {
        this.totalShippedQuantity = totalShippedQuantity;
    }

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderCategory getCategory() {
        return category;
    }

    public void setCategory(OrderCategory category) {
        this.category = category;
    }

    public String getTransferReceiptNumber() {
        return transferReceiptNumber;
    }

    public void setTransferReceiptNumber(String transferReceiptNumber) {
        this.transferReceiptNumber = transferReceiptNumber;
    }

    public Long getTransferReceiptWarehouseId() {
        return transferReceiptWarehouseId;
    }

    public void setTransferReceiptWarehouseId(Long transferReceiptWarehouseId) {
        this.transferReceiptWarehouseId = transferReceiptWarehouseId;
    }

    public Long getStageLocationId() {
        return stageLocationId;
    }

    public void setStageLocationId(Long stageLocationId) {
        this.stageLocationId = stageLocationId;
    }

    public Location getStageLocation() {
        return stageLocation;
    }

    public void setStageLocation(Location stageLocation) {
        this.stageLocation = stageLocation;
    }

    public List<OrderDocument> getOrderDocuments() {
        return orderDocuments;
    }

    public void setOrderDocuments(List<OrderDocument> orderDocuments) {
        this.orderDocuments = orderDocuments;
    }

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
    }

    public String getQuickbookCustomerListId() {
        return quickbookCustomerListId;
    }

    public void setQuickbookCustomerListId(String quickbookCustomerListId) {
        this.quickbookCustomerListId = quickbookCustomerListId;
    }

    public List<ParcelPackage> getParcelPackages() {
        return parcelPackages;
    }

    public void setParcelPackages(List<ParcelPackage> parcelPackages) {
        this.parcelPackages = parcelPackages;
    }

    public List<OrderBillableActivity> getOrderBillableActivities() {
        return orderBillableActivities;
    }

    public void setOrderBillableActivities(List<OrderBillableActivity> orderBillableActivities) {
        this.orderBillableActivities = orderBillableActivities;
    }

    public List<ShipmentRequest> getHualeiShipmentRequests() {
        return hualeiShipmentRequests;
    }

    public void setHualeiShipmentRequests(List<ShipmentRequest> hualeiShipmentRequests) {
        this.hualeiShipmentRequests = hualeiShipmentRequests;
    }

    public String getShipToContactorPhoneNumber() {
        return shipToContactorPhoneNumber;
    }

    public void setShipToContactorPhoneNumber(String shipToContactorPhoneNumber) {
        this.shipToContactorPhoneNumber = shipToContactorPhoneNumber;
    }

    public Boolean getCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(Boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    public ZonedDateTime getCancelRequestedTime() {
        return cancelRequestedTime;
    }

    public void setCancelRequestedTime(ZonedDateTime cancelRequestedTime) {
        this.cancelRequestedTime = cancelRequestedTime;
    }

    public String getCancelRequestedUsername() {
        return cancelRequestedUsername;
    }

    public void setCancelRequestedUsername(String cancelRequestedUsername) {
        this.cancelRequestedUsername = cancelRequestedUsername;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Boolean getAllowForManualPick() {
        return allowForManualPick;
    }

    public void setAllowForManualPick(Boolean allowForManualPick) {
        this.allowForManualPick = allowForManualPick;
    }
}

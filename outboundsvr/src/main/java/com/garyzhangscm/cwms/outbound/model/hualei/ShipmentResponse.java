package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.OrderLine;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hualei_shipment_response")
public class ShipmentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipment_response_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "ack")
    @JsonProperty("ack")
    private String ack;

    @Column(name = "attr1")
    @JsonProperty("attr1")
    private String attr1;

    @Column(name = "attr2")
    @JsonProperty("attr2")
    private String attr2;

    @JsonProperty("childList")
    @OneToMany(
            mappedBy = "shipmentResponse",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<ShipmentResponseChild> childList = new ArrayList<>();

    @Column(name = "delay_type")
    @JsonProperty("delay_type")
    private String delayType;

    @Column(name = "is_change_numbers")
    @JsonProperty("is_changenumbers")
    private String isChangeNumbers;

    @Column(name = "is_delay")
    @JsonProperty("is_delay")
    private String isDelay;

    @Column(name = "is_remote")
    @JsonProperty("is_remote")
    private String isRemote;

    @Column(name = "is_residential")
    @JsonProperty("is_residential")
    private String isResidential;

    @Column(name = "message")
    @JsonProperty("message")
    private String message;

    @Column(name = "order_id")
    @JsonProperty("order_id")
    private String orderId;

    @Column(name = "order_private_code")
    @JsonProperty("order_privatecode")
    private String orderPrivateCode;

    @Column(name = "order_transfer_code")
    @JsonProperty("order_transfercode")
    private String orderTransferCode;

    @Column(name = "order_price_trial_amount")
    @JsonProperty("orderpricetrial_amount")
    private String orderPriceTrialAmount;

    @Column(name = "order_price_trial_currency")
    @JsonProperty("orderpricetrial_currency")
    private String orderPriceTrialCurrency;

    @Column(name = "post_customer_name")
    @JsonProperty("post_customername")
    private String postCustomerName;

    @Column(name = "product_track_no_api_type")
    @JsonProperty("product_tracknoapitype")
    private String productTrackNoApiType;

    @Column(name = "reference_number")
    @JsonProperty("reference_number")
    private String referenceNumber;

    @Column(name = "return_address")
    @JsonProperty("return_address")
    private String returnAddress;

    @Column(name = "tracking_number")
    @JsonProperty("tracking_number")
    private String trackingNumber;

    @OneToOne(mappedBy = "shipmentResponse")
    @JsonIgnore
    private ShipmentRequest shipmentRequest;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public String getAttr2() {
        return attr2;
    }

    public void setAttr2(String attr2) {
        this.attr2 = attr2;
    }

    public List<ShipmentResponseChild> getChildList() {
        return childList;
    }

    public void setChildList(List<ShipmentResponseChild> childList) {
        this.childList = childList;
    }

    public String getDelayType() {
        return delayType;
    }

    public void setDelayType(String delayType) {
        this.delayType = delayType;
    }


    public ShipmentRequest getShipmentRequest() {
        return shipmentRequest;
    }

    public void setShipmentRequest(ShipmentRequest shipmentRequest) {
        this.shipmentRequest = shipmentRequest;
    }

    public String getIsDelay() {
        return isDelay;
    }

    public void setIsDelay(String isDelay) {
        this.isDelay = isDelay;
    }

    public String getIsRemote() {
        return isRemote;
    }

    public void setIsRemote(String isRemote) {
        this.isRemote = isRemote;
    }

    public String getIsResidential() {
        return isResidential;
    }

    public void setIsResidential(String isResidential) {
        this.isResidential = isResidential;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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

    public String getIsChangeNumbers() {
        return isChangeNumbers;
    }

    public void setIsChangeNumbers(String isChangeNumbers) {
        this.isChangeNumbers = isChangeNumbers;
    }

    public String getOrderPrivateCode() {
        return orderPrivateCode;
    }

    public void setOrderPrivateCode(String orderPrivateCode) {
        this.orderPrivateCode = orderPrivateCode;
    }

    public String getOrderTransferCode() {
        return orderTransferCode;
    }

    public void setOrderTransferCode(String orderTransferCode) {
        this.orderTransferCode = orderTransferCode;
    }

    public String getOrderPriceTrialAmount() {
        return orderPriceTrialAmount;
    }

    public void setOrderPriceTrialAmount(String orderPriceTrialAmount) {
        this.orderPriceTrialAmount = orderPriceTrialAmount;
    }

    public String getOrderPriceTrialCurrency() {
        return orderPriceTrialCurrency;
    }

    public void setOrderPriceTrialCurrency(String orderPriceTrialCurrency) {
        this.orderPriceTrialCurrency = orderPriceTrialCurrency;
    }

    public String getPostCustomerName() {
        return postCustomerName;
    }

    public void setPostCustomerName(String postCustomerName) {
        this.postCustomerName = postCustomerName;
    }

    public String getProductTrackNoApiType() {
        return productTrackNoApiType;
    }

    public void setProductTrackNoApiType(String productTrackNoApiType) {
        this.productTrackNoApiType = productTrackNoApiType;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(String returnAddress) {
        this.returnAddress = returnAddress;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}

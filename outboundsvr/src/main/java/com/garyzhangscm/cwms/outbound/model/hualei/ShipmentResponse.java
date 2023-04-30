package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class ShipmentResponse {
    @JsonProperty("ack")
    private String ack;
    @JsonProperty("attr1")
    private String attr1;
    @JsonProperty("attr2")
    private String attr2;
    @JsonProperty("childList")
    private List<ShipmentResponseChild> childList = new ArrayList<>();
    @JsonProperty("delay_type")
    private String delayType;
    @JsonProperty("is_changenumbers")
    private String isChangenumbers;
    @JsonProperty("is_delay")
    private String isDelay;
    @JsonProperty("is_remote")
    private String isRemote;
    @JsonProperty("is_residential")
    private String isResidential;
    @JsonProperty("message")
    private String message;
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("order_privatecode")
    private String orderPrivatecode;
    @JsonProperty("order_transfercode")
    private String orderTransfercode;
    @JsonProperty("orderpricetrial_amount")
    private String orderpricetrialAmount;
    @JsonProperty("orderpricetrial_currency")
    private String orderpricetrialCurrency;
    @JsonProperty("post_customername")
    private String postCustomername;
    @JsonProperty("product_tracknoapitype")
    private String productTracknoapitype;
    @JsonProperty("reference_number")
    private String referenceNumber;
    @JsonProperty("return_address")
    private String returnAddress;
    @JsonProperty("tracking_number")
    private String trackingNumber;

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

    public String getIsChangenumbers() {
        return isChangenumbers;
    }

    public void setIsChangenumbers(String isChangenumbers) {
        this.isChangenumbers = isChangenumbers;
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

    public String getOrderPrivatecode() {
        return orderPrivatecode;
    }

    public void setOrderPrivatecode(String orderPrivatecode) {
        this.orderPrivatecode = orderPrivatecode;
    }

    public String getOrderTransfercode() {
        return orderTransfercode;
    }

    public void setOrderTransfercode(String orderTransfercode) {
        this.orderTransfercode = orderTransfercode;
    }

    public String getOrderpricetrialAmount() {
        return orderpricetrialAmount;
    }

    public void setOrderpricetrialAmount(String orderpricetrialAmount) {
        this.orderpricetrialAmount = orderpricetrialAmount;
    }

    public String getOrderpricetrialCurrency() {
        return orderpricetrialCurrency;
    }

    public void setOrderpricetrialCurrency(String orderpricetrialCurrency) {
        this.orderpricetrialCurrency = orderpricetrialCurrency;
    }

    public String getPostCustomername() {
        return postCustomername;
    }

    public void setPostCustomername(String postCustomername) {
        this.postCustomername = postCustomername;
    }

    public String getProductTracknoapitype() {
        return productTracknoapitype;
    }

    public void setProductTracknoapitype(String productTracknoapitype) {
        this.productTracknoapitype = productTracknoapitype;
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

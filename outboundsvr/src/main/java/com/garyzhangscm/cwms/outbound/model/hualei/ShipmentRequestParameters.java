package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShipmentRequestParameters {
    @JsonProperty("cargo_type")
    private String cargo_type;
    @JsonProperty("consignee_address")
    private String consignee_address;
    @JsonProperty("consignee_city")
    private String consignee_city;
    @JsonProperty("consignee_name")
    private String consignee_name;
    @JsonProperty("consignee_postcode")
    private String consignee_postcode;
    @JsonProperty("consignee_state")
    private String consignee_state;
    @JsonProperty("consignee_telephone")
    private String consignee_telephone;
    @JsonProperty("country")
    private String country;
    @JsonProperty("customer_id")
    private String customer_id;
    @JsonProperty("customer_userid")
    private String customer_userid;
    @JsonProperty("customs_clearance")
    private String customs_clearance;
    @JsonProperty("customs_declaration")
    private String customs_declaration;
    @JsonProperty("duty_type")
    private String duty_type;
    @JsonProperty("from")
    private String from;
    @JsonProperty("is_fba")
    private String is_fba;
    @JsonProperty("order_customerinvoicecode")
    private String order_customerinvoicecode;
    @JsonProperty("order_piece")
    private Integer order_piece;
    @JsonProperty("order_returnsign")
    private String order_returnsign;
    @JsonProperty("product_id")
    private String product_id;
    @JsonProperty("weight")
    private Double weight;
    @JsonProperty("orderVolumeParam")
    private ShipmentRequestOrderVolumeParameters orderVolumeParam;
    @JsonProperty("orderInvoiceParam")
    private ShipmentRequestOrderInvoiceParameters orderInvoiceParam;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getCargo_type() {
        return cargo_type;
    }

    public void setCargo_type(String cargo_type) {
        this.cargo_type = cargo_type;
    }

    public String getConsignee_address() {
        return consignee_address;
    }

    public void setConsignee_address(String consignee_address) {
        this.consignee_address = consignee_address;
    }

    public String getConsignee_city() {
        return consignee_city;
    }

    public void setConsignee_city(String consignee_city) {
        this.consignee_city = consignee_city;
    }

    public String getConsignee_name() {
        return consignee_name;
    }

    public void setConsignee_name(String consignee_name) {
        this.consignee_name = consignee_name;
    }

    public String getConsignee_postcode() {
        return consignee_postcode;
    }

    public void setConsignee_postcode(String consignee_postcode) {
        this.consignee_postcode = consignee_postcode;
    }

    public String getConsignee_state() {
        return consignee_state;
    }

    public void setConsignee_state(String consignee_state) {
        this.consignee_state = consignee_state;
    }

    public String getConsignee_telephone() {
        return consignee_telephone;
    }

    public void setConsignee_telephone(String consignee_telephone) {
        this.consignee_telephone = consignee_telephone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getCustomer_userid() {
        return customer_userid;
    }

    public void setCustomer_userid(String customer_userid) {
        this.customer_userid = customer_userid;
    }

    public String getCustoms_clearance() {
        return customs_clearance;
    }

    public void setCustoms_clearance(String customs_clearance) {
        this.customs_clearance = customs_clearance;
    }

    public String getCustoms_declaration() {
        return customs_declaration;
    }

    public void setCustoms_declaration(String customs_declaration) {
        this.customs_declaration = customs_declaration;
    }

    public String getDuty_type() {
        return duty_type;
    }

    public void setDuty_type(String duty_type) {
        this.duty_type = duty_type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getIs_fba() {
        return is_fba;
    }

    public void setIs_fba(String is_fba) {
        this.is_fba = is_fba;
    }

    public String getOrder_customerinvoicecode() {
        return order_customerinvoicecode;
    }

    public void setOrder_customerinvoicecode(String order_customerinvoicecode) {
        this.order_customerinvoicecode = order_customerinvoicecode;
    }

    public Integer getOrder_piece() {
        return order_piece;
    }

    public void setOrder_piece(Integer order_piece) {
        this.order_piece = order_piece;
    }

    public String getOrder_returnsign() {
        return order_returnsign;
    }

    public void setOrder_returnsign(String order_returnsign) {
        this.order_returnsign = order_returnsign;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public ShipmentRequestOrderVolumeParameters getOrderVolumeParam() {
        return orderVolumeParam;
    }

    public void setOrderVolumeParam(ShipmentRequestOrderVolumeParameters orderVolumeParam) {
        this.orderVolumeParam = orderVolumeParam;
    }

    public ShipmentRequestOrderInvoiceParameters getOrderInvoiceParam() {
        return orderInvoiceParam;
    }

    public void setOrderInvoiceParam(ShipmentRequestOrderInvoiceParameters orderInvoiceParam) {
        this.orderInvoiceParam = orderInvoiceParam;
    }
}

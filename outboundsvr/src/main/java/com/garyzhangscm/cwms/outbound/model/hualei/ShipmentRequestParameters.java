package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;

@Entity
@Table(name = "hualei_shipment_request_parameters")
public class ShipmentRequestParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipment_request_parameters_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "cargo_type")
    @JsonProperty("cargo_type")
    private String cargoType;

    @Column(name = "consignee_address")
    @JsonProperty("consignee_address")
    private String consigneeAddress;

    @Column(name = "consignee_city")
    @JsonProperty("consignee_city")
    private String consigneeCity;

    @Column(name = "consignee_name")
    @JsonProperty("consignee_name")
    private String consigneeName;

    @Column(name = "consignee_postcode")
    @JsonProperty("consignee_postcode")
    private String consigneePostcode;

    @Column(name = "consignee_state")
    @JsonProperty("consignee_state")
    private String consigneeState;

    @Column(name = "consignee_telephone")
    @JsonProperty("consignee_telephone")
    private String consigneeTelephone;

    @Column(name = "country")
    @JsonProperty("country")
    private String country;

    @Column(name = "customer_id")
    @JsonProperty("customer_id")
    private String customerId;

    @Column(name = "customer_userid")
    @JsonProperty("customer_userid")
    private String customerUserid;

    @Column(name = "customs_clearance")
    @JsonProperty("customs_clearance")
    private String customsClearance;

    @Column(name = "customs_declaration")
    @JsonProperty("customs_declaration")
    private String customsDeclaration;

    @Column(name = "duty_type")
    @JsonProperty("duty_type")
    private String dutyType;

    @Column(name = "ship_from")
    @JsonProperty("from")
    private String shipFrom;

    @Column(name = "is_fba")
    @JsonProperty("is_fba")
    private String isFba;

    @Column(name = "order_customer_invoice_code")
    @JsonProperty("order_customerinvoicecode")
    private String orderCustomerInvoiceCode;

    @Column(name = "order_piece")
    @JsonProperty("order_piece")
    private Integer orderPiece;

    @Column(name = "order_return_sign")
    @JsonProperty("order_returnsign")
    private String orderReturnSign;

    @Column(name = "product_id")
    @JsonProperty("product_id")
    private String productId;

    @Column(name = "weight")
    @JsonProperty("weight")
    private Double weight;

    @OneToOne(mappedBy = "shipmentRequestParameters")
    @JsonIgnore
    private ShipmentRequest shipmentRequest;

    @JsonProperty("orderVolumeParam")
    @OneToOne
    @JoinColumn(name="hualei_shipment_request_order_volume_parameters_id")
    private ShipmentRequestOrderVolumeParameters orderVolumeParam;

    @JsonProperty("orderInvoiceParam")
    @OneToOne
    @JoinColumn(name="hualei_shipment_request_order_invoice_parameters_id")
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

    public String getCargoType() {
        return cargoType;
    }

    public void setCargoType(String cargoType) {
        this.cargoType = cargoType;
    }


    public String getConsigneeAddress() {
        return consigneeAddress;
    }

    public void setConsigneeAddress(String consigneeAddress) {
        this.consigneeAddress = consigneeAddress;
    }

    public String getConsigneeCity() {
        return consigneeCity;
    }

    public void setConsigneeCity(String consigneeCity) {
        this.consigneeCity = consigneeCity;
    }

    public String getConsigneeName() {
        return consigneeName;
    }

    public ShipmentRequest getShipmentRequest() {
        return shipmentRequest;
    }

    public void setShipmentRequest(ShipmentRequest shipmentRequest) {
        this.shipmentRequest = shipmentRequest;
    }

    public void setConsigneeName(String consigneeName) {
        this.consigneeName = consigneeName;
    }

    public String getConsigneePostcode() {
        return consigneePostcode;
    }

    public void setConsigneePostcode(String consigneePostcode) {
        this.consigneePostcode = consigneePostcode;
    }

    public String getConsigneeState() {
        return consigneeState;
    }

    public void setConsigneeState(String consigneeState) {
        this.consigneeState = consigneeState;
    }

    public String getConsigneeTelephone() {
        return consigneeTelephone;
    }

    public void setConsigneeTelephone(String consigneeTelephone) {
        this.consigneeTelephone = consigneeTelephone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerUserid() {
        return customerUserid;
    }

    public void setCustomerUserid(String customerUserid) {
        this.customerUserid = customerUserid;
    }

    public String getCustomsClearance() {
        return customsClearance;
    }

    public void setCustomsClearance(String customsClearance) {
        this.customsClearance = customsClearance;
    }

    public String getCustomsDeclaration() {
        return customsDeclaration;
    }

    public void setCustomsDeclaration(String customsDeclaration) {
        this.customsDeclaration = customsDeclaration;
    }

    public String getDutyType() {
        return dutyType;
    }

    public void setDutyType(String dutyType) {
        this.dutyType = dutyType;
    }

    public String getShipFrom() {
        return shipFrom;
    }

    public void setShipFrom(String shipFrom) {
        this.shipFrom = shipFrom;
    }

    public String getIsFba() {
        return isFba;
    }

    public void setIsFba(String isFba) {
        this.isFba = isFba;
    }



    public Integer getOrderPiece() {
        return orderPiece;
    }

    public void setOrderPiece(Integer orderPiece) {
        this.orderPiece = orderPiece;
    }

    public String getOrderCustomerInvoiceCode() {
        return orderCustomerInvoiceCode;
    }

    public void setOrderCustomerInvoiceCode(String orderCustomerInvoiceCode) {
        this.orderCustomerInvoiceCode = orderCustomerInvoiceCode;
    }

    public String getOrderReturnSign() {
        return orderReturnSign;
    }

    public void setOrderReturnSign(String orderReturnSign) {
        this.orderReturnSign = orderReturnSign;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

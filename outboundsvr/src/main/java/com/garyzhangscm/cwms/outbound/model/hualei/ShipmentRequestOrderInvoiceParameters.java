package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.AuditibleEntity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hualei_shipment_request_order_invoice_parameters")
public class ShipmentRequestOrderInvoiceParameters  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipment_request_order_invoice_parameters_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "box_no")
    @JsonProperty("box_no")
    private String boxNo;

    @Column(name = "hs_code")
    @JsonProperty("hs_code")
    private String hsCode;

    @Column(name = "invoice_amount")
    @JsonProperty("invoice_amount")
    private Double invoiceAmount;

    @Column(name = "invoice_pcs")
    @JsonProperty("invoice_pcs")
    private Integer invoicePieces;

    @Column(name = "invoice_title")
    @JsonProperty("invoice_title")
    private String invoiceTitle;

    @Column(name = "invoice_weight")
    @JsonProperty("invoice_weight")
    private Double invoiceWeight;

    @Column(name = "weight_unit")
    @JsonProperty("weight_unit")
    private String weightUnit;

    @Column(name = "sku")
    @JsonProperty("sku")
    private String sku;

    @Column(name = "sku_code")
    @JsonProperty("sku_code")
    private String skuCode;

    @ManyToOne
    @JoinColumn(name="hualei_shipment_request_parameters_id")
    @JsonIgnore
    private ShipmentRequestParameters shipmentRequestParameters;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ShipmentRequestParameters getShipmentRequestParameters() {
        return shipmentRequestParameters;
    }

    public void setShipmentRequestParameters(ShipmentRequestParameters shipmentRequestParameters) {
        this.shipmentRequestParameters = shipmentRequestParameters;
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

    public String getBoxNo() {
        return boxNo;
    }

    public void setBoxNo(String boxNo) {
        this.boxNo = boxNo;
    }

    public String getHsCode() {
        return hsCode;
    }

    public void setHsCode(String hsCode) {
        this.hsCode = hsCode;
    }

    public Double getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(Double invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public Integer getInvoicePieces() {
        return invoicePieces;
    }

    public void setInvoicePieces(Integer invoicePieces) {
        this.invoicePieces = invoicePieces;
    }

    public String getInvoiceTitle() {
        return invoiceTitle;
    }

    public void setInvoiceTitle(String invoiceTitle) {
        this.invoiceTitle = invoiceTitle;
    }

    public Double getInvoiceWeight() {
        return invoiceWeight;
    }

    public void setInvoiceWeight(Double invoiceWeight) {
        this.invoiceWeight = invoiceWeight;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }
}

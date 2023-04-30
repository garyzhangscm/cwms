package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShipmentRequestOrderInvoiceParameters {
    @JsonProperty("box_no")
    private String boxNo;
    @JsonProperty("hs_code")
    private String hsCode;
    @JsonProperty("invoice_amount")
    private Double invoiceAmount;
    @JsonProperty("invoice_pcs")
    private Double invoicePieces;
    @JsonProperty("invoice_title")
    private Double invoiceTitle;
    @JsonProperty("invoice_weight")
    private Double invoiceWeight;
    @JsonProperty("sku")
    private String sku;
    @JsonProperty("sku_code")
    private String skuCode;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public Double getInvoicePieces() {
        return invoicePieces;
    }

    public void setInvoicePieces(Double invoicePieces) {
        this.invoicePieces = invoicePieces;
    }

    public Double getInvoiceTitle() {
        return invoiceTitle;
    }

    public void setInvoiceTitle(Double invoiceTitle) {
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
}

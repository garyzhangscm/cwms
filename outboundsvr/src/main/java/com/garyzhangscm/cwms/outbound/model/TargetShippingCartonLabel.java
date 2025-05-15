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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Entity
@Table(name = "target_shipping_carton_label")
public class TargetShippingCartonLabel extends AuditibleEntity<String> implements Serializable  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "target_shipping_carton_label_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;



    @Column(name = "partner_id")
    private String partnerID;

    @Column(name = "doc_type")
    private String docType;

    @Column(name = "po_number")
    private String poNumber;

    @Column(name = "po_date")
    private String poDate;


    @Column(name = "ship_to_name")
    private String shipToName;

    @Column(name = "address_1")
    private String address1;


    @Column(name = "city_state_zip")
    private String cityStateZip;

    @Column(name = "zip_420")
    private String zip420;

    @Column(name = "line_item_number")
    private String lineItemNumber;

    @Column(name = "UOM")
    private String UOM;

    @Column(name = "piece_carton")
    private String pieceCarton;

    @Column(name = "item_number")
    private String itemNumber;

    @Column(name = "customer_sku")
    private String customerSKU;

    @Column(name = "UPC")
    @JsonProperty(value="UPC")
    private String UPC;

    @Column(name = "weight")
    private String weight;

    @Column(name = "shipped_quantity")
    private String shippedQuantity;

    @Column(name = "order_quantity")
    private String orderQuantity;

    // unique key
    @Column(name = "SSCC18")
    @JsonProperty(value="SSCC18")
    private String SSCC18;

    @Column(name = "ship_date")
    private String shipDate;

    @Column(name = "BOL")
    private String BOL;

    @Column(name = "SCAC")
    private String SCAC;

    @Column(name = "freight_type")
    private String freightType;

    @Column(name = "ship_id")
    private String shipId;

    @Column(name = "dpci_dashed")
    private String dpciDashed;

    @Column(name = "GTIN")
    @JsonProperty(value="GTIN")
    private String GTIN;


    // only if the carton label is attached to
    // certain pallet pick label so that the
    // pallet pick label will be printed along with
    // all the shipping labels that attached to it
    @ManyToOne
    @JoinColumn(name="pallet_pick_label_content_id")
    private PalletPickLabelContent palletPickLabelContent;

    @Column(name = "last_print_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastPrintTime;

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

    public String getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(String partnerID) {
        this.partnerID = partnerID;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getPoDate() {
        return poDate;
    }

    public void setPoDate(String poDate) {
        this.poDate = poDate;
    }

    public String getShipToName() {
        return shipToName;
    }

    public void setShipToName(String shipToName) {
        this.shipToName = shipToName;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getCityStateZip() {
        return cityStateZip;
    }

    public void setCityStateZip(String cityStateZip) {
        this.cityStateZip = cityStateZip;
    }

    public String getZip420() {
        return zip420;
    }

    public void setZip420(String zip420) {
        this.zip420 = zip420;
    }

    public String getLineItemNumber() {
        return lineItemNumber;
    }

    public void setLineItemNumber(String lineItemNumber) {
        this.lineItemNumber = lineItemNumber;
    }

    public String getUOM() {
        return UOM;
    }

    public void setUOM(String UOM) {
        this.UOM = UOM;
    }

    public String getPieceCarton() {
        return pieceCarton;
    }

    public void setPieceCarton(String pieceCarton) {
        this.pieceCarton = pieceCarton;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getCustomerSKU() {
        return customerSKU;
    }

    public void setCustomerSKU(String customerSKU) {
        this.customerSKU = customerSKU;
    }

    public String getUPC() {
        return UPC;
    }

    public void setUPC(String UPC) {
        this.UPC = UPC;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getShippedQuantity() {
        return shippedQuantity;
    }

    public void setShippedQuantity(String shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }

    public String getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(String orderQuantity) {
        this.orderQuantity = orderQuantity;
    }

    public String getSSCC18() {
        return SSCC18;
    }

    public void setSSCC18(String SSCC18) {
        this.SSCC18 = SSCC18;
    }

    public String getShipDate() {
        return shipDate;
    }

    public void setShipDate(String shipDate) {
        this.shipDate = shipDate;
    }

    public String getBOL() {
        return BOL;
    }

    public void setBOL(String BOL) {
        this.BOL = BOL;
    }

    public String getSCAC() {
        return SCAC;
    }

    public void setSCAC(String SCAC) {
        this.SCAC = SCAC;
    }

    public String getFreightType() {
        return freightType;
    }

    public void setFreightType(String freightType) {
        this.freightType = freightType;
    }

    public String getShipId() {
        return shipId;
    }

    public void setShipId(String shipId) {
        this.shipId = shipId;
    }

    public String getDpciDashed() {
        return dpciDashed;
    }

    public void setDpciDashed(String dpciDashed) {
        this.dpciDashed = dpciDashed;
    }

    public String getDpci() {
        return Strings.isBlank(dpciDashed) ? "" : dpciDashed.replaceAll("-", "");
    }

    public String getGTIN() {
        return GTIN;
    }

    public void setGTIN(String GTIN) {
        this.GTIN = GTIN;
    }

    public PalletPickLabelContent getPalletPickLabelContent() {
        return palletPickLabelContent;
    }

    public void setPalletPickLabelContent(PalletPickLabelContent palletPickLabelContent) {
        this.palletPickLabelContent = palletPickLabelContent;
    }

    public ZonedDateTime getLastPrintTime() {
        return lastPrintTime;
    }

    public void setLastPrintTime(ZonedDateTime lastPrintTime) {
        this.lastPrintTime = lastPrintTime;
    }
}

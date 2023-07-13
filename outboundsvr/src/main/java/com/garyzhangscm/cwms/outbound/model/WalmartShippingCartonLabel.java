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

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "walmart_shipping_carton_label")
public class WalmartShippingCartonLabel extends AuditibleEntity<String> implements Serializable  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "walmart_shipping_carton_label_id")
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

    @Column(name = "type")
    private String type;

    @Column(name = "dept")
    private String dept;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "ship_to")
    private String shipTo;

    @Column(name = "address_1")
    private String address1;

    @Column(name = "city_state_zip")
    private String cityStateZip;

    @Column(name = "DC")
    @JsonProperty(value="DC")
    private String DC;

    @Column(name = "GLN")
    @JsonProperty(value="GLN")
    private String GLN;

    @Column(name = "item_number")
    private String itemNumber;

    @Column(name = "WMIT")
    @JsonProperty(value="WMIT")
    private String WMIT;

    @Column(name = "UPC")
    @JsonProperty(value="UPC")
    private String UPC;

    @Column(name = "UK")
    @JsonProperty(value="UK")
    private String UK;

    @Column(name = "GTIN14")
    @JsonProperty(value="GTIN14")
    private String GTIN14;

    @Column(name = "weight")
    private String weight;

    @Column(name = "description")
    private String description;

    @Column(name = "order_quantity")
    private String orderQuantity;

    @Column(name = "UOM")
    private String UOM;

    @Column(name = "piece_carton")
    private String pieceCarton;

    @Column(name = "carton_quantity")
    private String cartonQuantity;

    // unique key
    @Column(name = "SSCC18")
    @JsonProperty(value="SSCC18")
    private String SSCC18;

    @Column(name = "ship_date")
    private String shipDate;

    @Column(name = "eta_date")
    private String etaDate;

    @Column(name = "SCAC")
    private String SCAC;

    @Column(name = "transportation_method")
    private String transportationMethod;

    @Column(name = "BOL")
    private String BOL;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "fob")
    private String FOB;

    @Column(name = "load_id")
    private String loadId;

    @Column(name = "appointment_number")
    private String appointmentNumber;

    @Column(name = "carrier_number")
    private String carrierNumber;

    @Column(name = "equipment_type")
    private String equipmentType;

    @Column(name = "equipment_initial")
    private String equipmentInitial;

    @Column(name = "equipment_number")
    private String equipmentNumber;

    @Column(name = "seal_number")
    private String sealNumber;

    @Column(name = "FOB_Mira")
    private String fobMira;

    @Column(name = "LOMA_COLTON_FAYETEVILLE")
    private String lomaColtonFayeteville;

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

    public String getSSCC18() {
        return SSCC18;
    }

    public void setSSCC18(String SSCC18) {
        this.SSCC18 = SSCC18;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getShipTo() {
        return shipTo;
    }

    public void setShipTo(String shipTo) {
        this.shipTo = shipTo;
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

    public String getDC() {
        return DC;
    }

    public void setDC(String DC) {
        this.DC = DC;
    }

    public String getGLN() {
        return GLN;
    }

    public void setGLN(String GLN) {
        this.GLN = GLN;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getWMIT() {
        return WMIT;
    }

    public void setWMIT(String WMIT) {
        this.WMIT = WMIT;
    }

    public String getUPC() {
        return UPC;
    }

    public void setUPC(String UPC) {
        this.UPC = UPC;
    }

    public String getUK() {
        return UK;
    }

    public void setUK(String UK) {
        this.UK = UK;
    }

    public String getGTIN14() {
        return GTIN14;
    }

    public void setGTIN14(String GTIN14) {
        this.GTIN14 = GTIN14;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(String orderQuantity) {
        this.orderQuantity = orderQuantity;
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

    public String getCartonQuantity() {
        return cartonQuantity;
    }

    public void setCartonQuantity(String cartonQuantity) {
        this.cartonQuantity = cartonQuantity;
    }

    public String getShipDate() {
        return shipDate;
    }

    public void setShipDate(String shipDate) {
        this.shipDate = shipDate;
    }

    public String getEtaDate() {
        return etaDate;
    }

    public void setEtaDate(String etaDate) {
        this.etaDate = etaDate;
    }

    public String getSCAC() {
        return SCAC;
    }

    public void setSCAC(String SCAC) {
        this.SCAC = SCAC;
    }

    public String getTransportationMethod() {
        return transportationMethod;
    }

    public void setTransportationMethod(String transportationMethod) {
        this.transportationMethod = transportationMethod;
    }

    public String getBOL() {
        return BOL;
    }

    public void setBOL(String BOL) {
        this.BOL = BOL;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getFOB() {
        return FOB;
    }

    public void setFOB(String FOB) {
        this.FOB = FOB;
    }

    public String getLoadId() {
        return loadId;
    }

    public void setLoadId(String loadId) {
        this.loadId = loadId;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getCarrierNumber() {
        return carrierNumber;
    }

    public void setCarrierNumber(String carrierNumber) {
        this.carrierNumber = carrierNumber;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getEquipmentInitial() {
        return equipmentInitial;
    }

    public void setEquipmentInitial(String equipmentInitial) {
        this.equipmentInitial = equipmentInitial;
    }

    public String getEquipmentNumber() {
        return equipmentNumber;
    }

    public void setEquipmentNumber(String equipmentNumber) {
        this.equipmentNumber = equipmentNumber;
    }

    public String getSealNumber() {
        return sealNumber;
    }

    public void setSealNumber(String sealNumber) {
        this.sealNumber = sealNumber;
    }

    public String getFobMira() {
        return fobMira;
    }

    public void setFobMira(String fobMira) {
        this.fobMira = fobMira;
    }

    public String getLomaColtonFayeteville() {
        return lomaColtonFayeteville;
    }

    public void setLomaColtonFayeteville(String lomaColtonFayeteville) {
        this.lomaColtonFayeteville = lomaColtonFayeteville;
    }
}

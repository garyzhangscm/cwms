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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;

public class OrderLineCSVWrapper {
    private String client;
    private String order;

    private String line;
    private String item;
    private Long expectedQuantity;
    private String unitOfMeasure;
    private String inventoryStatus;

    private String allocationStrategyType;


    // fields in order that we may need to map to
    // if we want to create both order and order line
    // in one file
    private String shipToCustomer;
    private String billToCustomerSameAsShipToCustomer;
    private String billToCustomer;

    private String shipToContactorFirstname;
    private String shipToContactorLastname;
    private String shipToContactorPhoneNumber;
    private String shipToAddressCountry;
    private String shipToAddressState;
    private String shipToAddressCounty;
    private String shipToAddressCity;
    private String shipToAddressDistrict;
    private String shipToAddressLine1;
    private String shipToAddressLine2;
    private String shipToAddressPostcode;

    private String billToAddressSameAsShipToAddress;

    private String billToContactorFirstname;
    private String billToContactorLastname;
    private String billToAddressCountry;
    private String billToAddressState;
    private String billToAddressCounty;
    private String billToAddressCity;
    private String billToAddressDistrict;
    private String billToAddressLine1;
    private String billToAddressLine2;
    private String billToAddressPostcode;


    private String color;
    private String productSize;
    private String style;

    private String inventoryAttribute1;
    private String inventoryAttribute2;
    private String inventoryAttribute3;
    private String inventoryAttribute4;
    private String inventoryAttribute5;

    private String hualeiProductId;
    private String autoRequestShippingLabel;


    private String allocateByReceiptNumber;

    private String parcelInsured;
    private Double parcelInsuredAmountPerUnit;
    private String parcelSignatureRequired;

    private String load;
    private Integer stopSequence;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }


    public String getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(String allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getShipToCustomer() {
        return shipToCustomer;
    }

    public void setShipToCustomer(String shipToCustomer) {
        this.shipToCustomer = shipToCustomer;
    }

    public String getBillToCustomerSameAsShipToCustomer() {
        return billToCustomerSameAsShipToCustomer;
    }

    public void setBillToCustomerSameAsShipToCustomer(String billToCustomerSameAsShipToCustomer) {
        this.billToCustomerSameAsShipToCustomer = billToCustomerSameAsShipToCustomer;
    }

    public String getHualeiProductId() {
        return hualeiProductId;
    }

    public void setHualeiProductId(String hualeiProductId) {
        this.hualeiProductId = hualeiProductId;
    }

    public String getInventoryAttribute1() {
        return inventoryAttribute1;
    }

    public void setInventoryAttribute1(String inventoryAttribute1) {
        this.inventoryAttribute1 = inventoryAttribute1;
    }

    public String getInventoryAttribute2() {
        return inventoryAttribute2;
    }

    public void setInventoryAttribute2(String inventoryAttribute2) {
        this.inventoryAttribute2 = inventoryAttribute2;
    }

    public String getInventoryAttribute3() {
        return inventoryAttribute3;
    }

    public void setInventoryAttribute3(String inventoryAttribute3) {
        this.inventoryAttribute3 = inventoryAttribute3;
    }

    public String getInventoryAttribute4() {
        return inventoryAttribute4;
    }

    public void setInventoryAttribute4(String inventoryAttribute4) {
        this.inventoryAttribute4 = inventoryAttribute4;
    }

    public String getInventoryAttribute5() {
        return inventoryAttribute5;
    }

    public void setInventoryAttribute5(String inventoryAttribute5) {
        this.inventoryAttribute5 = inventoryAttribute5;
    }

    public String getAutoRequestShippingLabel() {
        return autoRequestShippingLabel;
    }

    public void setAutoRequestShippingLabel(String autoRequestShippingLabel) {
        this.autoRequestShippingLabel = autoRequestShippingLabel;
    }

    public String getBillToCustomer() {
        return billToCustomer;
    }

    public void setBillToCustomer(String billToCustomer) {
        this.billToCustomer = billToCustomer;
    }

    public String getShipToContactorFirstname() {
        return shipToContactorFirstname;
    }

    public void setShipToContactorFirstname(String shipToContactorFirstname) {
        this.shipToContactorFirstname = shipToContactorFirstname;
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

    public String getBillToAddressSameAsShipToAddress() {
        return billToAddressSameAsShipToAddress;
    }

    public void setBillToAddressSameAsShipToAddress(String billToAddressSameAsShipToAddress) {
        this.billToAddressSameAsShipToAddress = billToAddressSameAsShipToAddress;
    }

    public String getBillToContactorFirstname() {
        return billToContactorFirstname;
    }

    public void setBillToContactorFirstname(String billToContactorFirstname) {
        this.billToContactorFirstname = billToContactorFirstname;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
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

    public String getShipToContactorPhoneNumber() {
        return shipToContactorPhoneNumber;
    }

    public void setShipToContactorPhoneNumber(String shipToContactorPhoneNumber) {
        this.shipToContactorPhoneNumber = shipToContactorPhoneNumber;
    }

    public String getParcelInsured() {
        return parcelInsured;
    }

    public void setParcelInsured(String parcelInsured) {
        this.parcelInsured = parcelInsured;
    }

    public String getParcelSignatureRequired() {
        return parcelSignatureRequired;
    }

    public void setParcelSignatureRequired(String parcelSignatureRequired) {
        this.parcelSignatureRequired = parcelSignatureRequired;
    }

    public Double getParcelInsuredAmountPerUnit() {
        return parcelInsuredAmountPerUnit;
    }

    public void setParcelInsuredAmountPerUnit(Double parcelInsuredAmountPerUnit) {
        this.parcelInsuredAmountPerUnit = parcelInsuredAmountPerUnit;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }
}

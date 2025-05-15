/**
 * Copyright 2018
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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer implements Serializable {

    private Long id;

    private Long warehouseId;
    private Long companyId;

    private String name;

    private String description;

    private Boolean listPickEnabledFlag ;

    private String contactorFirstname;
    private String contactorLastname;

    private String addressCountry;
    private String addressState;
    private String addressCounty;
    private String addressCity;
    private String addressDistrict;
    private String addressLine1;
    private String addressLine2;
    private String addressPostcode;

    // if the customer is target
    // if so, we will allow the user to print
    // target specific labels
    private Boolean customerIsTarget;
    private Boolean customerIsWalmart;

    private Boolean allowPrintShippingCartonLabel;
    private Boolean allowPrintShippingCartonLabelWithPalletLabel;
    private Boolean allowPrintShippingCartonLabelWithPalletLabelWhenShort;


    private Double maxPalletSize;
    private Double maxPalletHeight;

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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public void setContactorFirstname(String contactorFirstname) {
        this.contactorFirstname = contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public void setContactorLastname(String contactorLastname) {
        this.contactorLastname = contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public Boolean getListPickEnabledFlag() {
        return listPickEnabledFlag;
    }

    public void setListPickEnabledFlag(Boolean listPickEnabledFlag) {
        this.listPickEnabledFlag = listPickEnabledFlag;
    }

    public Boolean getCustomerIsTarget() {
        return customerIsTarget;
    }

    public void setCustomerIsTarget(Boolean customerIsTarget) {
        this.customerIsTarget = customerIsTarget;
    }

    public Boolean getCustomerIsWalmart() {
        return customerIsWalmart;
    }

    public void setCustomerIsWalmart(Boolean customerIsWalmart) {
        this.customerIsWalmart = customerIsWalmart;
    }

    public Boolean getAllowPrintShippingCartonLabel() {
        return allowPrintShippingCartonLabel;
    }

    public void setAllowPrintShippingCartonLabel(Boolean allowPrintShippingCartonLabel) {
        this.allowPrintShippingCartonLabel = allowPrintShippingCartonLabel;
    }

    public Boolean getAllowPrintShippingCartonLabelWithPalletLabel() {
        return allowPrintShippingCartonLabelWithPalletLabel;
    }

    public void setAllowPrintShippingCartonLabelWithPalletLabel(Boolean allowPrintShippingCartonLabelWithPalletLabel) {
        this.allowPrintShippingCartonLabelWithPalletLabel = allowPrintShippingCartonLabelWithPalletLabel;
    }

    public Boolean getAllowPrintShippingCartonLabelWithPalletLabelWhenShort() {
        return allowPrintShippingCartonLabelWithPalletLabelWhenShort;
    }

    public void setAllowPrintShippingCartonLabelWithPalletLabelWhenShort(Boolean allowPrintShippingCartonLabelWithPalletLabelWhenShort) {
        this.allowPrintShippingCartonLabelWithPalletLabelWhenShort = allowPrintShippingCartonLabelWithPalletLabelWhenShort;
    }

    public Double getMaxPalletSize() {
        return maxPalletSize;
    }

    public void setMaxPalletSize(Double maxPalletSize) {
        this.maxPalletSize = maxPalletSize;
    }

    public Double getMaxPalletHeight() {
        return maxPalletHeight;
    }

    public void setMaxPalletHeight(Double maxPalletHeight) {
        this.maxPalletHeight = maxPalletHeight;
    }
}

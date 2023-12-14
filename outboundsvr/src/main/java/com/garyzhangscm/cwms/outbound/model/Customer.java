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
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer implements Serializable {

    private Long id;

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
    private Boolean isTarget;

    private Boolean allowPrintTargetShippingCartonLabel;
    private Boolean allowPrintTargetShippingCartonLabelWithPalletLabel;
    private Boolean allowPrintTargetShippingCartonLabelWithPalletLabelWhenShort;


    // if the customer is walmart
    // if so, we will allow the user to print
    // walmart specific labels
    private Boolean isWalmart;

    private Boolean allowPrintWalmartShippingCartonLabel;
    private Boolean allowPrintWalmartShippingCartonLabelWithPalletLabel;
    private Boolean allowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort;


    private Double maxPalletSize;
    private Double maxPalletHeight;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getTarget() {
        return isTarget;
    }

    public void setTarget(Boolean target) {
        isTarget = target;
    }

    public Boolean getAllowPrintTargetShippingCartonLabel() {
        return allowPrintTargetShippingCartonLabel;
    }

    public void setAllowPrintTargetShippingCartonLabel(Boolean allowPrintTargetShippingCartonLabel) {
        this.allowPrintTargetShippingCartonLabel = allowPrintTargetShippingCartonLabel;
    }

    public Boolean getAllowPrintTargetShippingCartonLabelWithPalletLabel() {
        return allowPrintTargetShippingCartonLabelWithPalletLabel;
    }

    public void setAllowPrintTargetShippingCartonLabelWithPalletLabel(Boolean allowPrintTargetShippingCartonLabelWithPalletLabel) {
        this.allowPrintTargetShippingCartonLabelWithPalletLabel = allowPrintTargetShippingCartonLabelWithPalletLabel;
    }

    public Boolean getAllowPrintTargetShippingCartonLabelWithPalletLabelWhenShort() {
        return allowPrintTargetShippingCartonLabelWithPalletLabelWhenShort;
    }

    public void setAllowPrintTargetShippingCartonLabelWithPalletLabelWhenShort(Boolean allowPrintTargetShippingCartonLabelWithPalletLabelWhenShort) {
        this.allowPrintTargetShippingCartonLabelWithPalletLabelWhenShort = allowPrintTargetShippingCartonLabelWithPalletLabelWhenShort;
    }

    public Boolean getWalmart() {
        return isWalmart;
    }

    public void setWalmart(Boolean walmart) {
        isWalmart = walmart;
    }

    public Boolean getAllowPrintWalmartShippingCartonLabel() {
        return allowPrintWalmartShippingCartonLabel;
    }

    public void setAllowPrintWalmartShippingCartonLabel(Boolean allowPrintWalmartShippingCartonLabel) {
        this.allowPrintWalmartShippingCartonLabel = allowPrintWalmartShippingCartonLabel;
    }

    public Boolean getAllowPrintWalmartShippingCartonLabelWithPalletLabel() {
        return allowPrintWalmartShippingCartonLabelWithPalletLabel;
    }

    public void setAllowPrintWalmartShippingCartonLabelWithPalletLabel(Boolean allowPrintWalmartShippingCartonLabelWithPalletLabel) {
        this.allowPrintWalmartShippingCartonLabelWithPalletLabel = allowPrintWalmartShippingCartonLabelWithPalletLabel;
    }

    public Boolean getAllowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort() {
        return allowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort;
    }

    public void setAllowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort(Boolean allowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort) {
        this.allowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort = allowPrintWalmartShippingCartonLabelWithPalletLabelWhenShort;
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

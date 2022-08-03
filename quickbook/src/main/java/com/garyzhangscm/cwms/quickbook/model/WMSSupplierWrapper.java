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

package com.garyzhangscm.cwms.quickbook.model;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;


public class WMSSupplierWrapper implements Serializable {


    private static final Logger logger = LoggerFactory.getLogger(WMSSupplierWrapper.class);

    private Long warehouseId;
    private Long companyId;

    private String name;

    private String description;

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

    private String userDefinedField1;
    private String userDefinedField2;
    private String userDefinedField3;
    private String userDefinedField4;
    private String userDefinedField5;

    public WMSSupplierWrapper() {
    }
    public WMSSupplierWrapper(Vendor vendor) {
        setName(vendor.getDisplayName());
        setDescription(vendor.getDisplayName());
        setCompanyId(vendor.getCompanyId());
        setWarehouseId(vendor.getWarehouseId());
        setContactorFirstname(vendor.getGivenName());
        setContactorLastname(vendor.getFamilyName());
        Address address = Objects.nonNull(vendor.getShipAddr()) ?
                vendor.getShipAddr() : vendor.getBillAddr();
        if (Objects.nonNull(address)) {

            setAddressCountry(address.getCountry());
            setAddressState(address.getCountrySubDivisionCode());
            setAddressCounty(address.getCounty());
            setAddressCity(address.getCity());
            setAddressDistrict(address.getCity());
            setAddressLine1(address.getLine1());
            setAddressLine2(address.getLine2());
            setAddressPostcode(address.getPostalCode());
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getUserDefinedField1() {
        return userDefinedField1;
    }

    public void setUserDefinedField1(String userDefinedField1) {
        this.userDefinedField1 = userDefinedField1;
    }

    public String getUserDefinedField2() {
        return userDefinedField2;
    }

    public void setUserDefinedField2(String userDefinedField2) {
        this.userDefinedField2 = userDefinedField2;
    }

    public String getUserDefinedField3() {
        return userDefinedField3;
    }

    public void setUserDefinedField3(String userDefinedField3) {
        this.userDefinedField3 = userDefinedField3;
    }

    public String getUserDefinedField4() {
        return userDefinedField4;
    }

    public void setUserDefinedField4(String userDefinedField4) {
        this.userDefinedField4 = userDefinedField4;
    }

    public String getUserDefinedField5() {
        return userDefinedField5;
    }

    public void setUserDefinedField5(String userDefinedField5) {
        this.userDefinedField5 = userDefinedField5;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}

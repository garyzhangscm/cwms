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

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class SiloGroup implements Serializable {

    @JsonProperty("BILL_TO_KEY")
    private String billToKey;
    @JsonProperty("STATE")
    private String state;
    @JsonProperty("GROUP_ID")
    private String groupId;
    @JsonProperty("NAME")
    private String name;
    @JsonProperty("COUNTRY")
    private String country;
    @JsonProperty("TOTAL_ROWS")
    private String totalRows;
    @JsonProperty("ROW")
    private String row;
    @JsonProperty("IS_2FA_REQUIRED")
    private String is2FARequired;
    @JsonProperty("ALIAS")
    private String alias;
    @JsonProperty("PHONE")
    private String phone;
    @JsonProperty("ADDR2")
    private String addressLine2;
    @JsonProperty("COUNTRY_ID")
    private String countryId;
    @JsonProperty("ADDR1")
    private String addressLine1;
    @JsonProperty("ZIP")
    private String zipCode;
    @JsonProperty("STATE_ID")
    private String stateId;
    @JsonProperty("EMAIL")
    private String email;
    @JsonProperty("CITY")
    private String city;
    @JsonProperty("CUSTOMER_NUMBER")
    private String customerNumber;
    @JsonProperty("STATUS_CDE")
    private String statusCode;
    @JsonProperty("FAX")
    private String fax;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getBillToKey() {
        return billToKey;
    }

    public void setBillToKey(String billToKey) {
        this.billToKey = billToKey;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(String totalRows) {
        this.totalRows = totalRows;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public String getIs2FARequired() {
        return is2FARequired;
    }

    public void setIs2FARequired(String is2FARequired) {
        this.is2FARequired = is2FARequired;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }
}

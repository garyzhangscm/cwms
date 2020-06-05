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

package com.garyzhangscm.cwms.integration.model;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_client")
public class DBBasedClient implements Serializable, IntegrationClientData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_client_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


    @Column(name = "contactor_firstname")
    private String contactorFirstname;
    @Column(name = "contactor_lastname")
    private String contactorLastname;

    @Column(name = "address_country")
    private String addressCountry;
    @Column(name = "address_state")
    private String addressState;
    @Column(name = "address_county")
    private String addressCounty;
    @Column(name = "address_city")
    private String addressCity;
    @Column(name = "address_district")
    private String addressDistrict;
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    @Column(name = "address_postcode")
    private String addressPostcode;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;
    @Column(name = "insert_time")
    private LocalDateTime insertTime;
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
    @Column(name = "error_message")
    private String errorMessage;

    public Client convertToClient() {
        Client client = new Client();
        client.setName(getName());
        client.setDescription(getDescription());

        client.setContactorFirstname(getContactorFirstname());
        client.setContactorLastname(getContactorLastname());

        client.setAddressCountry(getAddressCountry());
        client.setAddressState(getAddressState());
        client.setAddressCounty(getAddressCounty());
        client.setAddressCity(getAddressCity());
        client.setAddressDistrict(getAddressDistrict());
        client.setAddressLine1(getAddressLine1());
        client.setAddressLine2(getAddressLine2());
        client.setAddressPostcode(getAddressPostcode());
        return client;
    }

    public DBBasedClient(){}


    public DBBasedClient(Client client) {

        setName(client.getName());
        setDescription(client.getDescription());

        setContactorFirstname(client.getContactorFirstname());
        setContactorLastname(client.getContactorLastname());

        setAddressCountry(client.getAddressCountry());
        setAddressState(client.getAddressState());
        setAddressCounty(client.getAddressCounty());
        setAddressCity(client.getAddressCity());
        setAddressDistrict(client.getAddressDistrict());
        setAddressLine1(client.getAddressLine1());
        setAddressLine2(client.getAddressLine2());
        setAddressPostcode(client.getAddressPostcode());

        setStatus(IntegrationStatus.PENDING);
        setInsertTime(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "DBBasedClient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", contactorFirstname='" + contactorFirstname + '\'' +
                ", contactorLastname='" + contactorLastname + '\'' +
                ", addressCountry='" + addressCountry + '\'' +
                ", addressState='" + addressState + '\'' +
                ", addressCounty='" + addressCounty + '\'' +
                ", addressCity='" + addressCity + '\'' +
                ", addressDistrict='" + addressDistrict + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", addressPostcode='" + addressPostcode + '\'' +
                ", status=" + status +
                ", insertTime=" + insertTime +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }

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

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(LocalDateTime insertTime) {
        this.insertTime = insertTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

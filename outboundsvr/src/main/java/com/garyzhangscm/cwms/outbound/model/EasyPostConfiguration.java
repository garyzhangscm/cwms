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
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "easy_post_configuration")
public class EasyPostConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "easy_post_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "api_key")
    private String apiKey;


    @Column(name = "webhook_secret")
    private String webhookSecret;


    // if we use the warehouse address as the ship from address
    @Column(name = "use_warehouse_address_as_ship_from_flag")
    private Boolean useWarehouseAddressAsShipFromFlag = true;

    // if we have a ship from address other than the warehouse address
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

    // address for return
    // if we use the warehouse address as the ship from address
    @Column(name = "use_warehouse_address_as_return_flag")
    private Boolean useWarehouseAddressAsReturnFlag = true;

    // if we have a ship from address other than the warehouse address
    @Column(name = "return_contactor_firstname")
    private String returnContactorFirstname;
    @Column(name = "return_contactor_lastname")
    private String returnContactorLastname;

    @Column(name = "return_address_country")
    private String returnAddressCountry;
    @Column(name = "return_address_state")
    private String returnAddressState;
    @Column(name = "return_address_county")
    private String returnAddressCounty;
    @Column(name = "return_address_city")
    private String returnAddressCity;
    @Column(name = "return_address_district")
    private String returnAddressDistrict;
    @Column(name = "return_address_line1")
    private String returnAddressLine1;
    @Column(name = "return_address_line2")
    private String returnAddressLine2;
    @Column(name = "return_address_postcode")
    private String returnAddressPostcode;

    @OneToMany(
            mappedBy = "easyPostConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<EasyPostCarrier> carriers = new ArrayList<>();

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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public List<EasyPostCarrier> getCarriers() {
        return carriers;
    }

    public void setCarriers(List<EasyPostCarrier> carriers) {
        this.carriers = carriers;
    }

    public Boolean getUseWarehouseAddressAsShipFromFlag() {
        return useWarehouseAddressAsShipFromFlag;
    }

    public void setUseWarehouseAddressAsShipFromFlag(Boolean useWarehouseAddressAsShipFromFlag) {
        this.useWarehouseAddressAsShipFromFlag = useWarehouseAddressAsShipFromFlag;
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

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
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

    public Boolean getUseWarehouseAddressAsReturnFlag() {
        return useWarehouseAddressAsReturnFlag;
    }

    public void setUseWarehouseAddressAsReturnFlag(Boolean useWarehouseAddressAsReturnFlag) {
        this.useWarehouseAddressAsReturnFlag = useWarehouseAddressAsReturnFlag;
    }

    public String getReturnContactorFirstname() {
        return returnContactorFirstname;
    }

    public void setReturnContactorFirstname(String returnContactorFirstname) {
        this.returnContactorFirstname = returnContactorFirstname;
    }

    public String getReturnContactorLastname() {
        return returnContactorLastname;
    }

    public void setReturnContactorLastname(String returnContactorLastname) {
        this.returnContactorLastname = returnContactorLastname;
    }

    public String getReturnAddressCountry() {
        return returnAddressCountry;
    }

    public void setReturnAddressCountry(String returnAddressCountry) {
        this.returnAddressCountry = returnAddressCountry;
    }

    public String getReturnAddressState() {
        return returnAddressState;
    }

    public void setReturnAddressState(String returnAddressState) {
        this.returnAddressState = returnAddressState;
    }

    public String getReturnAddressCounty() {
        return returnAddressCounty;
    }

    public void setReturnAddressCounty(String returnAddressCounty) {
        this.returnAddressCounty = returnAddressCounty;
    }

    public String getReturnAddressCity() {
        return returnAddressCity;
    }

    public void setReturnAddressCity(String returnAddressCity) {
        this.returnAddressCity = returnAddressCity;
    }

    public String getReturnAddressDistrict() {
        return returnAddressDistrict;
    }

    public void setReturnAddressDistrict(String returnAddressDistrict) {
        this.returnAddressDistrict = returnAddressDistrict;
    }

    public String getReturnAddressLine1() {
        return returnAddressLine1;
    }

    public void setReturnAddressLine1(String returnAddressLine1) {
        this.returnAddressLine1 = returnAddressLine1;
    }

    public String getReturnAddressLine2() {
        return returnAddressLine2;
    }

    public void setReturnAddressLine2(String returnAddressLine2) {
        this.returnAddressLine2 = returnAddressLine2;
    }

    public String getReturnAddressPostcode() {
        return returnAddressPostcode;
    }

    public void setReturnAddressPostcode(String returnAddressPostcode) {
        this.returnAddressPostcode = returnAddressPostcode;
    }
}

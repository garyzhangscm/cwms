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

package com.garyzhangscm.cwms.integration.model.tiktok;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.garyzhangscm.cwms.integration.model.AuditibleEntity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tiktok_seller_authorized_shop")
public class TikTokSellerAuthorizedShop extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tiktok_seller_authorized_shop_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "auth_code")
    private String authCode;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "client_id")
    private Long clientId;

    // shop id from tiktok
    @Column(name = "shop_id")
    @JsonProperty(value="shop_id")
    private String shopId;

    @Column(name = "name")
    @JsonProperty(value="shop_name")
    private String name;

    @Column(name = "region")
    private String region;

    @Column(name = "type")
    @JsonProperty(value="type")
    private String sellerType;

    @Column(name = "cipher")
    @JsonProperty(value="shop_cipher")
    private String cipher;

    @Column(name = "code")
    @JsonProperty(value="shop_code")
    private String code;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSellerType() {
        return sellerType;
    }

    public void setSellerType(String sellerType) {
        this.sellerType = sellerType;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

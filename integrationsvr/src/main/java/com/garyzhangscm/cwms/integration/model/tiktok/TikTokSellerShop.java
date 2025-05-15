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
@Table(name = "tiktok_seller_shop")
public class TikTokSellerShop extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tiktok_seller_shop_id")
    @JsonProperty(value="tiktok_seller_shop_id")
    private Long id;


    @Column(name = "auth_code")
    private String authCode;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "client_id")
    private Long clientId;

    // shop id from tiktok
    @Column(name = "shop_id")
    @JsonProperty(value="id")
    private String shopId;

    @Column(name = "region")
    private String region;

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


    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
}

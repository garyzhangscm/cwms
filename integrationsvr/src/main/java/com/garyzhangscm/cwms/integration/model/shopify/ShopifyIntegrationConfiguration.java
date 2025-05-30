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

package com.garyzhangscm.cwms.integration.model.shopify;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.garyzhangscm.cwms.integration.model.AuditibleEntity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "shopify_integration_configuration")
public class ShopifyIntegrationConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopify_integration_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "client_id")
    private Long clientId;


    @Column(name = "shop")
    private String shop;

    @Column(name = "access_token")
    @JsonProperty(value="access_token")
    private String accessToken;

    @Column(name = "scope")
    private String scope;

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

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}

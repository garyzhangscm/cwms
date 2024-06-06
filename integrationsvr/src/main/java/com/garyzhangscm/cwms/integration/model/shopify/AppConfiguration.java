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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.integration.model.AuditibleEntity;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "shopify_app_configuration")
public class AppConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopify_app_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "url")
    private String url;

    @JsonIgnore
    @Column(name = "shopify_app_client_id")
    private String shopifyAppClientId;
    @JsonIgnore
    @Column(name = "shopify_app_client_secret")
    private String shopifyAppClientSecret;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getShopifyAppClientId() {
        return shopifyAppClientId;
    }

    public void setShopifyAppClientId(String shopifyAppClientId) {
        this.shopifyAppClientId = shopifyAppClientId;
    }

    public String getShopifyAppClientSecret() {
        return shopifyAppClientSecret;
    }

    public void setShopifyAppClientSecret(String shopifyAppClientSecret) {
        this.shopifyAppClientSecret = shopifyAppClientSecret;
    }
}

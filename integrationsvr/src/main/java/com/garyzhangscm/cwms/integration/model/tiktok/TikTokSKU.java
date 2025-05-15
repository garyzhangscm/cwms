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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.AuditibleEntity;

import java.io.Serializable;

public class TikTokSKU extends AuditibleEntity<String> implements Serializable {

    @JsonProperty(value="external_sku_id")
    private String externalSkuId;
    @JsonProperty(value="id")
    private String id;

    @JsonProperty(value="identifier_code")
    private TikTokSKUIdentifierCode identifierCode;

    @JsonProperty(value="price")
    private TikTokPrice price;

    @JsonProperty(value="seller_sku")
    private String sellerSku;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getExternalSkuId() {
        return externalSkuId;
    }

    public void setExternalSkuId(String externalSkuId) {
        this.externalSkuId = externalSkuId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TikTokSKUIdentifierCode getIdentifierCode() {
        return identifierCode;
    }

    public void setIdentifierCode(TikTokSKUIdentifierCode identifierCode) {
        this.identifierCode = identifierCode;
    }

    public TikTokPrice getPrice() {
        return price;
    }

    public void setPrice(TikTokPrice price) {
        this.price = price;
    }

    public String getSellerSku() {
        return sellerSku;
    }

    public void setSellerSku(String sellerSku) {
        this.sellerSku = sellerSku;
    }
}

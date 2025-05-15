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
import java.util.List;

public class TikTokOrderLine extends AuditibleEntity<String> implements Serializable {

    @JsonProperty(value="currency")
    private String currency;
    @JsonProperty(value="display_status")
    private String displayStatus;

    @JsonProperty(value="id")
    private String id;

    @JsonProperty(value="is_gift")
    private Boolean isGift;

    @JsonProperty(value="item_tax")
    private List<TikTokItemTax> itemTax;

    @JsonProperty(value="original_price")
    private String originalPrice;

    @JsonProperty(value="platform_discount")
    private String platformDiscount;
    @JsonProperty(value="product_id")
    private String productId;
    @JsonProperty(value="product_name")
    private String productName;
    @JsonProperty(value="sale_price")
    private String salePrice;
    @JsonProperty(value="seller_discount")
    private String sellerDiscount;
    @JsonProperty(value="seller_sku")
    private String sellerSku;
    @JsonProperty(value="sku_id")
    private String skuId;
    @JsonProperty(value="sku_image")
    private String skuImage;
    @JsonProperty(value="sku_name")
    private String skuName;
    @JsonProperty(value="sku_type")
    private String skuType;





    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getGift() {
        return isGift;
    }

    public void setGift(Boolean gift) {
        isGift = gift;
    }

    public List<TikTokItemTax> getItemTax() {
        return itemTax;
    }

    public void setItemTax(List<TikTokItemTax> itemTax) {
        this.itemTax = itemTax;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getPlatformDiscount() {
        return platformDiscount;
    }

    public void setPlatformDiscount(String platformDiscount) {
        this.platformDiscount = platformDiscount;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(String salePrice) {
        this.salePrice = salePrice;
    }

    public String getSellerDiscount() {
        return sellerDiscount;
    }

    public void setSellerDiscount(String sellerDiscount) {
        this.sellerDiscount = sellerDiscount;
    }

    public String getSellerSku() {
        return sellerSku;
    }

    public void setSellerSku(String sellerSku) {
        this.sellerSku = sellerSku;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getSkuImage() {
        return skuImage;
    }

    public void setSkuImage(String skuImage) {
        this.skuImage = skuImage;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSkuType() {
        return skuType;
    }

    public void setSkuType(String skuType) {
        this.skuType = skuType;
    }
}

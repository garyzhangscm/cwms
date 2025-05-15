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

public class TikTokPayment extends AuditibleEntity<String> implements Serializable {

    @JsonProperty(value="currency")
    private String currency;

    @JsonProperty(value="original_shipping_fee")
    private String originalShippingFee;
    @JsonProperty(value="original_total_product_price")
    private String originalTotalProductPrice;
    @JsonProperty(value="platform_discount")
    private String platformDiscount;
    @JsonProperty(value="product_tax")
    private String productTax;
    @JsonProperty(value="seller_discount")
    private String sellerDiscount;
    @JsonProperty(value="shipping_fee")
    private String shippingFee;
    @JsonProperty(value="shipping_fee_platform_discount")
    private String shippingFeePlatformDiscount;
    @JsonProperty(value="shipping_fee_seller_discount")
    private String shippingFeeSellerDiscount;
    @JsonProperty(value="shipping_fee_tax")
    private String shippingFeeTax;
    @JsonProperty(value="sub_total")
    private String subTotal;

    @JsonProperty(value="tax")
    private String tax;

    @JsonProperty(value="total_amount")
    private String totalAmount;

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

    public String getOriginalShippingFee() {
        return originalShippingFee;
    }

    public void setOriginalShippingFee(String originalShippingFee) {
        this.originalShippingFee = originalShippingFee;
    }

    public String getOriginalTotalProductPrice() {
        return originalTotalProductPrice;
    }

    public void setOriginalTotalProductPrice(String originalTotalProductPrice) {
        this.originalTotalProductPrice = originalTotalProductPrice;
    }

    public String getPlatformDiscount() {
        return platformDiscount;
    }

    public void setPlatformDiscount(String platformDiscount) {
        this.platformDiscount = platformDiscount;
    }

    public String getProductTax() {
        return productTax;
    }

    public void setProductTax(String productTax) {
        this.productTax = productTax;
    }

    public String getSellerDiscount() {
        return sellerDiscount;
    }

    public void setSellerDiscount(String sellerDiscount) {
        this.sellerDiscount = sellerDiscount;
    }

    public String getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(String shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getShippingFeePlatformDiscount() {
        return shippingFeePlatformDiscount;
    }

    public void setShippingFeePlatformDiscount(String shippingFeePlatformDiscount) {
        this.shippingFeePlatformDiscount = shippingFeePlatformDiscount;
    }

    public String getShippingFeeSellerDiscount() {
        return shippingFeeSellerDiscount;
    }

    public void setShippingFeeSellerDiscount(String shippingFeeSellerDiscount) {
        this.shippingFeeSellerDiscount = shippingFeeSellerDiscount;
    }

    public String getShippingFeeTax() {
        return shippingFeeTax;
    }

    public void setShippingFeeTax(String shippingFeeTax) {
        this.shippingFeeTax = shippingFeeTax;
    }

    public String getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(String subTotal) {
        this.subTotal = subTotal;
    }

    public String getTax() {
        return tax;
    }

    public void setTax(String tax) {
        this.tax = tax;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }
}

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

public class TikTokOrder extends AuditibleEntity<String> implements Serializable {

    @JsonProperty(value="buyer_email")
    private String buyerEmail;

    @JsonProperty(value="buyer_message")
    private String buyerMessage;
    @JsonProperty(value="create_time")
    private Long createTime;
    @JsonProperty(value="delivery_option_id")
    private String deliveryOptionId;
    @JsonProperty(value="delivery_option_name")
    private String deliveryOptionName;

    @JsonProperty(value="fulfillment_type")
    private String fulfillmentType;
    @JsonProperty(value="has_updated_recipient_address")
    private Boolean hasUpdatedRecipientAddress;

    @JsonProperty(value="id")
    private String orderId;
    @JsonProperty(value="is_cod")
    private Boolean isCod;
    @JsonProperty(value="is_on_hold_order")
    private Boolean isOnHoldOrder;
    @JsonProperty(value="is_replacement_order")
    private Boolean isReplacementOrder;

    @JsonProperty(value="is_sample_order")
    private Boolean isSampleOrder;

    @JsonProperty(value="line_items")
    private List<TikTokOrderLine> lineItems;

    @JsonProperty(value="packages")
    private List<TikTokPackage> packages;

    @JsonProperty(value="paid_time")
    private Long paidTime;


    @JsonProperty(value="payment")
    private TikTokPayment payment;

    @JsonProperty(value="payment_method_name")
    private String paymentMethodName;

    @JsonProperty(value="recipient_address")
    private TikTokAddress recipientAddress;

    @JsonProperty(value="shipping_type")
    private String shippingType;
    @JsonProperty(value="status")
    private String status;

    @JsonProperty(value="update_time")
    private Long updateTime;

    @JsonProperty(value="user_id")
    private String userId;
    @JsonProperty(value="warehouse_id")
    private String warehouseId;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public String getBuyerMessage() {
        return buyerMessage;
    }

    public void setBuyerMessage(String buyerMessage) {
        this.buyerMessage = buyerMessage;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getDeliveryOptionId() {
        return deliveryOptionId;
    }

    public void setDeliveryOptionId(String deliveryOptionId) {
        this.deliveryOptionId = deliveryOptionId;
    }

    public String getDeliveryOptionName() {
        return deliveryOptionName;
    }

    public void setDeliveryOptionName(String deliveryOptionName) {
        this.deliveryOptionName = deliveryOptionName;
    }

    public String getFulfillmentType() {
        return fulfillmentType;
    }

    public void setFulfillmentType(String fulfillmentType) {
        this.fulfillmentType = fulfillmentType;
    }

    public Boolean getHasUpdatedRecipientAddress() {
        return hasUpdatedRecipientAddress;
    }

    public void setHasUpdatedRecipientAddress(Boolean hasUpdatedRecipientAddress) {
        this.hasUpdatedRecipientAddress = hasUpdatedRecipientAddress;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Boolean getCod() {
        return isCod;
    }

    public void setCod(Boolean cod) {
        isCod = cod;
    }

    public Boolean getOnHoldOrder() {
        return isOnHoldOrder;
    }

    public void setOnHoldOrder(Boolean onHoldOrder) {
        isOnHoldOrder = onHoldOrder;
    }

    public Boolean getReplacementOrder() {
        return isReplacementOrder;
    }

    public void setReplacementOrder(Boolean replacementOrder) {
        isReplacementOrder = replacementOrder;
    }

    public Boolean getSampleOrder() {
        return isSampleOrder;
    }

    public void setSampleOrder(Boolean sampleOrder) {
        isSampleOrder = sampleOrder;
    }

    public List<TikTokOrderLine> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<TikTokOrderLine> lineItems) {
        this.lineItems = lineItems;
    }

    public Long getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(Long paidTime) {
        this.paidTime = paidTime;
    }

    public TikTokPayment getPayment() {
        return payment;
    }

    public void setPayment(TikTokPayment payment) {
        this.payment = payment;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public TikTokAddress getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(TikTokAddress recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getShippingType() {
        return shippingType;
    }

    public void setShippingType(String shippingType) {
        this.shippingType = shippingType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public List<TikTokPackage> getPackages() {
        return packages;
    }

    public void setPackages(List<TikTokPackage> packages) {
        this.packages = packages;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
}

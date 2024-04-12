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
import com.garyzhangscm.cwms.outbound.service.ObjectCopyUtil;

import java.util.Objects;

public class OrderLineQueryWrapper {

    private String number;
    private String item;
    private Long expectedQuantity;
    private String inventoryStatus;

    private AllocationStrategyType allocationStrategyType;


    private String color;
    private String productSize;
    private String style;

    private String inventoryAttribute1;
    private String inventoryAttribute2;
    private String inventoryAttribute3;
    private String inventoryAttribute4;
    private String inventoryAttribute5;

    private Carrier carrier;

    private CarrierServiceLevel carrierServiceLevel;

    private String hualeiProductId;
    private Boolean autoRequestShippingLabel;


    public OrderLineQueryWrapper() {}
    public OrderLineQueryWrapper(OrderLine orderLine) {

        String[] fieldNames = {
             "number", "expectedQuantity", "allocationStrategyType",
                "color", "productSize", "style", "hualeiProductId", "autoRequestShippingLabel",
                "carrier", "carrierServiceLevel"
        };

        ObjectCopyUtil.copyValue(orderLine, this,  fieldNames);
        if (Objects.nonNull(orderLine.getItem())) {
            setItem(orderLine.getItem().getName());
        }
        if (Objects.nonNull(orderLine.getInventoryStatus())) {
            setInventoryStatus(orderLine.getInventoryStatus().getName());
        }

    }
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public AllocationStrategyType getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(AllocationStrategyType allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
    }



    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getInventoryAttribute1() {
        return inventoryAttribute1;
    }

    public void setInventoryAttribute1(String inventoryAttribute1) {
        this.inventoryAttribute1 = inventoryAttribute1;
    }

    public String getInventoryAttribute2() {
        return inventoryAttribute2;
    }

    public void setInventoryAttribute2(String inventoryAttribute2) {
        this.inventoryAttribute2 = inventoryAttribute2;
    }

    public String getInventoryAttribute3() {
        return inventoryAttribute3;
    }

    public void setInventoryAttribute3(String inventoryAttribute3) {
        this.inventoryAttribute3 = inventoryAttribute3;
    }

    public String getInventoryAttribute4() {
        return inventoryAttribute4;
    }

    public void setInventoryAttribute4(String inventoryAttribute4) {
        this.inventoryAttribute4 = inventoryAttribute4;
    }

    public String getInventoryAttribute5() {
        return inventoryAttribute5;
    }

    public void setInventoryAttribute5(String inventoryAttribute5) {
        this.inventoryAttribute5 = inventoryAttribute5;
    }

    public String getHualeiProductId() {
        return hualeiProductId;
    }

    public void setHualeiProductId(String hualeiProductId) {
        this.hualeiProductId = hualeiProductId;
    }

    public Boolean getAutoRequestShippingLabel() {
        return autoRequestShippingLabel;
    }

    public void setAutoRequestShippingLabel(Boolean autoRequestShippingLabel) {
        this.autoRequestShippingLabel = autoRequestShippingLabel;
    }
}

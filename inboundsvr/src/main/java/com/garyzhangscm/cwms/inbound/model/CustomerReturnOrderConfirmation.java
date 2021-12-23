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

package com.garyzhangscm.cwms.inbound.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomerReturnOrderConfirmation implements Serializable{


    private String number;


    private Long warehouseId;

    private String warehouseName;

    private Long clientId;

    private String clientName;

    private Long supplierId;

    private String supplierName;

    private List<CustomerReturnOrderLineConfirmation> customerReturnOrderLineConfirmations = new ArrayList<>();


    private Boolean allowUnexpectedItem;

    public CustomerReturnOrderConfirmation() {}
    public CustomerReturnOrderConfirmation(CustomerReturnOrder customerReturnOrder) {
        setNumber(customerReturnOrder.getNumber());

        setWarehouseId(customerReturnOrder.getWarehouseId());
        if (Objects.nonNull(customerReturnOrder.getWarehouse())) {

            setWarehouseName(customerReturnOrder.getWarehouse().getName());
        }

        setClientId(customerReturnOrder.getClientId());
        if (Objects.nonNull(customerReturnOrder.getClient())) {
            setClientName(customerReturnOrder.getClient().getName());
        }


        setAllowUnexpectedItem(customerReturnOrder.getAllowUnexpectedItem());

        customerReturnOrder.getCustomerReturnOrderLines().forEach(customerReturnOrderLine -> {
            CustomerReturnOrderLineConfirmation customerReturnOrderLineConfirmation =
                    new CustomerReturnOrderLineConfirmation(customerReturnOrderLine);

            addCustomerReturnOrderLineConfirmation(customerReturnOrderLineConfirmation);
        });


    }

    private void addCustomerReturnOrderLineConfirmation(CustomerReturnOrderLineConfirmation customerReturnOrderLineConfirmation) {
        if (Objects.isNull(customerReturnOrderLineConfirmations)) {
            customerReturnOrderLineConfirmations = new ArrayList<>();
        }
        customerReturnOrderLineConfirmations.add(customerReturnOrderLineConfirmation);
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public List<CustomerReturnOrderLineConfirmation> getCustomerReturnOrderLineConfirmations() {
        return customerReturnOrderLineConfirmations;
    }

    public void setCustomerReturnOrderLineConfirmations(List<CustomerReturnOrderLineConfirmation> customerReturnOrderLineConfirmations) {
        this.customerReturnOrderLineConfirmations = customerReturnOrderLineConfirmations;
    }

    public Boolean getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(Boolean allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }


}

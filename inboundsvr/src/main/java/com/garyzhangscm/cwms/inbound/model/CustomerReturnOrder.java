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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "customer_return_order")
public class CustomerReturnOrder {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_return_order_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @Column(name = "rma_number")
    private String RMANumber;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Transient
    private Customer customer;


    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private ReceiptCategory category = ReceiptCategory.CUSTOMER_RETURN;



    @OneToMany(
            mappedBy = "customerReturnOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<CustomerReturnOrderLine> customerReturnOrderLines = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReceiptStatus status = ReceiptStatus.OPEN;

    @Column(name = "allow_unexpected_item")
    private Boolean allowUnexpectedItem = false;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getRMANumber() {
        return RMANumber;
    }

    public void setRMANumber(String RMANumber) {
        this.RMANumber = RMANumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public ReceiptCategory getCategory() {
        return category;
    }

    public void setCategory(ReceiptCategory category) {
        this.category = category;
    }

    public List<CustomerReturnOrderLine> getCustomerReturnOrderLines() {
        return customerReturnOrderLines;
    }

    public void setCustomerReturnOrderLines(List<CustomerReturnOrderLine> customerReturnOrderLines) {
        this.customerReturnOrderLines = customerReturnOrderLines;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(ReceiptStatus status) {
        this.status = status;
    }

    public Boolean getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(Boolean allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}

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

package com.garyzhangscm.cwms.integration.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_order_confirmation")
public class DBBasedOrderConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationOrderConfirmationData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_order_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;


    @Column(name = "quickbook_customer_list_id")
    private String quickbookCustomerListId;

    @Column(name = "quickbook_txnid")
    private String quickbookTxnID;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedOrderLineConfirmation> orderLines = new ArrayList<>();


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public DBBasedOrderConfirmation(){}

    public DBBasedOrderConfirmation(OrderConfirmation orderConfirmation){



        setNumber(orderConfirmation.getNumber());
        setWarehouseId(orderConfirmation.getWarehouseId());
        setWarehouseName(orderConfirmation.getWarehouseName());
        setQuickbookCustomerListId(orderConfirmation.getQuickbookCustomerListId());
        setQuickbookTxnID(orderConfirmation.getQuickbookTxnID());

        setClientId(orderConfirmation.getClientId());
        setClient(orderConfirmation.getClient());


        orderConfirmation.getOrderLines().forEach(orderLineConfirmation -> {
            DBBasedOrderLineConfirmation dbBasedOrderLineConfirmation =
                    new DBBasedOrderLineConfirmation(orderLineConfirmation);
            dbBasedOrderLineConfirmation.setOrder(this);
            dbBasedOrderLineConfirmation.setStatus(IntegrationStatus.ATTACHED);
            addOrderLine(dbBasedOrderLineConfirmation);
        });

        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        setStatus(IntegrationStatus.PENDING);

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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    @Override
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    @Override
    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    @Override
    public List<DBBasedOrderLineConfirmation> getOrderLines() {
        return orderLines;
    }
    public void addOrderLine(DBBasedOrderLineConfirmation dbBasedOrderLineConfirmation) {
        if (Objects.isNull(orderLines)) {
            orderLines = new ArrayList<>();
        }
        orderLines.add(dbBasedOrderLineConfirmation);
    }

    public void setOrderLines(List<DBBasedOrderLineConfirmation> orderLines) {
        this.orderLines = orderLines;
    }

    public String getQuickbookCustomerListId() {
        return quickbookCustomerListId;
    }

    public void setQuickbookCustomerListId(String quickbookCustomerListId) {
        this.quickbookCustomerListId = quickbookCustomerListId;
    }

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
    }

    @Override
    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}

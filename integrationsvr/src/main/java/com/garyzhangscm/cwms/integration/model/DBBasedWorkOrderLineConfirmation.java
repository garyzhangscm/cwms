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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_work_order_line_confirmation")
public class DBBasedWorkOrderLineConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationWorkOrderLineConfirmationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_work_order_line_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;



    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;
    @Column(name = "open_quantity")
    private Long openQuantity;
    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;
    @Column(name = "delivered_quantity")
    private Long deliveredQuantity;
    @Column(name = "consumed_quantity")
    private Long consumedQuantity;
    @Column(name = "scrapped_quantity")
    private Long scrappedQuantity;
    @Column(name = "returned_quantity")
    private Long returnedQuantity;


    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Column(name = "inventory_status_name")
    private String inventoryStatusName;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_work_order_confirmation_id")
    private DBBasedWorkOrderConfirmation workOrderConfirmation;



    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public DBBasedWorkOrderLineConfirmation(){}

    public DBBasedWorkOrderLineConfirmation(WorkOrderLineConfirmation workOrderLineConfirmation){


        setNumber(workOrderLineConfirmation.getNumber());
        setItemId(workOrderLineConfirmation.getItemId());
        setItemName(workOrderLineConfirmation.getItemName());

        setExpectedQuantity(workOrderLineConfirmation.getExpectedQuantity());
        setOpenQuantity(workOrderLineConfirmation.getOpenQuantity());
        setInprocessQuantity(workOrderLineConfirmation.getInprocessQuantity());
        setDeliveredQuantity(workOrderLineConfirmation.getDeliveredQuantity());
        setConsumedQuantity(workOrderLineConfirmation.getConsumedQuantity());
        setScrappedQuantity(workOrderLineConfirmation.getScrappedQuantity());
        setReturnedQuantity(workOrderLineConfirmation.getReturnedQuantity());
        setInventoryStatusId(workOrderLineConfirmation.getInventoryStatusId());
        setInventoryStatusName(workOrderLineConfirmation.getInventoryStatusName());

        setCreatedTime(LocalDateTime.now());
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


    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Override
    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    @Override
    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    @Override
    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public Long getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(Long scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }

    public Long getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(Long returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }

    @Override
    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    @Override
    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }

    @Override
    public DBBasedWorkOrderConfirmation getWorkOrderConfirmation() {
        return workOrderConfirmation;
    }

    public void setWorkOrderConfirmation(DBBasedWorkOrderConfirmation workOrderConfirmation) {
        this.workOrderConfirmation = workOrderConfirmation;
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
}

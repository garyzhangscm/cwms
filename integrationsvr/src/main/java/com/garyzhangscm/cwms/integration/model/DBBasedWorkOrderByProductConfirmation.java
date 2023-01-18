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
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "integration_work_order_by_product_confirmation")
public class DBBasedWorkOrderByProductConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationWorkOrderByProductConfirmationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_work_order_by_product_confirmation_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;
    @Column(name = "produced_quantity")
    private Long producedQuantity;



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


    public DBBasedWorkOrderByProductConfirmation(){}

    public DBBasedWorkOrderByProductConfirmation(WorkOrderByProductConfirmation workOrderByProductConfirmation){

        setItemId(workOrderByProductConfirmation.getItemId());
        setItemName(workOrderByProductConfirmation.getItemName());

        setExpectedQuantity(workOrderByProductConfirmation.getExpectedQuantity());
        setProducedQuantity(workOrderByProductConfirmation.getProducedQuantity());

        setInventoryStatusId(workOrderByProductConfirmation.getInventoryStatusId());
        setInventoryStatusName(workOrderByProductConfirmation.getInventoryStatusName());

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


    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
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

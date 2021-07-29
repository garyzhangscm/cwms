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

package com.garyzhangscm.cwms.dblink.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "FTI_MES_ECO_WIP_COMP")
public class DBBasedWorkOrderReceivingConfirmation implements Serializable {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "completion_txn_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "quantity")
    private Long quantity;
    @Column(name = "job_name")
    private String workOrderNumber;
    @Column(name = "trx_date")
    private LocalDateTime transactionDate;

    public DBBasedWorkOrderReceivingConfirmation(){}

    public DBBasedWorkOrderReceivingConfirmation(
            DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation
    ){
        String id = new StringBuilder()
                .append(System.currentTimeMillis())
                .append((int)(Math.random() * 100)).toString();
        setId(Long.parseLong(id));
        setQuantity(dbBasedInventoryAdjustmentConfirmation.getAdjustQuantity());
        setWorkOrderNumber(dbBasedInventoryAdjustmentConfirmation.getDocumentNumber());

        setTransactionDate(dbBasedInventoryAdjustmentConfirmation.getInsertTime());

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

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}

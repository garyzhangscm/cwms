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
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "integration_work_order_confirmation")
public class DBBasedWorkOrderConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationWorkOrderConfirmationData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_work_order_confirmation_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "number")
    private String number;

    @Column(name = "production_line_name")
    private String productionLineName;

    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "bill_of_material_name")
    private String billOfMaterialName;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @Column(name = "produced_quantity")
    private Long producedQuantity;


    @OneToMany(
            mappedBy = "workOrderConfirmation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedWorkOrderLineConfirmation> workOrderLineConfirmations = new ArrayList<>();


    @OneToMany(
            mappedBy = "workOrderConfirmation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DBBasedWorkOrderByProductConfirmation> workOrderByProductConfirmations = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public DBBasedWorkOrderConfirmation(){}

    public DBBasedWorkOrderConfirmation(WorkOrderConfirmation workOrderConfirmation){



        setNumber(workOrderConfirmation.getNumber());
        setWarehouseId(workOrderConfirmation.getWarehouseId());
        setWarehouseName(workOrderConfirmation.getWarehouseName());
        setProductionLineName(workOrderConfirmation.getProductionLineName());

        setItemId(workOrderConfirmation.getItemId());
        setItemName(workOrderConfirmation.getItemName());
        setBillOfMaterialName(workOrderConfirmation.getBillOfMaterialName());
        setExpectedQuantity(workOrderConfirmation.getExpectedQuantity());
        setProducedQuantity(workOrderConfirmation.getProducedQuantity());


        workOrderConfirmation.getWorkOrderLines().forEach(workOrderLineConfirmation -> {
            DBBasedWorkOrderLineConfirmation dbBasedWorkOrderLineConfirmation =
                    new DBBasedWorkOrderLineConfirmation(workOrderLineConfirmation);
            dbBasedWorkOrderLineConfirmation.setWorkOrderConfirmation(this);
            dbBasedWorkOrderLineConfirmation.setStatus(IntegrationStatus.ATTACHED);
            addWorkOrderLineConfirmation(dbBasedWorkOrderLineConfirmation);
        });

        workOrderConfirmation.getWorkOrderByProducts().forEach(workOrderByProductConfirmation -> {
            DBBasedWorkOrderByProductConfirmation dbBasedWorkOrderLineConfirmation =
                    new DBBasedWorkOrderByProductConfirmation(workOrderByProductConfirmation);
            dbBasedWorkOrderLineConfirmation.setWorkOrderConfirmation(this);
            dbBasedWorkOrderLineConfirmation.setStatus(IntegrationStatus.ATTACHED);
            addWorkOrderByProductConfirmation(dbBasedWorkOrderLineConfirmation);
        });

        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        setStatus(IntegrationStatus.PENDING);

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

    public String getProductionLineName() {
        return productionLineName;
    }

    public void setProductionLineName(String productionLineName) {
        this.productionLineName = productionLineName;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public String getBillOfMaterialName() {
        return billOfMaterialName;
    }

    public void setBillOfMaterialName(String billOfMaterialName) {
        this.billOfMaterialName = billOfMaterialName;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    @Override
    public List<DBBasedWorkOrderLineConfirmation> getWorkOrderLineConfirmations() {
        return workOrderLineConfirmations;
    }

    public void setWorkOrderLineConfirmations(List<DBBasedWorkOrderLineConfirmation> workOrderLineConfirmations) {
        this.workOrderLineConfirmations = workOrderLineConfirmations;
    }

    public void addWorkOrderLineConfirmation(DBBasedWorkOrderLineConfirmation dbBasedWorkOrderLineConfirmation) {
        this.workOrderLineConfirmations.add(dbBasedWorkOrderLineConfirmation);
    }

    @Override
    public List<DBBasedWorkOrderByProductConfirmation> getWorkOrderByProductConfirmations() {
        return workOrderByProductConfirmations;
    }

    public void setWorkOrderByProductConfirmations(List<DBBasedWorkOrderByProductConfirmation> workOrderByProductConfirmations) {
        this.workOrderByProductConfirmations = workOrderByProductConfirmations;
    }

    public void addWorkOrderByProductConfirmation(DBBasedWorkOrderByProductConfirmation dBBasedWorkOrderByProductConfirmation) {
        this.workOrderByProductConfirmations.add(dBBasedWorkOrderByProductConfirmation);
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

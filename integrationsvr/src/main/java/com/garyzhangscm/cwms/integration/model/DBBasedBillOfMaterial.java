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
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "integration_bill_of_material")
public class DBBasedBillOfMaterial extends AuditibleEntity<String> implements Serializable, IntegrationBillOfMaterialData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_bill_of_material_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;


    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedBillOfMaterialLine> billOfMaterialLines = new HashSet<>();

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedWorkOrderInstructionTemplate> workOrderInstructionTemplates = new HashSet<>();

    @OneToMany(
            mappedBy = "billOfMaterial",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedBillOfMaterialByProduct> billOfMaterialByProducts = new HashSet<>();



    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public BillOfMaterial convertToBillOfMaterial() {

        BillOfMaterial billOfMaterial = new BillOfMaterial();

        String[] fieldNames = {
                "number",   "warehouseId", "itemId", "expectedQuantity"
        };

        ObjectCopyUtil.copyValue(this, billOfMaterial, fieldNames);

        // Copy each order line as well
        getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine -> {
            BillOfMaterialLine billOfMaterialLine = dbBasedBillOfMaterialLine.convertToBillOfMaterialLine();
            billOfMaterial.getBillOfMaterialLines().add(billOfMaterialLine);
        });

        getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct -> {
            BillOfMaterialByProduct billOfMaterialByProduct = dbBasedBillOfMaterialByProduct.convertToBillOfMaterialByProduct();
            billOfMaterial.getBillOfMaterialByProducts().add(billOfMaterialByProduct);
        });


        getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate -> {
            WorkOrderInstructionTemplate workOrderInstructionTemplate = dbBasedWorkOrderInstructionTemplate.convertToWorkOrderInstructionTemplate();
            billOfMaterial.getWorkOrderInstructionTemplates().add(workOrderInstructionTemplate);
        });


        return billOfMaterial;
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
    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    @Override
    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    @Override
    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    @Override
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    @Override
    public Set<DBBasedBillOfMaterialLine> getBillOfMaterialLines() {
        return billOfMaterialLines;
    }

    public void setBillOfMaterialLines(Set<DBBasedBillOfMaterialLine> billOfMaterialLines) {
        this.billOfMaterialLines = billOfMaterialLines;
    }

    @Override
    public Set<DBBasedWorkOrderInstructionTemplate> getWorkOrderInstructionTemplates() {
        return workOrderInstructionTemplates;
    }

    public void setWorkOrderInstructionTemplates(Set<DBBasedWorkOrderInstructionTemplate> workOrderInstructionTemplates) {
        this.workOrderInstructionTemplates = workOrderInstructionTemplates;
    }

    public Set<DBBasedBillOfMaterialByProduct> getBillOfMaterialByProducts() {
        return billOfMaterialByProducts;
    }

    public void setBillOfMaterialByProducts(Set<DBBasedBillOfMaterialByProduct> billOfMaterialByProducts) {
        this.billOfMaterialByProducts = billOfMaterialByProducts;
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

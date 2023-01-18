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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "integration_work_order")
public class DBBasedWorkOrder extends AuditibleEntity<String> implements Serializable, IntegrationWorkOrderData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_work_order_id")
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


    @Column(name = "po_number")
    private String poNumber;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedWorkOrderLine> workOrderLines = new HashSet<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedWorkOrderInstruction> workOrderInstructions = new HashSet<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<DBBasedWorkOrderByProduct> workOrderByProduct = new HashSet<>();



    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public WorkOrder convertToWorkOrder() {
        WorkOrder workOrder = new WorkOrder();

        String[] fieldNames = {
                "number",   "warehouseId", "itemId", "expectedQuantity", "poNumber"
        };

        ObjectCopyUtil.copyValue(this, workOrder, fieldNames);

        // Copy each order line as well
        getWorkOrderLines().forEach(dbBasedWorkOrderLine -> {
            WorkOrderLine workOrderLine = dbBasedWorkOrderLine.convertToWorkOrderLine();
            workOrder.getWorkOrderLines().add(workOrderLine);
        });

        getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct -> {
            WorkOrderByProduct workOrderByProduct = dbBasedWorkOrderByProduct.convertToWorkOrderByProduct();
            workOrder.getWorkOrderByProducts().add(workOrderByProduct);
        });


        getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction -> {
            WorkOrderInstruction workOrderInstruction = dbBasedWorkOrderInstruction.convertToWorkOrderInstruction();
            workOrder.getWorkOrderInstructions().add(workOrderInstruction);
        });


        return workOrder;
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
    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    @Override
    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    @Override
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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
    public Set<DBBasedWorkOrderLine> getWorkOrderLines() {
        return workOrderLines;
    }

    public void setWorkOrderLines(Set<DBBasedWorkOrderLine> workOrderLines) {
        this.workOrderLines = workOrderLines;
    }

    @Override
    public Set<DBBasedWorkOrderInstruction> getWorkOrderInstructions() {
        return workOrderInstructions;
    }

    public void setWorkOrderInstructions(Set<DBBasedWorkOrderInstruction> workOrderInstructions) {
        this.workOrderInstructions = workOrderInstructions;
    }

    @Override
    public Set<DBBasedWorkOrderByProduct> getWorkOrderByProduct() {
        return workOrderByProduct;
    }

    public void setWorkOrderByProduct(Set<DBBasedWorkOrderByProduct> workOrderByProduct) {
        this.workOrderByProduct = workOrderByProduct;
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

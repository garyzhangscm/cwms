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

package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


public class BillOfMaterial implements Serializable {


    private String number;
    private String description;

    private Long itemId;
    private Long warehouseId;


    private Long expectedQuantity;


    private Set<BillOfMaterialLine> billOfMaterialLines = new HashSet<>();

    private Set<BillOfMaterialByProduct> billOfMaterialByProducts = new HashSet<>();

    private Set<WorkOrderInstructionTemplate> workOrderInstructionTemplates = new HashSet<>();

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<BillOfMaterialLine> getBillOfMaterialLines() {
        return billOfMaterialLines;
    }

    public void setBillOfMaterialLines(Set<BillOfMaterialLine> billOfMaterialLines) {
        this.billOfMaterialLines = billOfMaterialLines;
    }

    public Set<BillOfMaterialByProduct> getBillOfMaterialByProducts() {
        return billOfMaterialByProducts;
    }

    public void setBillOfMaterialByProducts(Set<BillOfMaterialByProduct> billOfMaterialByProducts) {
        this.billOfMaterialByProducts = billOfMaterialByProducts;
    }

    public Set<WorkOrderInstructionTemplate> getWorkOrderInstructionTemplates() {
        return workOrderInstructionTemplates;
    }

    public void setWorkOrderInstructionTemplates(Set<WorkOrderInstructionTemplate> workOrderInstructionTemplates) {
        this.workOrderInstructionTemplates = workOrderInstructionTemplates;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }
}

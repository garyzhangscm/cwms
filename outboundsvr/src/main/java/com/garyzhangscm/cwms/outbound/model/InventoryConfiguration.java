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

package com.garyzhangscm.cwms.outbound.model;


import java.io.Serializable;

public class InventoryConfiguration   implements Serializable {


    private Long id;

    private Long companyId;
    private Long warehouseId;

    private String lpnValidationRule;

    private Boolean newItemAutoGenerateDefaultPackageType = true;

    private String newItemDefaultPackageTypeName;
    private String newItemDefaultPackageTypeDescription;

    private String inventoryAttribute1DisplayName;
    private Boolean inventoryAttribute1Enabled;
    private String inventoryAttribute2DisplayName;
    private Boolean inventoryAttribute2Enabled;
    private String inventoryAttribute3DisplayName;
    private Boolean inventoryAttribute3Enabled;
    private String inventoryAttribute4DisplayName;
    private Boolean inventoryAttribute4Enabled;
    private String inventoryAttribute5DisplayName;
    private Boolean inventoryAttribute5Enabled;


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


    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }


    public String getLpnValidationRule() {
        return lpnValidationRule;
    }

    public void setLpnValidationRule(String lpnValidationRule) {
        this.lpnValidationRule = lpnValidationRule;
    }

    public Boolean getNewItemAutoGenerateDefaultPackageType() {
        return newItemAutoGenerateDefaultPackageType;
    }

    public void setNewItemAutoGenerateDefaultPackageType(Boolean newItemAutoGenerateDefaultPackageType) {
        this.newItemAutoGenerateDefaultPackageType = newItemAutoGenerateDefaultPackageType;
    }

    public String getNewItemDefaultPackageTypeName() {
        return newItemDefaultPackageTypeName;
    }

    public void setNewItemDefaultPackageTypeName(String newItemDefaultPackageTypeName) {
        this.newItemDefaultPackageTypeName = newItemDefaultPackageTypeName;
    }

    public String getNewItemDefaultPackageTypeDescription() {
        return newItemDefaultPackageTypeDescription;
    }

    public void setNewItemDefaultPackageTypeDescription(String newItemDefaultPackageTypeDescription) {
        this.newItemDefaultPackageTypeDescription = newItemDefaultPackageTypeDescription;
    }

    public String getInventoryAttribute1DisplayName() {
        return inventoryAttribute1DisplayName;
    }

    public void setInventoryAttribute1DisplayName(String inventoryAttribute1DisplayName) {
        this.inventoryAttribute1DisplayName = inventoryAttribute1DisplayName;
    }

    public String getInventoryAttribute2DisplayName() {
        return inventoryAttribute2DisplayName;
    }

    public void setInventoryAttribute2DisplayName(String inventoryAttribute2DisplayName) {
        this.inventoryAttribute2DisplayName = inventoryAttribute2DisplayName;
    }

    public String getInventoryAttribute3DisplayName() {
        return inventoryAttribute3DisplayName;
    }

    public void setInventoryAttribute3DisplayName(String inventoryAttribute3DisplayName) {
        this.inventoryAttribute3DisplayName = inventoryAttribute3DisplayName;
    }

    public String getInventoryAttribute4DisplayName() {
        return inventoryAttribute4DisplayName;
    }

    public void setInventoryAttribute4DisplayName(String inventoryAttribute4DisplayName) {
        this.inventoryAttribute4DisplayName = inventoryAttribute4DisplayName;
    }

    public String getInventoryAttribute5DisplayName() {
        return inventoryAttribute5DisplayName;
    }

    public void setInventoryAttribute5DisplayName(String inventoryAttribute5DisplayName) {
        this.inventoryAttribute5DisplayName = inventoryAttribute5DisplayName;
    }

    public Boolean getInventoryAttribute1Enabled() {
        return inventoryAttribute1Enabled;
    }

    public void setInventoryAttribute1Enabled(Boolean inventoryAttribute1Enabled) {
        this.inventoryAttribute1Enabled = inventoryAttribute1Enabled;
    }

    public Boolean getInventoryAttribute2Enabled() {
        return inventoryAttribute2Enabled;
    }

    public void setInventoryAttribute2Enabled(Boolean inventoryAttribute2Enabled) {
        this.inventoryAttribute2Enabled = inventoryAttribute2Enabled;
    }

    public Boolean getInventoryAttribute3Enabled() {
        return inventoryAttribute3Enabled;
    }

    public void setInventoryAttribute3Enabled(Boolean inventoryAttribute3Enabled) {
        this.inventoryAttribute3Enabled = inventoryAttribute3Enabled;
    }

    public Boolean getInventoryAttribute4Enabled() {
        return inventoryAttribute4Enabled;
    }

    public void setInventoryAttribute4Enabled(Boolean inventoryAttribute4Enabled) {
        this.inventoryAttribute4Enabled = inventoryAttribute4Enabled;
    }

    public Boolean getInventoryAttribute5Enabled() {
        return inventoryAttribute5Enabled;
    }

    public void setInventoryAttribute5Enabled(Boolean inventoryAttribute5Enabled) {
        this.inventoryAttribute5Enabled = inventoryAttribute5Enabled;
    }
}
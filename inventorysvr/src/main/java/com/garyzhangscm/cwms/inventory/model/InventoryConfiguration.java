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

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "inventory_configuration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryConfiguration extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_configuration_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Company company;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "lpn_validation_rule")
    private String lpnValidationRule;

    @Column(name = "new_item_auto_generate_default_package_type")
    private Boolean newItemAutoGenerateDefaultPackageType = true;

    @Column(name = "new_item_default_package_type_name")
    private String newItemDefaultPackageTypeName;
    @Column(name = "new_item_default_package_type_description")
    private String newItemDefaultPackageTypeDescription;


    @OneToMany(
            mappedBy = "inventoryConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<ItemDefaultPackageUOM> itemDefaultPackageUOMS= new ArrayList<>();


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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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

    public List<ItemDefaultPackageUOM> getItemDefaultPackageUOMS() {
        return itemDefaultPackageUOMS;
    }

    public void setItemDefaultPackageUOMS(List<ItemDefaultPackageUOM> itemDefaultPackageUOMS) {
        this.itemDefaultPackageUOMS = itemDefaultPackageUOMS;
    }
}

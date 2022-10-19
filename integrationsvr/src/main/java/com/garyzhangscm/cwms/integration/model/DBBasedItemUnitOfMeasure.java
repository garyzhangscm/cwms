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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.service.DBBasedItemUnitOfMeasureIntegration;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_item_unit_of_measure")
public class DBBasedItemUnitOfMeasure extends AuditibleEntity<String> implements Serializable, IntegrationItemUnitOfMeasureData {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemUnitOfMeasure.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_item_unit_of_measure_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name")
    private String itemName;


    @Column(name = "item_package_type_id")
    private Long itemPackageTypeId;

    @Column(name = "item_package_type_name")
    private String itemPackageTypeName;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Column(name = "unit_of_measure_name")
    private String unitOfMeasureName;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_item_package_type_id")
    private DBBasedItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "length")
    private Double length;
    @Column(name = "width")
    private Double width;
    @Column(name = "height")
    private Double height;


    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public ItemUnitOfMeasure convertToItemUnitOfMeasure(
            InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
            CommonServiceRestemplateClient commonServiceRestemplateClient,
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient
    ) throws UnsupportedEncodingException {
        return convertToItemUnitOfMeasure(
                inventoryServiceRestemplateClient,
                commonServiceRestemplateClient,
                warehouseLayoutServiceRestemplateClient,
                false
        );
    }

    public ItemUnitOfMeasure convertToItemUnitOfMeasure(
            InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
            CommonServiceRestemplateClient commonServiceRestemplateClient,
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
            boolean attachedToItemPackageTypeTransaction
    ) throws UnsupportedEncodingException {

        // company ID or company code is required
        if (Objects.isNull(companyId) && Strings.isBlank(companyCode)) {

            throw MissingInformationException.raiseException("company information is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            setCompanyId(
                    warehouseLayoutServiceRestemplateClient
                            .getCompanyByCode(companyCode).getId()
            );
        }


        ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure();

        String[] fieldNames = {
                "itemId","itemName",
                "itemPackageTypeId", "itemPackageTypeName",
                "unitOfMeasureId", "unitOfMeasureName",
                "quantity","weight",
                "length","width","height",
                "warehouseId","warehouseName",
                "companyId","companyCode"
        };

        ObjectCopyUtil.copyValue( this, itemUnitOfMeasure,  fieldNames);

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        itemUnitOfMeasure.setWarehouseId(warehouseId);

        if (!attachedToItemPackageTypeTransaction) {
            logger.debug("Will get item by company id {}, warehouse id {}, item name: {}",
                    companyId, warehouseId, getItemName());
            if (Objects.isNull(getItemId()) && Objects.nonNull(getItemName())) {
                itemUnitOfMeasure.setItemId(
                        inventoryServiceRestemplateClient.getItemByName(
                                companyId,
                                warehouseId,
                                getItemName()).getId()
                );
            }


            logger.debug("Will get item package type by id by company id {}, warehouse id {}, item id: {}, package type name {}",
                    companyId, warehouseId, getItemId(), getItemPackageTypeName());

            if (Objects.isNull(getItemPackageTypeId()) && Objects.nonNull(getItemPackageTypeName())) {
                itemUnitOfMeasure.setItemPackageTypeId(
                        inventoryServiceRestemplateClient.getItemPackageTypeByName(
                                companyId,
                                warehouseId,
                                getItemId(),
                                getItemPackageTypeName()).getId()
                );
            }
        }
        if (Objects.isNull(getUnitOfMeasureId()) && Objects.nonNull(getUnitOfMeasureName())) {
            itemUnitOfMeasure.setUnitOfMeasureId(
                    commonServiceRestemplateClient.getUnitOfMeasureByName(
                            companyId, warehouseId, getUnitOfMeasureName()
                    ).getId()
            );
        }


        return itemUnitOfMeasure;

    }

    public DBBasedItemUnitOfMeasure() {}

    public DBBasedItemUnitOfMeasure(ItemUnitOfMeasure itemUnitOfMeasure) {

        String[] fieldNames = {
                "itemId","itemName",
                "itemPackageTypeId", "itemPackageTypeName",
                "unitOfMeasureId", "unitOfMeasureName",
                "quantity","weight",
                "length","width","height",
                "warehouseId","warehouseName"
        };

        ObjectCopyUtil.copyValue(  itemUnitOfMeasure, this,  fieldNames);

        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(LocalDateTime.now());

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

    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);
        setLastModifiedTime(LocalDateTime.now());
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }


    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public DBBasedItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(DBBasedItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getUnitOfMeasureName() {
        return unitOfMeasureName;
    }

    public void setUnitOfMeasureName(String unitOfMeasureName) {
        this.unitOfMeasureName = unitOfMeasureName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
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

    public Long getItemPackageTypeId() {
        return itemPackageTypeId;
    }

    public void setItemPackageTypeId(Long itemPackageTypeId) {
        this.itemPackageTypeId = itemPackageTypeId;
    }

    public String getItemPackageTypeName() {
        return itemPackageTypeName;
    }

    public void setItemPackageTypeName(String itemPackageTypeName) {
        this.itemPackageTypeName = itemPackageTypeName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getItemPackageTypeReference() {
        return Objects.isNull(itemPackageType) ? "N/A" :
                Objects.isNull(itemPackageType.getName()) ? "NULL/NAME" : itemPackageType.getName();
    }
}

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
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_item_package_type")
public class DBBasedItemPackageType extends AuditibleEntity<String> implements Serializable, IntegrationItemPackageTypeData {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemPackageType.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_item_package_type_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name")
    private String itemName;


    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;


    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name")
    private String supplierName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_item_id")
    private DBBasedItem item;


    @OneToMany(
            mappedBy = "itemPackageType",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DBBasedItemUnitOfMeasure> itemUnitOfMeasures= new ArrayList<>();

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

    public ItemPackageType convertToItemPackageType(
            InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
            CommonServiceRestemplateClient commonServiceRestemplateClient,
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {
        return convertToItemPackageType(
                inventoryServiceRestemplateClient,
                commonServiceRestemplateClient,
                warehouseLayoutServiceRestemplateClient,
                false
        );
    }

    /**
     * Read data from the integration table and convert to the item package type object
     * so we can send it to the right service for process
     * @param inventoryServiceRestemplateClient
     * @param commonServiceRestemplateClient
     * @param warehouseLayoutServiceRestemplateClient
     * @param attachedToItemTransaction whether this is a stand-alone transaction, or a transaction attached to the item
     *                                  transaction. If this is a stand-alone transaction, then we need to make sure the
     *                                  item is already exists, otherwise, we allow the item to be a new item as we know
     *                                  the attached item transaction may create the new item
     * @return
     */
    public ItemPackageType convertToItemPackageType(
            InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
            CommonServiceRestemplateClient commonServiceRestemplateClient,
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
            boolean attachedToItemTransaction)   {

        ItemPackageType itemPackageType = new ItemPackageType();


        String[] fieldNames = {
                "itemId","itemName",
                "name","description",
                "clientId","clientName",
                "supplierId","supplierName",
                "warehouseId","warehouseName",
                "companyId","companyCode"
        };

        ObjectCopyUtil.copyValue(this, itemPackageType,  fieldNames);


        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        itemPackageType.setWarehouseId(warehouseId);

        if (!attachedToItemTransaction) {
            // ok this is a standalone transaction to create / modify the item package type
            // we will need to make sure the item already exists
            if (Objects.isNull(getItemId()) && Objects.nonNull(getItemName())) {
                itemPackageType.setItemId(
                        inventoryServiceRestemplateClient.getItemByName(warehouseId,
                                getItemName()).getId()
                );
            }

        }

        if (Objects.isNull(getClientId()) && Objects.nonNull(getClientName())) {
            itemPackageType.setClientId(
                    commonServiceRestemplateClient.getClientByName(
                            warehouseId, getClientName()
                    ).getId()
            );
        }

        if (Objects.isNull(getSupplierId()) && Objects.nonNull(getSupplierName())) {
            itemPackageType.setSupplierId(
                    commonServiceRestemplateClient.getSupplierByName(
                            warehouseId, getSupplierName()
                    ).getId()
            );
        }

        getItemUnitOfMeasures().forEach(dbBasedItemUnitOfMeasure -> {
            itemPackageType.addItemUnitOfMeasure(dbBasedItemUnitOfMeasure.convertToItemUnitOfMeasure(
                    inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient,
                    true
            ));
        });

        return itemPackageType;
    }


    public String getItemReference() {
        return Objects.isNull(item) ? "N/A" :
                Objects.isNull(item.getName()) ? "NULL/NAME" : item.getName();
    }

    public DBBasedItemPackageType() {}
    public DBBasedItemPackageType(ItemPackageType itemPackageType) {


        String[] fieldNames = {
                "itemId","itemName",
                "name","description",
                "clientId","clientName",
                "supplierId","supplierName",
                "warehouseId","warehouseName"
        };

        ObjectCopyUtil.copyValue(itemPackageType, this,   fieldNames);

        itemPackageType.getItemUnitOfMeasures().forEach(itemUnitOfMeasure -> {
            DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure =
                    new DBBasedItemUnitOfMeasure(itemUnitOfMeasure);

            // logger.debug("Get DBBasedItemUnitOfMeasure \n {} \n from itemUnitOfMeasure: \n {}",
            //        dbBasedItemUnitOfMeasure, itemUnitOfMeasure);

            dbBasedItemUnitOfMeasure.setItemPackageType(this);
            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.ATTACHED);
            addItemUnitOfMeasure(dbBasedItemUnitOfMeasure);
        });


        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(LocalDateTime.now());

    }


    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);
        setLastModifiedTime(LocalDateTime.now());
        itemUnitOfMeasures.forEach(dbBasedItemUnitOfMeasure -> dbBasedItemUnitOfMeasure.completeIntegration(integrationStatus, errorMessage));
    }
    public void addItemUnitOfMeasure(DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure) {
        getItemUnitOfMeasures().add(dbBasedItemUnitOfMeasure);
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }


    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }


    public DBBasedItem getItem() {
        return item;
    }

    public void setItem(DBBasedItem item) {
        this.item = item;
    }

    public List<DBBasedItemUnitOfMeasure> getItemUnitOfMeasures() {
        return itemUnitOfMeasures;
    }

    public void setItemUnitOfMeasures(List<DBBasedItemUnitOfMeasure> itemUnitOfMeasures) {
        this.itemUnitOfMeasures = itemUnitOfMeasures;
    }


    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    @Override
    public LocalDateTime getInsertTime() {
        return getCreatedTime();
    }

    @Override
    public LocalDateTime getLastUpdateTime() {
        return getLastModifiedTime();
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

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
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.service.DBBasedSupplierIntegration;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_item")
public class DBBasedItem extends AuditibleEntity<String> implements Serializable, IntegrationItemData {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_item_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    @OneToOne(
            cascade = CascadeType.ALL
    )
    @JoinColumn(name="integration_item_family_id")
    private DBBasedItemFamily itemFamily;


    @Column(name = "item_family_name")
    private String itemFamilyName;
    @Column(name = "item_family_id")
    private Long itemFamilyId;

    @OneToMany(
        mappedBy = "item",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<DBBasedItemPackageType> itemPackageTypes= new ArrayList<>();

    @Column(name="unit_cost")
    private Double unitCost;

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

    public Item convertToItem(InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
                              CommonServiceRestemplateClient commonServiceRestemplateClient,
                              WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {

        Item item = new Item();


        String[] fieldNames = {
                "name","description",
                "clientId","clientName",
                "unitCost",
                "warehouseId","warehouseName",
                "companyId","companyCode"
        };

        ObjectCopyUtil.copyValue(this, item,  fieldNames);

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        item.setWarehouseId(warehouseId);


        if (Objects.isNull(getClientId()) && Objects.nonNull(getClientName())) {
            item.setClientId(
                    commonServiceRestemplateClient.getClientByName(
                            warehouseId, getClientName()
                    ).getId()
            );
        }


        // if item family data is passed in , then we are probably
        // creating a new item family along with the item,
        // if not, but item family name / id is passed in, then we are
        // assigning an existing item family to this item
        if (Objects.nonNull(getItemFamily())) {
            item.setItemFamily(getItemFamily().convertToItemFamily(warehouseLayoutServiceRestemplateClient));
        }
        else {
            if (Objects.nonNull(getItemFamilyId())) {
                item.setItemFamily(
                        inventoryServiceRestemplateClient.getItemFamilyById(
                                getItemFamilyId()
                        )
                );
                logger.debug("The item's family is setup to {} per id {}",
                        item.getItemFamily().getName(), getItemFamilyId());
            }
            else if (Strings.isNotBlank(getItemFamilyName())) {
                item.setItemFamily(
                        inventoryServiceRestemplateClient.getItemFamilyByName(
                                warehouseId, getItemFamilyName()
                        )
                );
                logger.debug("The item's family is setup to {} per name {}",
                        item.getItemFamily().getName(), getItemFamilyName());
            }
        }
        logger.debug("We have {} item package types for this item {}",
                getItemPackageTypes().size(), item.getName());
        getItemPackageTypes().forEach(dbBasedItemPackageType -> {
            item.addItemPackageType(dbBasedItemPackageType.convertToItemPackageType(
                    inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient,
                    true));
        });

        return item;

    }

    public DBBasedItem() {}
    public DBBasedItem(Item item) {

        String[] fieldNames = {
                "name","description","clientId","clientName","unitCost","warehouseId","warehouseName"
        };

        ObjectCopyUtil.copyValue(item, this, fieldNames);

        setItemFamily(new DBBasedItemFamily(item.getItemFamily()));
        getItemFamily().setStatus(IntegrationStatus.ATTACHED);

        item.getItemPackageTypes().forEach(itemPackageType -> {
            DBBasedItemPackageType dbBasedItemPackageType
                    = new DBBasedItemPackageType(itemPackageType);
            logger.debug("# Get DBBasedItemPackageType \n {} \n from itemPackageType: \n {}",
                   dbBasedItemPackageType, itemPackageType);
            dbBasedItemPackageType.setItem(this);
            dbBasedItemPackageType.setStatus(IntegrationStatus.ATTACHED);
            addItemPackageType(dbBasedItemPackageType);
        });

        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(LocalDateTime.now());

    }


    public void addItemPackageType(DBBasedItemPackageType dbBasedItemPackageType) {
        getItemPackageTypes().add(dbBasedItemPackageType);
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
        // Complete related integration
        if (Objects.nonNull(itemFamily)) {
            itemFamily.completeIntegration(integrationStatus, errorMessage);
        }
        getItemPackageTypes().forEach(dbBasedItemPackageType -> dbBasedItemPackageType.completeIntegration(integrationStatus, errorMessage));

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


    public DBBasedItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(DBBasedItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public List<DBBasedItemPackageType> getItemPackageTypes() {
        return itemPackageTypes;
    }

    public void setItemPackageTypes(List<DBBasedItemPackageType> itemPackageTypes) {
        this.itemPackageTypes = itemPackageTypes;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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



    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getItemFamilyName() {
        return itemFamilyName;
    }

    public void setItemFamilyName(String itemFamilyName) {
        this.itemFamilyName = itemFamilyName;
    }

    public Long getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(Long itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }
}

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;


@Entity
@Table(name = "integration_purchase_order_line")
public class DBBasedPurchaseOrderLine extends AuditibleEntity<String> implements Serializable, IntegrationPurchaseOrderLineData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_purchase_order_line_id")
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

    @Column(name = "item_quickbook_list_id")
    private String itemQuickbookListId;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;



    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_purchase_order_id")
    private DBBasedPurchaseOrder purchaseOrder;


    @Column(name = "quickbook_txnlineid")
    private String quickbookTxnLineID;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public DBBasedPurchaseOrderLine(){}


    public DBBasedPurchaseOrderLine(PurchaseOrderLine purchaseOrderLine) {

        setNumber(purchaseOrderLine.getNumber());

        setWarehouseId(purchaseOrderLine.getWarehouseId());
        setWarehouseName(purchaseOrderLine.getWarehouseName());
        setCompanyId(purchaseOrderLine.getCompanyId());
        setCompanyCode(purchaseOrderLine.getCompanyCode());

        setItemId(purchaseOrderLine.getItemId());
        setItemName(purchaseOrderLine.getItemName());
        setItemQuickbookListId(purchaseOrderLine.getItemQuickbookListId());

        setExpectedQuantity(purchaseOrderLine.getExpectedQuantity());
        setQuickbookTxnLineID(purchaseOrderLine.getQuickbookTxnLineID());


        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }


    public PurchaseOrderLine convertToPurchaseOrderLine(PurchaseOrder purchaseOrder,
                                        WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
                                        InventoryServiceRestemplateClient inventoryServiceRestemplateClient
    ) {

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

        PurchaseOrderLine purchaseOrderLine = new PurchaseOrderLine();

        String[] fieldNames = {
                "number", "itemId", "warehouseId", "expectedQuantity",
                "quickbookTxnLineID"
        };

        ObjectCopyUtil.copyValue(this, purchaseOrderLine, fieldNames);

        purchaseOrderLine.setWarehouseId(purchaseOrder.getWarehouseId());

        // in case item id is null, then we must have the item name or
        // quickbook item list id(if we integration with quickbook
        // so we can identify the unique item for this line
        if (Objects.isNull(getItemId())) {
            Item item = null;

            if (Strings.isNotBlank(getItemName())) {
                item = inventoryServiceRestemplateClient.getItemByName(
                        getCompanyId(),
                        purchaseOrderLine.getWarehouseId(), getItemName()
                );
            }
            else if (Strings.isNotBlank(getItemQuickbookListId())) {
                item = inventoryServiceRestemplateClient.getItemByQuickbookListId(
                        getCompanyId(),
                        purchaseOrderLine.getWarehouseId(), getItemQuickbookListId()
                );
            }
            else {
                throw MissingInformationException.raiseException("Either item id, or item name, or quickbook item list id " +
                        " needs to be present in order to identify the item for this order line");
            }
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item based on the order line's information");
            }
            purchaseOrderLine.setItemId(item.getId());
        }
        return purchaseOrderLine;
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
    public DBBasedPurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(DBBasedPurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public String getQuickbookTxnLineID() {
        return quickbookTxnLineID;
    }

    public void setQuickbookTxnLineID(String quickbookTxnLineID) {
        this.quickbookTxnLineID = quickbookTxnLineID;
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

    public String getItemQuickbookListId() {
        return itemQuickbookListId;
    }

    public void setItemQuickbookListId(String itemQuickbookListId) {
        this.itemQuickbookListId = itemQuickbookListId;
    }
}

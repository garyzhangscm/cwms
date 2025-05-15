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
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.controller.OrderIntegrationDataController;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_order_line")
public class DBBasedOrderLine extends AuditibleEntity<String> implements Serializable, IntegrationOrderLineData {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedOrderLine.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_order_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;

    @Column(name = "item_quickbook_list_id")
    private String itemQuickbookListId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;



    // Specific the inventory status that
    // user ordered. For example, when return
    // to vendor, we may return DAMAGED inventory
    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Column(name = "inventory_status_name")
    private String inventoryStatusName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_order_id")
    private DBBasedOrder order;



    @Column(name = "carrier_id")
    private Long carrierId;

    @Column(name = "carrier_name")
    private String carrierName;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Column(name = "carrier_service_level_name")
    private String carrierServiceLevelName;


    @Column(name = "quickbook_txnlineid")
    private String quickbookTxnLineID;

    @Column(name = "non_allocatable")
    private Boolean nonAllocatable;

    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

    // product id if shipped by hualei. This is
    // related to the carrier that ship the package
    @Column(name="hualei_product_id")
    private String hualeiProductId;

    @Column(name = "auto_request_shipping_label")
    private Boolean autoRequestShippingLabel;

    // only allocate inventory that received by certain receipt
    @Column(name = "allocate_by_receipt_number")
    private String allocateByReceiptNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    // when ship by parcel, whether we will need to
    // insure the package
    @Column(name = "parcel_insured")
    private Boolean parcelInsured;
    @Column(name = "parcel_insured_amount_per_unit")
    private Double parcelInsuredAmountPerUnit;
    // when ship by parcel, whether we will need to
    // insure the package
    @Column(name = "parcel_signature_required")
    private Boolean parcelSignatureRequired;

    public DBBasedOrderLine(){}


    public DBBasedOrderLine(OrderLine orderLine) {

        setNumber(orderLine.getNumber());

        setWarehouseId(orderLine.getWarehouseId());
        setWarehouseName(orderLine.getWarehouseName());
        setCompanyId(orderLine.getCompanyId());
        setCompanyCode(orderLine.getCompanyCode());

        setItemId(orderLine.getItemId());
        setItemName(orderLine.getItemName());
        setItemQuickbookListId(orderLine.getItemQuickbookListId());
        setNonAllocatable(orderLine.getNonAllocatable());

        setExpectedQuantity(orderLine.getExpectedQuantity());

        setInventoryStatusId(orderLine.getInventoryStatusId());
        setInventoryStatusName(orderLine.getInventoryStatusName());

        setCarrierId(orderLine.getCarrierId());
        setCarrierName(orderLine.getCarrierName());

        setCarrierServiceLevelId(orderLine.getCarrierServiceLevelId());
        setCarrierServiceLevelName(orderLine.getCarrierServiceLevelName());

        setQuickbookTxnLineID(orderLine.getQuickbookTxnLineID());
        setAllocateByReceiptNumber(orderLine.getAllocateByReceiptNumber());

        setHualeiProductId(orderLine.getHualeiProductId());
        setParcelInsured(orderLine.getParcelInsured());
        setParcelInsuredAmountPerUnit(orderLine.getParcelInsuredAmountPerUnit());
        setParcelSignatureRequired(orderLine.getParcelSignatureRequired());
        if (Objects.nonNull(orderLine.getAutoRequestShippingLabel())) {
            setAutoRequestShippingLabel(
                    orderLine.getAutoRequestShippingLabel()
            );
        }
        else {
            setAutoRequestShippingLabel(false);
        }
        setColor(orderLine.getColor());
        setProductSize(orderLine.getProductSize());
        setStyle(orderLine.getStyle());

        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    /*
    *  Convert db based order line into order line so we can process
    * the integration data by order service
    *
    * */
    public OrderLine convertToOrderLine(Order order,
                                        WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
                                        InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
                                        CommonServiceRestemplateClient commonServiceRestemplateClient
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


        OrderLine orderLine = new OrderLine();

        String[] fieldNames = {
                "number", "itemId",  "expectedQuantity",  "inventoryStatusId",
                "carrierId", "carrierServiceLevelId",
                "warehouseId","warehouseName", "quickbookTxnLineID","nonAllocatable",
                "hualeiProductId", "autoRequestShippingLabel",
                "color","productSize","style",
                "allocateByReceiptNumber",
                "parcelInsured", "parcelInsuredAmountPerUnit", "parcelSignatureRequired"

        };

        ObjectCopyUtil.copyValue(this, orderLine, fieldNames);

        orderLine.setWarehouseId(order.getWarehouseId());

        // in case item id is null, then we must have the item name or
        // quickbook item list id(if we integration with quickbook
        // so we can identify the unique item for this line
        if (Objects.isNull(getItemId())) {
            Item item = null;
            logger.debug("item id is not passed in for order {}, line {}, let's set it up",
                    order.getNumber(),
                    orderLine.getNumber());

            logger.debug("item name: {}", getItemName());
            logger.debug("item quickbook list id: {}", getItemQuickbookListId());

            if (Strings.isNotBlank(getItemQuickbookListId())) {
                item = inventoryServiceRestemplateClient.getItemByQuickbookListId(
                        getCompanyId(),
                        orderLine.getWarehouseId(), getItemQuickbookListId()
                );
            }
            else if (Strings.isNotBlank(getItemName())) {
                // see if the client is passed in
                Long clientId = null;
                if (Objects.nonNull(order.getClientId())) {
                    clientId = order.getClientId();
                }
                else if (Strings.isNotBlank(order.getClientName())) {
                    Client client = commonServiceRestemplateClient.getClientByName(
                            order.getWarehouseId(), order.getClientName()
                    );
                    if (Objects.nonNull(client)) {
                        clientId = client.getId();
                    }
                }
                item = inventoryServiceRestemplateClient.getItemByName(
                        getCompanyId(),
                        orderLine.getWarehouseId(),
                        clientId,
                        getItemName()
                );
            }
            else {
                throw MissingInformationException.raiseException("Either item id, or item name, or quickbook item list id " +
                        " needs to be present in order to identify the item for this order line");
            }
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item based on the order line's information");
            }
            orderLine.setItemId(item.getId());
            // if item is not inventory item, then set the non allocatable flag
            // to ture so the order line won't be allocated
            orderLine.setNonAllocatable(item.getNonInventoryItem());
        }
        else {
            Item item = inventoryServiceRestemplateClient.getItemById(getItemId());
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item based on the order line's information");
            }
            // if item is not inventory item, then set the non allocatable flag
            // to ture so the order line won't be allocated
            orderLine.setNonAllocatable(item.getNonInventoryItem());
        }

        if (Objects.isNull(getInventoryStatusId()) && Objects.nonNull(getInventoryStatusName())) {
            orderLine.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            orderLine.getWarehouseId(), getInventoryStatusName()
                    ).getId()
            );
        }

        return orderLine;
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
    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    @Override
    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    @Override
    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public DBBasedOrder getOrder() {
        return order;
    }

    public void setOrder(DBBasedOrder order) {
        this.order = order;
    }

    @Override
    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    @Override
    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    @Override
    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
    }

    @Override
    public String getItemQuickbookListId() {
        return itemQuickbookListId;
    }

    public void setItemQuickbookListId(String itemQuickbookListId) {
        this.itemQuickbookListId = itemQuickbookListId;
    }

    @Override
    public String getCarrierServiceLevelName() {
        return carrierServiceLevelName;
    }

    public void setCarrierServiceLevelName(String carrierServiceLevelName) {
        this.carrierServiceLevelName = carrierServiceLevelName;
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

    @Override
    public Boolean getNonAllocatable() {
        return nonAllocatable;
    }

    public void setNonAllocatable(Boolean nonAllocatable) {
        this.nonAllocatable = nonAllocatable;
    }

    public String getHualeiProductId() {
        return hualeiProductId;
    }

    public void setHualeiProductId(String hualeiProductId) {
        this.hualeiProductId = hualeiProductId;
    }

    public Boolean getAutoRequestShippingLabel() {
        return autoRequestShippingLabel;
    }

    public void setAutoRequestShippingLabel(Boolean autoRequestShippingLabel) {
        this.autoRequestShippingLabel = autoRequestShippingLabel;
    }

    public Boolean getParcelInsured() {
        return parcelInsured;
    }

    public void setParcelInsured(Boolean parcelInsured) {
        this.parcelInsured = parcelInsured;
    }

    public Double getParcelInsuredAmountPerUnit() {
        return parcelInsuredAmountPerUnit;
    }

    public void setParcelInsuredAmountPerUnit(Double parcelInsuredAmountPerUnit) {
        this.parcelInsuredAmountPerUnit = parcelInsuredAmountPerUnit;
    }

    public Boolean getParcelSignatureRequired() {
        return parcelSignatureRequired;
    }

    public void setParcelSignatureRequired(Boolean parcelSignatureRequired) {
        this.parcelSignatureRequired = parcelSignatureRequired;
    }
}

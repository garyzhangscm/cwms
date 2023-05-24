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
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_order")
public class DBBasedOrder extends AuditibleEntity<String> implements Serializable, IntegrationOrderData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_order_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "category")
    private String category;
    @Column(name = "transfer_receipt_warehouse_id")
    private Long transferReceiptWarehouseId;
    @Column(name = "transfer_receipt_warehouse_name")
    private String transferReceiptWarehouseName;

    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Column(name = "ship_to_customer_name")
    private String shipToCustomerName;


    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "bill_to_customer_id")
    private Long billToCustomerId;

    @Column(name = "bill_to_customer_name")
    private String billToCustomerName;

    // Ship to Address
    @Column(name = "ship_to_contactor_firstname")
    private String shipToContactorFirstname;
    @Column(name = "ship_to_contactor_lastname")
    private String shipToContactorLastname;
    @Column(name = "ship_to_contactor_phone_number")
    private String shipToContactorPhoneNumber;

    @Column(name = "ship_to_address_country")
    private String shipToAddressCountry;
    @Column(name = "ship_to_address_state")
    private String shipToAddressState;
    @Column(name = "ship_to_address_county")
    private String shipToAddressCounty;
    @Column(name = "ship_to_address_city")
    private String shipToAddressCity;
    @Column(name = "ship_to_address_district")
    private String shipToAddressDistrict;
    @Column(name = "ship_to_address_line1")
    private String shipToAddressLine1;
    @Column(name = "ship_to_address_line2")
    private String shipToAddressLine2;
    @Column(name = "ship_to_address_line3")
    private String shipToAddressLine3;
    @Column(name = "ship_to_address_postcode")
    private String shipToAddressPostcode;


    // Bill to Address
    @Column(name = "bill_to_contactor_firstname")
    private String billToContactorFirstname;
    @Column(name = "bill_to_contactor_lastname")
    private String billToContactorLastname;

    @Column(name = "bill_to_address_country")
    private String billToAddressCountry;
    @Column(name = "bill_to_address_state")
    private String billToAddressState;
    @Column(name = "bill_to_address_county")
    private String billToAddressCounty;
    @Column(name = "bill_to_address_city")
    private String billToAddressCity;
    @Column(name = "bill_to_address_district")
    private String billToAddressDistrict;
    @Column(name = "bill_to_address_line1")
    private String billToAddressLine1;
    @Column(name = "bill_to_address_line2")
    private String billToAddressLine2;
    @Column(name = "bill_to_address_line3")
    private String billToAddressLine3;
    @Column(name = "bill_to_address_postcode")
    private String billToAddressPostcode;

    @Column(name = "carrier_id")
    private Long carrierId;
    @Column(name = "carrier_name")
    private String carrierName;



    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;
    @Column(name = "carrier_service_level_name")
    private String carrierServiceLevelName;



    @Column(name = "client_id")
    private Long clientId;
    @Column(name = "client_name")
    private String clientName;


    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedOrderLine> orderLines = new ArrayList<>();

    // staging location group
    @Column(name="stage_location_group_id")
    private Long stageLocationGroupId;

    @Column(name="stage_location_group_name")
    private String stageLocationGroupName;

    @Column(name = "quickbook_customer_list_id")
    private String quickbookCustomerListId;

    @Column(name = "quickbook_txnid")
    private String quickbookTxnID;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public DBBasedOrder(){}


    public DBBasedOrder(Order order) {


        String[] fieldNames = {
                "number", "shipToCustomerId", "shipToCustomerName", "warehouseId", "warehouseName",
                "companyId", "companyCode",
                "billToCustomerId", "billToCustomerName", "shipToContactorFirstname", "shipToContactorLastname",
                "shipToAddressCountry", "shipToAddressState", "shipToAddressCounty", "shipToAddressCity", "shipToAddressDistrict",
                "shipToAddressLine1", "shipToAddressLine2", "shipToAddressPostcode", "billToContactorFirstname",
                "billToContactorLastname", "billToAddressCountry", "billToAddressState", "billToAddressCounty",
                "billToAddressCity", "billToAddressDistrict", "billToAddressLine1", "billToAddressLine2", "billToAddressPostcode",
                "carrierId", "carrierName", "carrierServiceLevelId","carrierServiceLevelName", "clientId", "clientName",
                "category", "transferReceiptWarehouseId", "quickbookTxnID", "quickbookCustomerListId",
                "shipToAddressLine3", "billToAddressLine3",
                "shipToContactorPhoneNumber"
        };

        ObjectCopyUtil.copyValue(order, this, fieldNames);


        order.getOrderLines().forEach(orderLine -> {

            DBBasedOrderLine dbBasedOrderLine
                    = new DBBasedOrderLine(orderLine);

            dbBasedOrderLine.setOrder(this);
            dbBasedOrderLine.setStatus(IntegrationStatus.ATTACHED);
            addOrderLine(dbBasedOrderLine);
        });

        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));

    }

    public Order convertToOrder(
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
            InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
            CommonServiceRestemplateClient commonServiceRestemplateClient) {
        Order order = new Order();

        String[] fieldNames = {
        "number", "shipToCustomerId", "billToCustomerId", "shipToContactorFirstname", "shipToContactorLastname",
                "shipToAddressCountry", "shipToAddressState", "shipToAddressCounty", "shipToAddressCity", "shipToAddressDistrict",
                "shipToAddressLine1", "shipToAddressLine2", "shipToAddressPostcode", "billToContactorFirstname",
                "billToContactorLastname", "billToAddressCountry", "billToAddressState", "billToAddressCounty",
                "billToAddressCity", "billToAddressDistrict", "billToAddressLine1", "billToAddressLine2",
                "billToAddressPostcode", "carrierId", "carrierServiceLevelId", "clientId",
                "category", "transferReceiptWarehouseId", "transferReceiptWarehouseName",
                "warehouseId","warehouseName", "quickbookTxnID", "quickbookCustomerListId",
                "shipToAddressLine3", "billToAddressLine3", "shipToContactorPhoneNumber"
        };

        ObjectCopyUtil.copyValue(this, order, fieldNames);

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        order.setWarehouseId(warehouseId);

        // Copy each order line as well
        getOrderLines().forEach(dbBasedOrderLine -> {
            OrderLine orderLine = dbBasedOrderLine.convertToOrderLine(order, warehouseLayoutServiceRestemplateClient,
                    inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient);
            order.getOrderLines().add(orderLine);
        });

        return order;
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

    @Override
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public String getShipToCustomerName() {
        return shipToCustomerName;
    }

    public void setShipToCustomerName(String shipToCustomerName) {
        this.shipToCustomerName = shipToCustomerName;
    }

    @Override
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    @Override
    public Long getBillToCustomerId() {
        return billToCustomerId;
    }

    public void setBillToCustomerId(Long billToCustomerId) {
        this.billToCustomerId = billToCustomerId;
    }

    public String getBillToCustomerName() {
        return billToCustomerName;
    }

    public void setBillToCustomerName(String billToCustomerName) {
        this.billToCustomerName = billToCustomerName;
    }

    @Override
    public String getShipToContactorFirstname() {
        return shipToContactorFirstname;
    }

    public void setShipToContactorFirstname(String shipToContactorFirstname) {
        this.shipToContactorFirstname = shipToContactorFirstname;
    }

    @Override
    public String getShipToContactorLastname() {
        return shipToContactorLastname;
    }

    public void setShipToContactorLastname(String shipToContactorLastname) {
        this.shipToContactorLastname = shipToContactorLastname;
    }

    @Override
    public String getShipToAddressCountry() {
        return shipToAddressCountry;
    }

    public void setShipToAddressCountry(String shipToAddressCountry) {
        this.shipToAddressCountry = shipToAddressCountry;
    }

    @Override
    public String getShipToAddressState() {
        return shipToAddressState;
    }

    public void setShipToAddressState(String shipToAddressState) {
        this.shipToAddressState = shipToAddressState;
    }

    @Override
    public String getShipToAddressCounty() {
        return shipToAddressCounty;
    }

    public void setShipToAddressCounty(String shipToAddressCounty) {
        this.shipToAddressCounty = shipToAddressCounty;
    }

    @Override
    public String getShipToAddressCity() {
        return shipToAddressCity;
    }

    public void setShipToAddressCity(String shipToAddressCity) {
        this.shipToAddressCity = shipToAddressCity;
    }

    @Override
    public String getShipToAddressDistrict() {
        return shipToAddressDistrict;
    }

    public void setShipToAddressDistrict(String shipToAddressDistrict) {
        this.shipToAddressDistrict = shipToAddressDistrict;
    }

    @Override
    public String getShipToAddressLine1() {
        return shipToAddressLine1;
    }

    public void setShipToAddressLine1(String shipToAddressLine1) {
        this.shipToAddressLine1 = shipToAddressLine1;
    }

    @Override
    public String getShipToAddressLine2() {
        return shipToAddressLine2;
    }

    public void setShipToAddressLine2(String shipToAddressLine2) {
        this.shipToAddressLine2 = shipToAddressLine2;
    }

    @Override
    public String getShipToAddressPostcode() {
        return shipToAddressPostcode;
    }

    public void setShipToAddressPostcode(String shipToAddressPostcode) {
        this.shipToAddressPostcode = shipToAddressPostcode;
    }

    @Override
    public String getBillToContactorFirstname() {
        return billToContactorFirstname;
    }

    public void setBillToContactorFirstname(String billToContactorFirstname) {
        this.billToContactorFirstname = billToContactorFirstname;
    }

    @Override
    public String getBillToContactorLastname() {
        return billToContactorLastname;
    }

    public void setBillToContactorLastname(String billToContactorLastname) {
        this.billToContactorLastname = billToContactorLastname;
    }

    @Override
    public String getBillToAddressCountry() {
        return billToAddressCountry;
    }

    public void setBillToAddressCountry(String billToAddressCountry) {
        this.billToAddressCountry = billToAddressCountry;
    }

    @Override
    public String getBillToAddressState() {
        return billToAddressState;
    }

    public void setBillToAddressState(String billToAddressState) {
        this.billToAddressState = billToAddressState;
    }

    @Override
    public String getBillToAddressCounty() {
        return billToAddressCounty;
    }

    public void setBillToAddressCounty(String billToAddressCounty) {
        this.billToAddressCounty = billToAddressCounty;
    }

    @Override
    public String getBillToAddressCity() {
        return billToAddressCity;
    }

    public void setBillToAddressCity(String billToAddressCity) {
        this.billToAddressCity = billToAddressCity;
    }

    @Override
    public String getBillToAddressDistrict() {
        return billToAddressDistrict;
    }

    public void setBillToAddressDistrict(String billToAddressDistrict) {
        this.billToAddressDistrict = billToAddressDistrict;
    }

    @Override
    public String getBillToAddressLine1() {
        return billToAddressLine1;
    }

    public void setBillToAddressLine1(String billToAddressLine1) {
        this.billToAddressLine1 = billToAddressLine1;
    }

    @Override
    public String getBillToAddressLine2() {
        return billToAddressLine2;
    }

    public void setBillToAddressLine2(String billToAddressLine2) {
        this.billToAddressLine2 = billToAddressLine2;
    }

    @Override
    public String getBillToAddressPostcode() {
        return billToAddressPostcode;
    }

    public void setBillToAddressPostcode(String billToAddressPostcode) {
        this.billToAddressPostcode = billToAddressPostcode;
    }

    @Override
    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

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

    public String getCarrierServiceLevelName() {
        return carrierServiceLevelName;
    }

    public void setCarrierServiceLevelName(String carrierServiceLevelName) {
        this.carrierServiceLevelName = carrierServiceLevelName;
    }

    public String getShipToContactorPhoneNumber() {
        return shipToContactorPhoneNumber;
    }

    public void setShipToContactorPhoneNumber(String shipToContactorPhoneNumber) {
        this.shipToContactorPhoneNumber = shipToContactorPhoneNumber;
    }

    @Override
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public Long getStageLocationGroupId() {
        return stageLocationGroupId;
    }

    public void setStageLocationGroupId(Long stageLocationGroupId) {
        this.stageLocationGroupId = stageLocationGroupId;
    }

    public String getStageLocationGroupName() {
        return stageLocationGroupName;
    }

    public void setStageLocationGroupName(String stageLocationGroupName) {
        this.stageLocationGroupName = stageLocationGroupName;
    }

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
    }

    @Override
    public List<DBBasedOrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<DBBasedOrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public void addOrderLine(DBBasedOrderLine orderLine) {
        this.orderLines.add(orderLine);
    }

    @Override
    public String getShipToAddressLine3() {
        return shipToAddressLine3;
    }

    public void setShipToAddressLine3(String shipToAddressLine3) {
        this.shipToAddressLine3 = shipToAddressLine3;
    }

    @Override
    public String getBillToAddressLine3() {
        return billToAddressLine3;
    }

    public void setBillToAddressLine3(String billToAddressLine3) {
        this.billToAddressLine3 = billToAddressLine3;
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
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public Long getTransferReceiptWarehouseId() {
        return transferReceiptWarehouseId;
    }

    public void setTransferReceiptWarehouseId(Long transferReceiptWarehouseId) {
        this.transferReceiptWarehouseId = transferReceiptWarehouseId;
    }

    @Override
    public String getTransferReceiptWarehouseName() {
        return transferReceiptWarehouseName;
    }

    public void setTransferReceiptWarehouseName(String transferReceiptWarehouseName) {
        this.transferReceiptWarehouseName = transferReceiptWarehouseName;
    }

    public String getQuickbookCustomerListId() {
        return quickbookCustomerListId;
    }

    public void setQuickbookCustomerListId(String quickbookCustomerListId) {
        this.quickbookCustomerListId = quickbookCustomerListId;
    }
}

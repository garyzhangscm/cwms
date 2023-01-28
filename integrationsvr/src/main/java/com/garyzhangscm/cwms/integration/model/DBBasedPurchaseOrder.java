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
@Table(name = "integration_purchase_order")
public class DBBasedPurchaseOrder extends AuditibleEntity<String> implements Serializable, IntegrationPurchaseOrderData{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_purchase_order_id")
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

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name")
    private String supplierName;

    @OneToMany(
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DBBasedPurchaseOrderLine> purchaseOrderLines = new ArrayList<>();


    @Column(name = "allow_unexpected_item")
    private Boolean allowUnexpectedItem;


    @Column(name = "quickbook_txnid")
    private String quickbookTxnID;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public PurchaseOrder convertToPurchaseOrder(
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();

        String[] fieldNames = {
                "number", "warehouseId", "clientId", "supplierId", "allowUnexpectedItem",
                "quickbookTxnID"
        };

        ObjectCopyUtil.copyValue(this, purchaseOrder, fieldNames);

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        purchaseOrder.setWarehouseId(warehouseId);
        purchaseOrder.setWarehouse(
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId)
        );

        // Copy each order line as well
        getPurchaseOrderLines().forEach(dbBasedPurchaseOrderLine -> {
            PurchaseOrderLine purchaseOrderLine = new PurchaseOrderLine();
            String[] purchaseOrderLineFieldNames = {
                    "number", "itemId", "warehouseId", "expectedQuantity"
            };
            ObjectCopyUtil.copyValue(dbBasedPurchaseOrderLine, purchaseOrderLine, purchaseOrderLineFieldNames);
            purchaseOrder.getPurchaseOrderLines().add(purchaseOrderLine);
        });

        return purchaseOrder;
    }

    public DBBasedPurchaseOrder(){}


    public DBBasedPurchaseOrder(PurchaseOrder purchaseOrder) {

        setNumber(purchaseOrder.getNumber());
        setWarehouseId(purchaseOrder.getWarehouseId());
        setWarehouseName(purchaseOrder.getWarehouseName());
        setCompanyId(purchaseOrder.getCompanyId());
        setCompanyCode(purchaseOrder.getCompanyCode());
        setQuickbookTxnID(purchaseOrder.getQuickbookTxnID());

        setClientId(purchaseOrder.getClientId());
        setClientName(purchaseOrder.getClientName());

        setSupplierId(purchaseOrder.getSupplierId());
        setSupplierName(purchaseOrder.getSupplierName());

        setAllowUnexpectedItem(purchaseOrder.getAllowUnexpectedItem());

        purchaseOrder.getPurchaseOrderLines().forEach(purchaseOrderLine -> {

            DBBasedPurchaseOrderLine dbBasedPurchaseOrderLine
                    = new DBBasedPurchaseOrderLine(purchaseOrderLine);

            dbBasedPurchaseOrderLine.setPurchaseOrder(this);
            dbBasedPurchaseOrderLine.setStatus(IntegrationStatus.ATTACHED);
            addPurchaseOrderLine(dbBasedPurchaseOrderLine);
        });

        setStatus(IntegrationStatus.PENDING);
        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
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
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    @Override
    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    @Override
    public List<DBBasedPurchaseOrderLine> getPurchaseOrderLines() {
        return purchaseOrderLines;
    }

    public void setPurchaseOrderLines(List<DBBasedPurchaseOrderLine> purchaseOrderLines) {
        this.purchaseOrderLines = purchaseOrderLines;
    }

    public void addPurchaseOrderLine(DBBasedPurchaseOrderLine purchaseOrderLine) {
        this.purchaseOrderLines.add(purchaseOrderLine);
    }

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
    }

    @Override
    public Boolean getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(Boolean allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }

    @Override
    public IntegrationStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

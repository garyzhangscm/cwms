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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_receipt")
public class DBBasedReceipt extends AuditibleEntity<String> implements Serializable, IntegrationReceiptData{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_receipt_id")
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
            mappedBy = "receipt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DBBasedReceiptLine> receiptLines = new ArrayList<>();


    @Column(name = "allow_unexpected_item")
    private Boolean allowUnexpectedItem;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public Receipt convertToReceipt(
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {
        Receipt receipt = new Receipt();

        String[] fieldNames = {
                "number", "warehouseId", "clientId", "supplierId", "allowUnexpectedItem"
        };

        ObjectCopyUtil.copyValue(this, receipt, fieldNames);

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
        }
        receipt.setWarehouseId(warehouseId);
        receipt.setWarehouse(
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId)
        );

        // Copy each order line as well
        getReceiptLines().forEach(dbBasedReceiptLine -> {
            ReceiptLine receiptLine = new ReceiptLine();
            String[] receiptLineFieldNames = {
                    "number", "itemId", "warehouseId", "expectedQuantity", "overReceivingQuantity",
                    "overReceivingPercent"
            };
            ObjectCopyUtil.copyValue(dbBasedReceiptLine, receiptLine, receiptLineFieldNames);
            receipt.getReceiptLines().add(receiptLine);
        });

        return receipt;
    }

    public DBBasedReceipt(){}


    public DBBasedReceipt(Receipt receipt) {

        setNumber(receipt.getNumber());
        setWarehouseId(receipt.getWarehouseId());
        setWarehouseName(receipt.getWarehouseName());

        setClientId(receipt.getClientId());
        setClientName(receipt.getClientName());

        setSupplierId(receipt.getSupplierId());
        setSupplierName(receipt.getSupplierName());

        setAllowUnexpectedItem(receipt.getAllowUnexpectedItem());

        receipt.getReceiptLines().forEach(receiptLine -> {

            DBBasedReceiptLine dbBasedReceiptLine
                    = new DBBasedReceiptLine(receiptLine);

            dbBasedReceiptLine.setReceipt(this);
            dbBasedReceiptLine.setStatus(IntegrationStatus.ATTACHED);
            addReceiptLine(dbBasedReceiptLine);
        });

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
    public List<DBBasedReceiptLine> getReceiptLines() {
        return receiptLines;
    }

    public void setReceiptLines(List<DBBasedReceiptLine> receiptLines) {
        this.receiptLines = receiptLines;
    }
    public void addReceiptLine(DBBasedReceiptLine receiptLine) {
        this.receiptLines.add(receiptLine);
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

    @Override
    public LocalDateTime getInsertTime() {
        return getCreatedTime();
    }

    @Override
    public LocalDateTime getLastUpdateTime() {
        return getLastModifiedTime();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

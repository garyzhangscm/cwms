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
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_receipt_confirmation")
public class DBBasedReceiptConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationReceiptConfirmationData{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_receipt_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;


    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

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
    private List<DBBasedReceiptLineConfirmation> receiptLines = new ArrayList<>();


    @Column(name = "allow_unexpected_item")
    private Boolean allowUnexpectedItem;

    @Column(name = "quickbook_txnid")
    private String quickbookTxnID;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;



    public DBBasedReceiptConfirmation(){}

    public DBBasedReceiptConfirmation(ReceiptConfirmation receiptConfirmation){

        setNumber(receiptConfirmation.getNumber());
        setQuickbookTxnID(receiptConfirmation.getQuickbookTxnID());

        setWarehouseId(receiptConfirmation.getWarehouseId());
        setWarehouseName(receiptConfirmation.getWarehouseName());

        setClientId(receiptConfirmation.getClientId());
        setClientName(receiptConfirmation.getClientName());

        setSupplierId(receiptConfirmation.getSupplierId());
        setSupplierName(receiptConfirmation.getSupplierName());

        receiptConfirmation.getReceiptLines().forEach(receiptLineConfirmation -> {
            DBBasedReceiptLineConfirmation dbBasedReceiptLineConfirmation =
                    new DBBasedReceiptLineConfirmation(receiptLineConfirmation);
            dbBasedReceiptLineConfirmation.setReceipt(this);
            dbBasedReceiptLineConfirmation.setStatus(IntegrationStatus.ATTACHED);
            addReceiptLine(dbBasedReceiptLineConfirmation);
        });

        setAllowUnexpectedItem(receiptConfirmation.getAllowUnexpectedItem());


        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        setStatus(IntegrationStatus.PENDING);

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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
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
    public List<DBBasedReceiptLineConfirmation> getReceiptLines() {
        return receiptLines;
    }

    public void setReceiptLines(List<DBBasedReceiptLineConfirmation> receiptLines) {
        this.receiptLines = receiptLines;
    }

    public void addReceiptLine(DBBasedReceiptLineConfirmation receiptLine) {
        if (Objects.isNull(receiptLines)) {
            receiptLines = new ArrayList<>();
        }
        receiptLines.add(receiptLine);
    }
    @Override
    public Boolean getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(Boolean allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
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
}

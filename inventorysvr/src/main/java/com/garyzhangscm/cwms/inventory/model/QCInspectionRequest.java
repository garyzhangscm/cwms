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


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qc_inspection_request")
public class QCInspectionRequest extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(QCInspectionRequest.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qc_inspection_request_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    // only if the qc is for inventory
    @ManyToOne
    @JoinColumn(name="inventory_id")
    private Inventory inventory;


    // only if the qc request is manually generated
    // and by item. So that the user can specify
    // which invenotories has been QCed during
    // the request
    @ManyToMany(cascade = {
            CascadeType.ALL
    })
    @JoinTable(name = "qc_inspection_request_inventory",
            joinColumns = @JoinColumn(name = "qc_inspection_request_id"),
            inverseJoinColumns = @JoinColumn(name = "inventory_id")
    )
    private List<Inventory> inventories = new ArrayList<>();

    // only if the qc is for work order
    @Column(name="work_order_qc_sample_id")
    private Long workOrderQCSampleId;
    @Transient
    private WorkOrderQCSample workOrderQCSample;
    // only if the qc is for work order sampling
    // or item by work order/receipt
    @Column(name="qc_quantity")
    private Long qcQuantity = 0l;


    // when qc by item
    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    // more filter when qc by item.
    // restrict to inventory of the item
    // that is received from work order or receipt
    @Column(name="work_order_id")
    private Long workOrderId;
    @Transient
    private WorkOrder workOrder;

    @Column(name="receipt_id")
    private Long receiptId;
    @Transient
    private Receipt receipt;


    @Column(name = "qc_inspection_result")
    @Enumerated(EnumType.STRING)
    private QCInspectionResult qcInspectionResult;

    @Column(name = "qc_inspection_request_type")
    @Enumerated(EnumType.STRING)
    private QCInspectionRequestType type;

    @Column(name = "qc_username")
    private String qcUsername;

    @Column(name = "qc_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime qcTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime qcTime;

    @Column(name = "qc_rf_code")
    private String rfCode;


    @OneToMany(
            mappedBy = "qcInspectionRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<QCInspectionRequestItem> qcInspectionRequestItems = new ArrayList<>();


    @Column(name = "document_urls")
    private String documentUrls;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public QCInspectionResult getQcInspectionResult() {
        return qcInspectionResult;
    }

    public void setQcInspectionResult(QCInspectionResult qcInspectionResult) {
        this.qcInspectionResult = qcInspectionResult;
    }

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }

    public String getQcUsername() {
        return qcUsername;
    }

    public void setQcUsername(String qcUsername) {
        this.qcUsername = qcUsername;
    }

    public ZonedDateTime getQcTime() {
        return qcTime;
    }

    public void setQcTime(ZonedDateTime qcTime) {
        this.qcTime = qcTime;
    }

    public List<QCInspectionRequestItem> getQcInspectionRequestItems() {
        return qcInspectionRequestItems;
    }

    public void setQcInspectionRequestItems(List<QCInspectionRequestItem> qcInspectionRequestItems) {
        this.qcInspectionRequestItems = qcInspectionRequestItems;
    }

    public void addQcInspectionRequestItem(QCInspectionRequestItem qcInspectionRequestItem) {
        this.qcInspectionRequestItems.add(qcInspectionRequestItem);
    }

    public Long getWorkOrderQCSampleId() {
        return workOrderQCSampleId;
    }

    public void setWorkOrderQCSampleId(Long workOrderQCSampleId) {
        this.workOrderQCSampleId = workOrderQCSampleId;
    }

    public WorkOrderQCSample getWorkOrderQCSample() {
        return workOrderQCSample;
    }

    public void setWorkOrderQCSample(WorkOrderQCSample workOrderQCSample) {
        this.workOrderQCSample = workOrderQCSample;
    }

    public Long getQcQuantity() {
        return qcQuantity;
    }

    public void setQcQuantity(Long qcQuantity) {
        this.qcQuantity = qcQuantity;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public QCInspectionRequestType getType() {
        return type;
    }

    public void setType(QCInspectionRequestType type) {
        this.type = type;
    }

    public List<Inventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<Inventory> inventories) {
        this.inventories = inventories;
    }

    public String getDocumentUrls() {
        return documentUrls;
    }

    public void setDocumentUrls(String documentUrls) {
        this.documentUrls = documentUrls;
    }
}

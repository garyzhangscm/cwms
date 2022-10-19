/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Table(name = "quickbook_webhook_history")
public class QuickBookWebhookHistory extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quickbook_webhook_history_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Company company;

    @Column(name = "realm_id")
    private String realmId;

    @Column(name = "signature")
    private String signature;

    @Column(name = "payload")
    private String payload;

    // entity name
    @Column(name = "entity_name")
    private String entityName;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processed_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime processedTime;

    public QuickBookWebhookHistory() {}

    public QuickBookWebhookHistory(String signature, String payload) {
        this(signature, payload, WebhookStatus.PENDING);
    }
    public QuickBookWebhookHistory(String signature, String payload, WebhookStatus status) {
        this(signature, payload, "", status, "");
    }
    public QuickBookWebhookHistory(String signature, String payload,
                                   String entityName,
                                   WebhookStatus status) {
        this(signature, payload, entityName, status, "");
    }
    public QuickBookWebhookHistory(String signature, String payload,
                                   WebhookStatus status,
                                   String errorMessage) {
        this.signature = signature;
        this.payload = payload;
        this.status = status;
        this.errorMessage = errorMessage;
        this.processedTime = LocalDateTime.now();
    }
    public QuickBookWebhookHistory(String signature, String payload, String entityName,
                                   WebhookStatus status,
                                   String errorMessage) {
        this.signature = signature;
        this.payload = payload;
        this.entityName = entityName;
        this.status = status;
        this.errorMessage = errorMessage;
    }
    public QuickBookWebhookHistory(Long companyId, Long warehouseId, String realmId,
                                   String signature, String payload, String entityName,
                                   WebhookStatus status,
                                   String errorMessage,
                                   LocalDateTime processedTime) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.realmId = realmId;
        this.signature = signature;
        this.payload = payload;
        this.entityName = entityName;
        this.status = status;
        this.errorMessage = errorMessage;
        this.processedTime = processedTime;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(LocalDateTime processedTime) {
        this.processedTime = processedTime;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}

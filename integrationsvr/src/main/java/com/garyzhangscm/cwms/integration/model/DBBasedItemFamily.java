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


import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_item_family")
public class DBBasedItemFamily implements Serializable, IntegrationItemFamilyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_item_family_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;
    @Column(name = "insert_time")
    private LocalDateTime insertTime;
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
    @Column(name = "error_message")
    private String errorMessage;

    public ItemFamily convertToItemFamily(
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {
        ItemFamily itemFamily = new ItemFamily();

        String[] fieldNames = {
                "name","description", "warehouseId","warehouseName"
        };

        ObjectCopyUtil.copyValue(this,itemFamily,  fieldNames);


        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    getWarehouseName()
            ).getId();
            itemFamily.setWarehouseId(warehouseId);
        }


        return itemFamily;
    }

    public DBBasedItemFamily() {}
    public DBBasedItemFamily(ItemFamily itemFamily) {

        String[] fieldNames = {
                "name","description", "warehouseId","warehouseName"
        };

        ObjectCopyUtil.copyValue(itemFamily,this,  fieldNames);

        setStatus(IntegrationStatus.PENDING);
        setInsertTime(LocalDateTime.now());
    }

    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);
        setLastUpdateTime(LocalDateTime.now());
    }

        @Override
    public String toString() {
        return "DBBasedItemFamily{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", warehouseId=" + warehouseId +
                ", warehouseName='" + warehouseName + '\'' +
                ", status=" + status +
                ", insertTime=" + insertTime +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
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

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(LocalDateTime insertTime) {
        this.insertTime = insertTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

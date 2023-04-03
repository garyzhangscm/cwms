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

package com.garyzhangscm.cwms.adminserver.model;


import com.garyzhangscm.cwms.adminserver.model.wms.Client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ClientInventoryAgingSnapshot extends AuditibleEntity<String> implements Serializable {

    private Long id;

    private Long warehouseId;

    private Long clientId;
    private Client client;

    private Long averageAgeInDays;

    private Long averageAgeInWeeks;


    private List<InventoryAgingByLPN> inventoryAgingByLPNS = new ArrayList<>();


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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getAverageAgeInDays() {
        return averageAgeInDays;
    }

    public void setAverageAgeInDays(Long averageAgeInDays) {
        this.averageAgeInDays = averageAgeInDays;
    }

    public Long getAverageAgeInWeeks() {
        return averageAgeInWeeks;
    }

    public void setAverageAgeInWeeks(Long averageAgeInWeeks) {
        this.averageAgeInWeeks = averageAgeInWeeks;
    }

    public List<InventoryAgingByLPN> getInventoryAgingByLPNS() {
        return inventoryAgingByLPNS;
    }

    public void setInventoryAgingByLPNS(List<InventoryAgingByLPN> inventoryAgingByLPNS) {
        this.inventoryAgingByLPNS = inventoryAgingByLPNS;
    }
}

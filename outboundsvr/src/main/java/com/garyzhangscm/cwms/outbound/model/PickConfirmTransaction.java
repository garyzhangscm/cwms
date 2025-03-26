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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "pick_confirm_transaction")
public class PickConfirmTransaction extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_confirm_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "pick_id")
    @JsonIgnore
    private Pick pick;


    @Column(name = "username")
    private String username;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private PickConfirmTransactionType type;


    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "lpn")
    private String lpn;
    public PickConfirmTransaction() {}

    public PickConfirmTransaction(Long companyId, Long warehouseId,
                                  String sessionId,
                                  Pick pick, String username, PickConfirmTransactionType type, Long quantity, String lpn) {

        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.sessionId = sessionId;
        this.pick = pick;
        this.username = username;
        this.type = type;
        this.quantity = quantity;
        this.lpn = lpn;
    }

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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PickConfirmTransactionType getType() {
        return type;
    }

    public void setType(PickConfirmTransactionType type) {
        this.type = type;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }
}

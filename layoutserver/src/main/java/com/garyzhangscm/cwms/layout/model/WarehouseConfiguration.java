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

package com.garyzhangscm.cwms.layout.model;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "warehouse_configuration")
public class WarehouseConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;


    // whether this warehouse is serving as
    // 3pl warehouse
    @Column(name = "three_party_logistics_flag")
    private Boolean threePartyLogisticsFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getThreePartyLogisticsFlag() {
        return threePartyLogisticsFlag;
    }

    public void setThreePartyLogisticsFlag(Boolean threePartyLogisticsFlag) {
        this.threePartyLogisticsFlag = threePartyLogisticsFlag;
    }

}

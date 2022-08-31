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

package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.Objects;

/**
 * Default configuration on RFs
 */
@Entity
@Table(name = "rf_configuration")
public class RFConfiguration extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rf_configuration_id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    // can be override by RF
    @Column(name = "rf_code")
    private String rfCode;

    // work order - manual pick
    @Column(name = "work_order_validate_partial_lpn_pick")
    private Boolean workOrderValidatePartialLPNPick;

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

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }

    public Boolean getWorkOrderValidatePartialLPNPick() {
        return workOrderValidatePartialLPNPick;
    }

    public void setWorkOrderValidatePartialLPNPick(Boolean workOrderValidatePartialLPNPick) {
        this.workOrderValidatePartialLPNPick = workOrderValidatePartialLPNPick;
    }
}

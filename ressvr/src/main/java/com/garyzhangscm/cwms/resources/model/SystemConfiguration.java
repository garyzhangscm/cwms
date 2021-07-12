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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "system_configuration")
public class SystemConfiguration extends AuditibleEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_configuration_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "allow_data_initial")
    private Boolean allowDataInitialFlag = false;

    @Column(name = "server_side_printing")
    private Boolean serverSidePrinting = true;

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

    public Boolean getAllowDataInitialFlag() {
        return allowDataInitialFlag;
    }

    public void setAllowDataInitialFlag(Boolean allowDataInitialFlag) {
        this.allowDataInitialFlag = allowDataInitialFlag;
    }

    public Boolean getServerSidePrinting() {
        return serverSidePrinting;
    }

    public void setServerSidePrinting(Boolean serverSidePrinting) {
        this.serverSidePrinting = serverSidePrinting;
    }
}

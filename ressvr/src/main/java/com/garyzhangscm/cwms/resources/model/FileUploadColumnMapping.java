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

import javax.persistence.*;

@Entity
@Table(name = "file_upload_column_mapping")
public class FileUploadColumnMapping extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_upload_column_mapping_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "type")
    private String type;

    @Column(name = "column_name")
    private String columnName;
    @Column(name = "mapping_to_column_name")
    private String mapToColumnName;


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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getMapToColumnName() {
        return mapToColumnName;
    }

    public void setMapToColumnName(String mapToColumnName) {
        this.mapToColumnName = mapToColumnName;
    }
}

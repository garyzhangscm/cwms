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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_report")
public class CustomReport extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_report_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    // whether run the query at the company level or
    // warehouse level
    @Column(name = "run_at_company_level")
    private Boolean runAtCompanyLevel;


    @Column(name = "company_id_field_name")
    private String companyIdFieldName;


    @Column(name = "warehouse_id_field_name")
    private String warehouseIdFieldName;

    @Column(name = "query")
    private String query;

    @Column(name = "group_by")
    private String groupBy;

    @Column(name = "sort_by")
    private String sortBy;


    @OneToMany(
            mappedBy = "customReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<CustomReportParameter> customReportParameters = new ArrayList<>();

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

    public Boolean getRunAtCompanyLevel() {
        return runAtCompanyLevel;
    }

    public void setRunAtCompanyLevel(Boolean runAtCompanyLevel) {
        this.runAtCompanyLevel = runAtCompanyLevel;
    }

    public String getCompanyIdFieldName() {
        return companyIdFieldName;
    }

    public void setCompanyIdFieldName(String companyIdFieldName) {
        this.companyIdFieldName = companyIdFieldName;
    }

    public String getWarehouseIdFieldName() {
        return warehouseIdFieldName;
    }

    public void setWarehouseIdFieldName(String warehouseIdFieldName) {
        this.warehouseIdFieldName = warehouseIdFieldName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<CustomReportParameter> getCustomReportParameters() {
        return customReportParameters;
    }

    public void setCustomReportParameters(List<CustomReportParameter> customReportParameters) {
        this.customReportParameters = customReportParameters;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}

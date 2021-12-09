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

package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;

import javax.persistence.*;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "data_transfer_request")
public class DataTransferRequest extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_transfer_request_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DataTransferRequestStatus status;

    @Transient
    private Company company;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private DataTransferRequestType type;


    @OneToMany(
            mappedBy = "dataTransferRequest",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private Set<DataTransferRequestDetail> dataTransferRequestDetails = new TreeSet<>();


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataTransferRequestStatus getStatus() {
        return status;
    }

    public DataTransferRequestType getType() {
        return type;
    }

    public void setType(DataTransferRequestType type) {
        this.type = type;
    }

    public void setStatus(DataTransferRequestStatus status) {
        this.status = status;
    }

    public Set<DataTransferRequestDetail> getDataTransferRequestDetails() {
        return dataTransferRequestDetails;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @JsonIgnore
    public DataTransferRequestDetail getDataTransferRequestDetail(DataTransferRequestTable tablesName) {
        return dataTransferRequestDetails.stream().filter(
                dataTransferRequestDetail -> dataTransferRequestDetail.getTablesName().equals(tablesName)
        ).findFirst().orElse(null);
    }

    public void setDataTransferRequestDetails(Set<DataTransferRequestDetail> dataTransferRequestDetails) {
        this.dataTransferRequestDetails = dataTransferRequestDetails;
    }

    public void addDataTransferRequestDetails(DataTransferRequestDetail dataTransferRequestDetail) {
        this.dataTransferRequestDetails.add(dataTransferRequestDetail);
    }
}

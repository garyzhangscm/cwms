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

import javax.persistence.*;

@Entity
@Table(name = "data_transfer_request_detail")
public class DataTransferRequestDetail extends AuditibleEntity<String> implements Comparable<DataTransferRequestDetail> {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_transfer_request_detail_id")
    private Long id;


    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "description")
    private String description;

    // tables that will be import / export
    @Column(name = "tables_name")
    @Enumerated(EnumType.STRING)
    private DataTransferRequestTable tablesName;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DataTransferRequestStatus status;

    @ManyToOne
    @JoinColumn(name="data_transfer_request_id")
    @JsonIgnore
    private DataTransferRequest dataTransferRequest;

    public DataTransferRequestDetail(){}
    public DataTransferRequestDetail(Integer sequence,
                                     String description, DataTransferRequestTable tablesName,
                                     DataTransferRequestStatus status,
                                     DataTransferRequest dataTransferRequest) {
        this.sequence = sequence;
        this.description = description;
        this.tablesName = tablesName;
        this.status = status;
        this.dataTransferRequest = dataTransferRequest;
    }


    @Override
    public int compareTo(DataTransferRequestDetail anotherDataTransferRequestDetail) {
        return this.getSequence().compareTo(anotherDataTransferRequestDetail.getSequence());
    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataTransferRequestTable getTablesName() {
        return tablesName;
    }

    public void setTablesName(DataTransferRequestTable tablesName) {
        this.tablesName = tablesName;
    }

    public DataTransferRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DataTransferRequestStatus status) {
        this.status = status;
    }

    public DataTransferRequest getDataTransferRequest() {
        return dataTransferRequest;
    }

    public void setDataTransferRequest(DataTransferRequest dataTransferRequest) {
        this.dataTransferRequest = dataTransferRequest;
    }
}

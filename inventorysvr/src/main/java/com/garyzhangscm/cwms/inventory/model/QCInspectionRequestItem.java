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

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "qc_inspection_request_item")
public class QCInspectionRequestItem extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(QCInspectionRequestItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qc_inspection_request_item_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="qc_rule_id")
    private QCRule qcRule;

    @Column(name = "qc_inspection_result")
    @Enumerated(EnumType.STRING)
    private QCInspectionResult qcInspectionResult;


    @ManyToOne
    @JoinColumn(name = "qc_inspection_request_id")
    @JsonIgnore
    private QCInspectionRequest qcInspectionRequest;

    @Transient
    private String qcInspectionRequestNumber;


    @OneToMany(
            mappedBy = "qcInspectionRequestItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<QCInspectionRequestItemOption> qcInspectionRequestItemOptions = new ArrayList<>();

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

    public QCRule getQcRule() {
        return qcRule;
    }

    public void setQcRule(QCRule qcRule) {
        this.qcRule = qcRule;
    }

    public QCInspectionResult getQcInspectionResult() {
        return qcInspectionResult;
    }

    public void setQcInspectionResult(QCInspectionResult qcInspectionResult) {
        this.qcInspectionResult = qcInspectionResult;
    }

    public QCInspectionRequest getQcInspectionRequest() {
        return qcInspectionRequest;
    }

    public void setQcInspectionRequest(QCInspectionRequest qcInspectionRequest) {
        this.qcInspectionRequest = qcInspectionRequest;
    }

    public List<QCInspectionRequestItemOption> getQcInspectionRequestItemOptions() {
        return qcInspectionRequestItemOptions;
    }

    public String getQcInspectionRequestNumber() {
        return Strings.isNotBlank(qcInspectionRequestNumber) ?
               qcInspectionRequestNumber :
                Objects.nonNull(qcInspectionRequest) ?
                    qcInspectionRequest.getNumber() : "";
    }

    public void setQcInspectionRequestNumber(String qcInspectionRequestNumber) {
        this.qcInspectionRequestNumber = qcInspectionRequestNumber;
    }

    public void setQcInspectionRequestItemOptions(List<QCInspectionRequestItemOption> qcInspectionRequestItemOptions) {
        this.qcInspectionRequestItemOptions = qcInspectionRequestItemOptions;
    }

    public void addQcInspectionRequestItemOption(QCInspectionRequestItemOption qcInspectionRequestItemOption) {
        this.qcInspectionRequestItemOptions.add(qcInspectionRequestItemOption);
    }
}

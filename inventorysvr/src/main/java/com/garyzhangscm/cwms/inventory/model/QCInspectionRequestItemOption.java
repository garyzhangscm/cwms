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

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "qc_inspection_request_item_option")
public class QCInspectionRequestItemOption extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qc_inspection_request_item_option_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="qc_rule_item_id")
    private QCRuleItem qcRuleItem;

    @Column(name = "qc_inspection_result")
    @Enumerated(EnumType.STRING)
    private QCInspectionResult qcInspectionResult;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "double_value")
    private Double doubleValue;

    // for display purpose
    @Transient
    private String qcRuleName;



    @ManyToOne
    @JoinColumn(name = "qc_inspection_request_item_id")
    @JsonIgnore
    private QCInspectionRequestItem qcInspectionRequestItem;

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

    public QCRuleItem getQcRuleItem() {
        return qcRuleItem;
    }

    public void setQcRuleItem(QCRuleItem qcRuleItem) {
        this.qcRuleItem = qcRuleItem;
    }

    public QCInspectionResult getQcInspectionResult() {
        return qcInspectionResult;
    }

    public void setQcInspectionResult(QCInspectionResult qcInspectionResult) {
        this.qcInspectionResult = qcInspectionResult;
    }

    public QCInspectionRequestItem getQcInspectionRequestItem() {
        return qcInspectionRequestItem;
    }

    public void setQcInspectionRequestItem(QCInspectionRequestItem qcInspectionRequestItem) {
        this.qcInspectionRequestItem = qcInspectionRequestItem;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public String getQcRuleName() {
        return Strings.isNotBlank(qcRuleName) ?
                  qcRuleName :
                Objects.nonNull(qcInspectionRequestItem) && Objects.nonNull(qcInspectionRequestItem.getQcRule()) ?
                    qcInspectionRequestItem.getQcRule().getName() : "";
    }

    public void setQcRuleName(String qcRuleName) {
        this.qcRuleName = qcRuleName;
    }
}

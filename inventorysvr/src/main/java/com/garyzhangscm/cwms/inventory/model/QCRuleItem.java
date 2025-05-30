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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;

import java.io.Serializable;


@Entity
@Table(name = "qc_rule_item")
public class QCRuleItem extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(QCRuleItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qc_rule_item_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "check_point")
    private String checkPoint;


    @Column(name = "qc_rule_item_type")
    @Enumerated(EnumType.STRING)
    private QCRuleItemType qcRuleItemType;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "enabled")
    private Boolean enabled = true;


    @Column(name = "qc_rule_item_comparator")
    @Enumerated(EnumType.STRING)
    private QCRuleItemComparator qcRuleItemComparator;


    @ManyToOne
    @JoinColumn(name = "qc_rule_id")
    @JsonIgnore
    private QCRule qcRule;

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

    public String getCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(String checkPoint) {
        this.checkPoint = checkPoint;
    }

    public QCRuleItemType getQcRuleItemType() {
        return qcRuleItemType;
    }

    public void setQcRuleItemType(QCRuleItemType qcRuleItemType) {
        this.qcRuleItemType = qcRuleItemType;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public QCRuleItemComparator getQcRuleItemComparator() {
        return qcRuleItemComparator;
    }

    public void setQcRuleItemComparator(QCRuleItemComparator qcRuleItemComparator) {
        this.qcRuleItemComparator = qcRuleItemComparator;
    }

    public QCRule getQcRule() {
        return qcRule;
    }

    public void setQcRule(QCRule qcRule) {
        this.qcRule = qcRule;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

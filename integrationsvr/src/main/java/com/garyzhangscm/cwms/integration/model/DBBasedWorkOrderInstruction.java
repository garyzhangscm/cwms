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

package com.garyzhangscm.cwms.integration.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_work_order_instruction")
public class DBBasedWorkOrderInstruction extends AuditibleEntity<String> implements Serializable, IntegrationWorkOrderInstructionData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_work_order_instruction_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "instruction")
    private String instruction;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_work_order_id")
    private DBBasedWorkOrder workOrder;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public DBBasedWorkOrderInstruction(){}


    public WorkOrderInstruction convertToWorkOrderInstruction() {
        WorkOrderInstruction workOrderInstruction = new WorkOrderInstruction();

        String[] fieldNames = {
                "sequence", "instruction"
        };

        ObjectCopyUtil.copyValue(this, workOrderInstruction, fieldNames);

        return workOrderInstruction;
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

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @Override
    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }


    @Override
    public DBBasedWorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(DBBasedWorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    @Override
    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}

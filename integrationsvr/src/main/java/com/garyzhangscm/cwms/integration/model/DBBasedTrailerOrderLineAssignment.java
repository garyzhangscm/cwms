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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_trailer_order_line_assignment")
public class DBBasedTrailerOrderLineAssignment extends AuditibleEntity<String>  implements Serializable  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_trailer_order_line_assignment_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;


    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "order_id")
    private Long orderId;
    @Column(name = "order_line_number")
    private String orderLineNumber;
    @Column(name = "order_line_id")
    private Long orderLineId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_stop_id")
    private DBBasedStop stop;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public TrailerOrderLineAssignment convertToTrailerOrderLineAssignment(
            WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) throws UnsupportedEncodingException {

        // company ID or company code is required
        if (Objects.isNull(companyId) && Strings.isBlank(companyCode)) {

            throw MissingInformationException.raiseException("company information is required for trailer appointment integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            setCompanyId(
                    warehouseLayoutServiceRestemplateClient
                            .getCompanyByCode(companyCode).getId()
            );
        }

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
            setWarehouseId(warehouseId);
        }

        TrailerOrderLineAssignment trailerOrderLineAssignment = new TrailerOrderLineAssignment();

        String[] fieldNames = {
                "warehouseId",
                "companyId",
                "orderNumber","orderId",
                "orderLineNumber","orderLineId"
        };

        ObjectCopyUtil.copyValue(this, trailerOrderLineAssignment,  fieldNames);

        return trailerOrderLineAssignment;

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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderLineNumber() {
        return orderLineNumber;
    }

    public void setOrderLineNumber(String orderLineNumber) {
        this.orderLineNumber = orderLineNumber;
    }

    public DBBasedStop getStop() {
        return stop;
    }

    public void setStop(DBBasedStop stop) {
        this.stop = stop;
    }

    public Long getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Long orderLineId) {
        this.orderLineId = orderLineId;
    }

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

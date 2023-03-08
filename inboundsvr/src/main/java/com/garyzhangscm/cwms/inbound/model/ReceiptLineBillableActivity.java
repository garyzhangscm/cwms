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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.ZonedDateTime;


@Entity
@Table(name = "receipt_line_billable_activity")
public class ReceiptLineBillableActivity extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_line_billable_activity_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "billable_activity_type_id")
    private Long billableActivityTypeId;

    @Transient
    private BillableActivityType billableActivityType;

    @Column(name = "activity_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime activityTime;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_line_id")
    private ReceiptLine receiptLine;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "total_charge")
    private Double totalCharge;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillableActivityTypeId() {
        return billableActivityTypeId;
    }

    public void setBillableActivityTypeId(Long billableActivityTypeId) {
        this.billableActivityTypeId = billableActivityTypeId;
    }

    public ReceiptLine getReceiptLine() {
        return receiptLine;
    }

    public void setReceiptLine(ReceiptLine receiptLine) {
        this.receiptLine = receiptLine;
    }

    public ZonedDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(ZonedDateTime activityTime) {
        this.activityTime = activityTime;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

    public BillableActivityType getBillableActivityType() {
        return billableActivityType;
    }

    public void setBillableActivityType(BillableActivityType billableActivityType) {
        this.billableActivityType = billableActivityType;
    }
}

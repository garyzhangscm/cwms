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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Entity
@Table(name = "wave")
public class Wave  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wave_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "number")
    private String number;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WaveStatus status;

    @Column(name = "comment")
    private String comment;

    @OneToMany(
        mappedBy = "wave",
        cascade = CascadeType.REMOVE,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<ShipmentLine> shipmentLines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }


    public List<ShipmentLine> getShipmentLines() {
        return shipmentLines;
    }

    public void setShipmentLines(List<ShipmentLine> shipmentLines) {
        this.shipmentLines = shipmentLines;
    }

    @JsonIgnore
    public List<Pick> getPicks() {

        return shipmentLines.stream()
                .map(shipmentLine -> shipmentLine.getPicks())
                .filter(list -> !list.isEmpty())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    @JsonIgnore
    public List<ShortAllocation> getShortAllocations() {

        return shipmentLines.stream()
                .map(shipmentLine -> shipmentLine.getShortAllocations())
                .filter(shortAllocations -> !shortAllocations.isEmpty())
                .flatMap(List::stream).collect(Collectors.toList());
    }

    public WaveStatus getStatus() {
        return status;
    }

    public void setStatus(WaveStatus status) {
        this.status = status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }



    public Long getTotalOrderCount() {

        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getOrderLine)
                .map(OrderLine::getOrder)
                .distinct().count();
    }
    public Long getTotalOrderLineCount() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getOrderLine)
                .distinct().count();
    }
    public Long getTotalItemCount() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getOrderLine)
                .map(OrderLine::getItemId)
                .distinct().count();
    }
    public Long getTotalQuantity() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getQuantity).mapToLong(Long::longValue).sum();
    }
    public Long getTotalOpenQuantity() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getOpenQuantity).mapToLong(Long::longValue).sum();
    }
    public Long getTotalInprocessQuantity() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getInprocessQuantity).mapToLong(Long::longValue).sum();
    }
    public Long getTotalShortQuantity() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(shipmentLine -> shipmentLine.getShortAllocations())
                .filter(shortAllocations -> !shortAllocations.isEmpty())
                .flatMap(List::stream)
                .filter(shortAllocation -> !shortAllocation.getStatus().equals(ShortAllocationStatus.CANCELLED))
        .map(ShortAllocation::getQuantity).mapToLong(Long::longValue).sum();
    }
    public Long getTotalPickedQuantity() {
        return 0l;
    }
    public Long getTotalStagedQuantity() {
        return 0l;
    }
    public Long getTotalShippedQuantity() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine::getShippedQuantity).mapToLong(Long::longValue).sum();
    }
    public String getLoadNumbers() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine -> Strings.isNotBlank(ShipmentLine.getShipmentLoadNumber()) ? ShipmentLine.getShipmentLoadNumber() : "")
                .distinct().collect(Collectors.joining(","));
    }
    public String getBillOfLadingNumbers() {
        return shipmentLines.stream()
                .filter(shipmentLine -> shipmentLine.getStatus() != ShipmentLineStatus.CANCELLED)
                .map(ShipmentLine -> Strings.isNotBlank(ShipmentLine.getShipmentBillOfLadingNumber()) ? ShipmentLine.getShipmentBillOfLadingNumber() : "")
                .distinct().collect(Collectors.joining(","));
    }
}

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

import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pallet_pick_label_content")
public class PalletPickLabelContent extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pallet_pick_label_content_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    // order id if the pallet is picked for one order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="outbound_order_id")
    private Order order;

    @Column(name = "reference_number")
    private String referenceNumber;


    @OneToMany(
            mappedBy = "palletPickLabelContent",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<PalletPickLabelPickDetail> palletPickLabelPickDetails = new ArrayList<>();


    // estimated volume
    @Column(name = "volume")
    private Double volume;

    // estimated height
    @Column(name = "height")
    private Double height;

    public PalletPickLabelContent() {}

    /**
     * Generate a pallet pick label that only have one pallet pick
     * @param pick
     */
    public PalletPickLabelContent(String number, Pick pick) {
        this.number = number;
        setWarehouseId(pick.getWarehouseId());
        if (Objects.nonNull(pick.getShipmentLine())) {

            setOrder(pick.getShipmentLine().getOrderLine().getOrder());
            setReferenceNumber(getOrder().getNumber());
        }
        palletPickLabelPickDetails.add(
                new PalletPickLabelPickDetail(this, pick, pick.getQuantity())
        );
        setVolume(pick.getSize());
        setHeight(pick.getHeight());

    }
    /**
     * Generate a pallet pick label that has a list picks
     * @param picks
     */
    public PalletPickLabelContent(String number, List<Pick> picks) {
        this.number = number;

        // we will assume the picks belong to the same warehouse since there's
        // no mean to group picks from different warehouse into
        // a physical pallet
        if (!picks.isEmpty()) {

            setWarehouseId(
                    picks.get(0).getWarehouseId()
            );
            // see if the picks belong to the same order
            if (picks.stream().noneMatch(pick -> Strings.isBlank(pick.getOrderNumber())) &&
                    picks.stream().map(pick -> pick.getOrderNumber()).distinct().count() == 1) {
                // all picks belong to the same order,
                setOrder(picks.get(0).getShipmentLine().getOrderLine().getOrder());
                setReferenceNumber(getOrder().getNumber());

            }
            double totalSize = 0.0;
            double totalHeight = 0.0;
            for(Pick pick : picks) {
                palletPickLabelPickDetails.add(
                        new PalletPickLabelPickDetail(this, pick, pick.getQuantity())
                );
                totalHeight += pick.getHeight();
                totalSize += pick.getSize();
            }
            setVolume(totalSize);
            setHeight(totalHeight);
        }
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public List<PalletPickLabelPickDetail> getPalletPickLabelPickDetails() {
        return palletPickLabelPickDetails;
    }

    public void setPalletPickLabelPickDetails(List<PalletPickLabelPickDetail> palletPickLabelPickDetails) {
        this.palletPickLabelPickDetails = palletPickLabelPickDetails;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}

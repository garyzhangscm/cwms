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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "item_unit_of_measure")
public class ItemUnitOfMeasure extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_unit_of_measure_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Transient
    private UnitOfMeasure unitOfMeasure;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_package_type_id")
    private ItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "weight")
    private Double weight;
    @Column(name = "weight_unit")
    private String weightUnit;

    @Column(name = "length")
    private Double length;
    @Column(name = "length_unit")
    private String lengthUnit;
    @Column(name = "width")
    private Double width;
    @Column(name = "width_unit")
    private String widthUnit;
    @Column(name = "height")
    private Double height;
    @Column(name = "height_unit")
    private String heightUnit;

    @Column(name = "default_for_inbound_receiving")
    private Boolean defaultForInboundReceiving = false;

    @Column(name = "default_for_work_order_receiving")
    private Boolean defaultForWorkOrderReceiving = false;

    // whether we will need to tracking LPN at this level
    @Column(name = "tracking_lpn")
    private Boolean trackingLpn = false;

    // whether we will display at this UOM level
    @Column(name = "default_for_display")
    private Boolean defaultForDisplay = false;

    // whether this is a case(box)
    // we may calculate the size of the inventory based on the
    // case UOM
    @Column(name = "case_flag")
    private Boolean caseFlag = false;

    @Column(name = "pack_flag")
    private Boolean packFlag = false;



    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Warehouse warehouse;

    public ItemUnitOfMeasure(){}

    public ItemUnitOfMeasure(Long companyId, Long warehouseId,
                             ItemDefaultPackageUOM itemDefaultPackageUOM) {
        setCompanyId(companyId);
        setWarehouseId(warehouseId);
        setUnitOfMeasureId(itemDefaultPackageUOM.getUnitOfMeasureId());
        setQuantity(itemDefaultPackageUOM.getQuantity());
        setWeight(itemDefaultPackageUOM.getWeight());
        setWeightUnit(itemDefaultPackageUOM.getWeightUnit());

        setLength(itemDefaultPackageUOM.getLength());
        setLengthUnit(itemDefaultPackageUOM.getLengthUnit());
        setWidth(itemDefaultPackageUOM.getWidth());
        setWidthUnit(itemDefaultPackageUOM.getWidthUnit());
        setHeight(itemDefaultPackageUOM.getHeight());
        setHeightUnit(itemDefaultPackageUOM.getHeightUnit());

        setDefaultForWorkOrderReceiving(itemDefaultPackageUOM.getDefaultForWorkOrderReceiving());
        setDefaultForInboundReceiving(itemDefaultPackageUOM.getDefaultForInboundReceiving());
        setTrackingLpn(itemDefaultPackageUOM.getTrackingLpn());
        setCaseFlag(itemDefaultPackageUOM.getCaseFlag());

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Boolean getDefaultForInboundReceiving() {
        return defaultForInboundReceiving;
    }

    public void setDefaultForInboundReceiving(Boolean defaultForInboundReceiving) {
        this.defaultForInboundReceiving = defaultForInboundReceiving;
    }

    public Boolean getDefaultForWorkOrderReceiving() {
        return defaultForWorkOrderReceiving;
    }

    public void setDefaultForWorkOrderReceiving(Boolean defaultForWorkOrderReceiving) {
        this.defaultForWorkOrderReceiving = defaultForWorkOrderReceiving;
    }

    public Boolean getTrackingLpn() {
        return trackingLpn;
    }

    public void setTrackingLpn(Boolean trackingLpn) {
        this.trackingLpn = trackingLpn;
    }

    public Boolean getCaseFlag() {
        return caseFlag;
    }

    public void setCaseFlag(Boolean caseFlag) {
        this.caseFlag = caseFlag;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public String getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(String lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public String getWidthUnit() {
        return widthUnit;
    }

    public void setWidthUnit(String widthUnit) {
        this.widthUnit = widthUnit;
    }

    public String getHeightUnit() {
        return heightUnit;
    }

    public void setHeightUnit(String heightUnit) {
        this.heightUnit = heightUnit;
    }

    public Boolean getDefaultForDisplay() {
        return defaultForDisplay;
    }

    public void setDefaultForDisplay(Boolean defaultForDisplay) {
        this.defaultForDisplay = defaultForDisplay;
    }

    public Boolean getPackFlag() {
        return packFlag;
    }

    public void setPackFlag(Boolean packFlag) {
        this.packFlag = packFlag;
    }
}

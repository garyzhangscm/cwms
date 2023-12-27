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

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.service.UnitService;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Pick implements Serializable {


    private Long id;


    private String number;

    private Long sourceLocationId;

    private Location sourceLocation;

    private Long destinationLocationId;

    private Location destinationLocation;

    private Long itemId;

    private Item item;

    private Long warehouseId;

    private Warehouse warehouse;

    private ShortAllocation shortAllocation;

    private PickType pickType;

    private Long quantity;

    private Long pickedQuantity;


    private Long workOrderLineId;

    private PickStatus status;

    List<PickMovement> pickMovements = new ArrayList<>();

    private PickList pickList;

    private Long itemPackageTypeId;

    private ItemPackageType itemPackageType;

    @JsonIgnore
    public Double getSize(UnitService unitService) {
        return getSize(unitService, true, false);
    }

    @JsonIgnore
    public Double getSize(UnitService unitService, boolean caseUOMFirst, boolean caseUOMOnly) {

        if (item == null) {
            return 0.0;
        }
        if (caseUOMOnly || caseUOMFirst) {
            // we will calculate the size by case UOM
            // first of all, see if we have a case UOM defined
            ItemPackageType itemPackageType = Objects.isNull(getItemPackageType()) ?
                    item.getDefaultItemPackageType() : getItemPackageType();
            ItemUnitOfMeasure caseUnitOfMeasure = itemPackageType.getCaseItemUnitOfMeasure();
            if (Objects.nonNull(caseUnitOfMeasure)) {
                return getSize(unitService, caseUnitOfMeasure, getQuantity() / caseUnitOfMeasure.getQuantity());
            }
            // case unit of measure if not defined, raise error if the client only
            // want to try with case unit of measure
            if (caseUOMOnly) {
                throw ResourceNotFoundException.raiseException("can't find case UOM for item " + item.getName() +
                        " on pick   " + getNumber() + ", fail to calculate the size");
            }
        }
        // ok, the user is good with calculate the size by stock UOM and there's no case UOM defined

        ItemPackageType itemPackageType = Objects.isNull(getItemPackageType()) ?
                item.getDefaultItemPackageType() : getItemPackageType();
        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasure();
        return getSize(unitService, stockItemUnitOfMeasure, getQuantity() );
    }


    @JsonIgnore
    public Double getSize(UnitService unitService,  ItemUnitOfMeasure itemUnitOfMeasure, Long quantityOfUOM) {


        return unitService.getVolumeByUOM(getWarehouseId(),
                itemUnitOfMeasure, quantityOfUOM);
    }


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

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }


    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getItemPackageTypeId() {
        return itemPackageTypeId;
    }

    public void setItemPackageTypeId(Long itemPackageTypeId) {
        this.itemPackageTypeId = itemPackageTypeId;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Long pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public PickStatus getStatus() {
        return status;
    }

    public void setStatus(PickStatus status) {
        this.status = status;
    }



    public List<PickMovement> getPickMovements() {
        return pickMovements;
    }

    public void setPickMovements(List<PickMovement> pickMovements) {
        this.pickMovements = pickMovements;
    }



    public ShortAllocation getShortAllocation() {
        return shortAllocation;
    }

    public void setShortAllocation(ShortAllocation shortAllocation) {
        this.shortAllocation = shortAllocation;
    }

    public PickList getPickList() {
        return pickList;
    }

    public void setPickList(PickList pickList) {
        this.pickList = pickList;
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




    public PickType getPickType() {
        return pickType;
    }

    public void setPickType(PickType pickType) {
        this.pickType = pickType;
    }

    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }
}

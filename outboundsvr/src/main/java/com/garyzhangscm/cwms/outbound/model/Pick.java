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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.service.UnitService;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.util.Pair;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pick")
public class Pick  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Transient
    private Location sourceLocation;

    @Column(name = "destination_location_id")
    private Long destinationLocationId;

    @Transient
    private Location destinationLocation;

    @Column(name = "item_id")
    private Long itemId;
    @Transient
    private Item item;

    @Column(name = "item_package_type_id")
    private Long itemPackageTypeId;
    @Transient
    private ItemPackageType itemPackageType;



    // will be setup when the pick allocates
    // the whole LPN
    @Column(name = "lpn")
    private String lpn;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    // Whether the pick is for
    // 1. shipment line
    // 2. work order line
    // 3. short allocation
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "shipment_line_id")
    @JsonIgnore
    private ShipmentLine shipmentLine;

    @Column(name = "work_task_id")
    private Long workTaskId;
    @Transient
    private WorkTask workTask;

    @ManyToOne
    @JoinColumn(name = "cartonization_id")
    @JsonIgnore
    private Cartonization cartonization;


    @Column(name = "work_order_line_id")
    private Long workOrderLineId;

    @ManyToOne
    @JoinColumn(name = "short_allocation_id")
    @JsonIgnore
    private ShortAllocation shortAllocation;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private PickType pickType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "whole_lpn_pick")
    private Boolean wholeLPNPick;


    @Column(name = "picked_quantity")
    private Long pickedQuantity;



    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PickStatus status;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @OneToMany(
            mappedBy = "pick",
            cascade = CascadeType.ALL,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    List<PickMovement> pickMovements = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "pick_list_id")
    @JsonIgnore
    private PickList pickList;

    @Transient
    private String pickListNumber;

    @ManyToOne
    @JoinColumn(name = "bulk_pick_id")
    @JsonIgnore
    private BulkPick bulkPick;

    @Transient
    private String bulkPickNumber;

    // the user that current working on this pick
    @Column(name = "acknowledged_username")
    private String acknowledgedUsername;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    // how to confirm the pick. THe flag
    // will be setup according to the area/ location's attribute
    @Column(name = "confirm_item_flag")
    private boolean confirmItemFlag;
    @Column(name = "confirm_location_flag")
    private boolean confirmLocationFlag;
    @Column(name = "confirm_location_code_flag")
    private boolean confirmLocationCodeFlag;
    @Column(name = "confirm_lpn_flag")
    private boolean confirmLpnFlag;


    @Column(name="color")
    private String color = "";

    @Column(name="product_size")
    private String productSize = "";

    @Column(name="style")
    private String style = "";

    // only allocate inventory that received by certain receipt
    @Column(name = "allocate_by_receipt_number")
    private String allocateByReceiptNumber;

    // for report purpose
    @Transient
    private String inventoryAttribute = "";
    @Transient
    private String quantityByUOM = "";

    /**
     * Note: the size will be in the format of the base unit
     * @param unitService
     * @return
     */
    @JsonIgnore
    public Pair<Double, String> getSize(UnitService unitService) {
        return getSize(unitService, true, false);
    }
    /**
     * Note: the size will be in the format of the base unit
     * @param unitService
     * @return
     */
    @JsonIgnore
    public Pair<Double, String> getSize(UnitService unitService, boolean caseUOMFirst, boolean caseUOMOnly) {

        Unit defaultUnit = unitService.getBaseUnit(getWarehouseId(), UnitType.VOLUME);
        if (Objects.isNull(defaultUnit)) {
            defaultUnit = unitService.getCubeInch(getWarehouseId());
        }
        // by default, we will use inch as the length's unit
        String defaultUnitName = Objects.isNull(defaultUnit) ?
                "cubic inch" : defaultUnit.getName();


        if (item == null) {
            return Pair.of(0.0, defaultUnitName);
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
    /**
     * Note: the size will be in the format of the base unit
     * @param unitService
     * @return
     */
    @JsonIgnore
    public Pair<Double, String> getSize(UnitService unitService, ItemUnitOfMeasure itemUnitOfMeasure, Long quantityOfUOM) {

        // Note: the result returned from getVolumeByUOM is the unit of length
        // we will show cubit unit to the user

        Pair<Double, Unit> result =
                unitService.getVolumeByUOM(getWarehouseId(),
                itemUnitOfMeasure, quantityOfUOM);

        return Pair.of(
                Objects.isNull(result.getFirst()) ? 0.0 : result.getFirst(),
                Objects.isNull(result.getSecond()) ? "" : result.getSecond().getName());
    }


    @JsonIgnore
    public Pair<Double, String> getHeight(UnitService unitService) {

        Unit defaultUnit = unitService.getBaseUnit(getWarehouseId(), UnitType.LENGTH);
        if (Objects.isNull(defaultUnit)) {
            defaultUnit = unitService.getInch(getWarehouseId());
        }
        // by default, we will use inch as the length's unit
        String defaultUnitName = Objects.isNull(defaultUnit) ?
                "inch" : defaultUnit.getName();

        if (item == null) {
            return Pair.of(0.0, defaultUnitName);
        }
        /**
        ItemUnitOfMeasure stockItemUnitOfMeasure = item.getItemPackageTypes().get(0).getStockItemUnitOfMeasure();

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
         * **/
        ItemPackageType itemPackageType = Objects.isNull(getItemPackageType()) ?
                item.getDefaultItemPackageType() : getItemPackageType();

        if (Objects.nonNull(itemPackageType.getCaseItemUnitOfMeasure())) {
            // if we have the case UOM defined for this package type
            // 1. if the picked quantity is less than one case, then we will assume there's no
            //    case needed for outbound and we will use the stock UOM's height
            // 2. otherwise
            // 2.1. if we also have case per tier defined, then we can know exactly
            //    how many layer of cases we will have if we palletize the picked inventory
            //    so we can calculate the height
            // 2.2. if we don't know the case per tier, then we know at least we probably will
            //    have one case and we will use the case's height as the picked inventory's overall
            //    height


            if (getQuantity() < itemPackageType.getCaseItemUnitOfMeasure().getQuantity()) {
                // pick quantity is less than one case, return the stock UOM's height
                return Pair.of(itemPackageType.getStockItemUnitOfMeasure().getHeight(),
                        Strings.isBlank(itemPackageType.getStockItemUnitOfMeasure().getHeightUnit()) ?
                                defaultUnitName : itemPackageType.getStockItemUnitOfMeasure().getHeightUnit());
            }

            if (Objects.nonNull(itemPackageType.getCasePerTier()) &&
                itemPackageType.getCasePerTier() > 0) {
                // we have both case UOM and case per tier defined, get the right height based on the
                // number of layer of cases, and case height
                return Pair.of((Math.ceil(getQuantity() / itemPackageType.getCaseItemUnitOfMeasure().getQuantity())  // how many cases in the pick
                        / itemPackageType.getCasePerTier())    // how many cases per tier
                        * itemPackageType.getCaseItemUnitOfMeasure().getHeight(),
                        Strings.isBlank(itemPackageType.getCaseItemUnitOfMeasure().getHeightUnit()) ?
                            defaultUnitName : itemPackageType.getCaseItemUnitOfMeasure().getHeightUnit());       // height per case(per tier)
            }
            // case per tier is not defined, we at least have one case but we don't know how the cases
            // are stacked, so we will just return the case's height
            return Pair.of(itemPackageType.getCaseItemUnitOfMeasure().getHeight(),
                    Strings.isBlank(itemPackageType.getCaseItemUnitOfMeasure().getHeightUnit()) ?
                        defaultUnitName : itemPackageType.getCaseItemUnitOfMeasure().getHeightUnit());
        }
        // case UOM is not defined, let's just return the stock UOM's height
        return Pair.of(itemPackageType.getStockItemUnitOfMeasure().getHeight(),
                Strings.isBlank(itemPackageType.getStockItemUnitOfMeasure().getHeightUnit()) ?
                    defaultUnitName : itemPackageType.getStockItemUnitOfMeasure().getHeightUnit());
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

    public String getPickListNumber() {
        if (Objects.isNull(pickList)) {
            // If the pick list is empty, let's check if it belongs
            // to a cartonization pick
            if (Objects.isNull(cartonization) ||
                    Objects.isNull(cartonization.getPickList())) {
                return null;
            }
            else {
                // the pick belongs to a cartonization,
                // let's return the list of the cartonization
                return cartonization.getPickList().getNumber();

            }
        }
        else {
            return pickList.getNumber();
        }
    }

    public Pick clone() {
        Pick pick = new Pick();

        pick.setNumber(getNumber());

        pick.setSourceLocationId(getSourceLocationId());
        pick.setDestinationLocationId(getDestinationLocationId());
        pick.setItemId(getItemId());
        pick.setLpn(getLpn());
        pick.setWarehouseId(getWarehouseId());
        pick.setShipmentLine(getShipmentLine());
        pick.setCartonization(getCartonization());

        pick.setWorkOrderLineId(getWorkOrderLineId());
        pick.setShortAllocation(getShortAllocation());
        pick.setPickType(getPickType());
        pick.setQuantity(getQuantity());
        pick.setPickedQuantity(getPickedQuantity());
        pick.setStatus(getStatus());
        pick.setInventoryStatusId(getInventoryStatusId());

        // setup the pick move
        List<PickMovement> newPickMovements = new ArrayList<>();
        for (PickMovement pickMovement : getPickMovements()) {
            PickMovement newPickMovement = new PickMovement();
            newPickMovement.setPick(pick);
            newPickMovement.setWarehouseId(pickMovement.getWarehouseId());
            newPickMovement.setLocationId(pickMovement.getLocationId());
            newPickMovement.setSequence(pickMovement.getSequence());
            newPickMovement.setArrivedQuantity(0l);
            newPickMovements.add(newPickMovement);
        }
        pick.setPickMovements(newPickMovements);
        pick.setPickList(getPickList());
        pick.setBulkPick(getBulkPick());
        pick.setUnitOfMeasureId(getUnitOfMeasureId());
        pick.setConfirmLpnFlag(isConfirmLpnFlag());
        pick.setConfirmItemFlag(isConfirmItemFlag());
        pick.setConfirmLocationCodeFlag(isConfirmLocationCodeFlag());
        pick.setConfirmLocationFlag(isConfirmLocationFlag());

        pick.setWorkTaskId(getWorkTaskId());
        pick.setColor(getColor());
        pick.setProductSize(getProductSize());
        pick.setStyle(getStyle());

        return pick;
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

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
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

    public String getOrderNumber() {
        return shipmentLine == null ? "" : shipmentLine.getOrderNumber();
    }

    public List<PickMovement> getPickMovements() {
        return pickMovements;
    }

    public void setPickMovements(List<PickMovement> pickMovements) {
        this.pickMovements = pickMovements;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
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

    public BulkPick getBulkPick() {
        return bulkPick;
    }

    public void setBulkPick(BulkPick bulkPick) {
        this.bulkPick = bulkPick;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getWholeLPNPick() {

        return wholeLPNPick;
    }

    public void setWholeLPNPick(Boolean wholeLPNPick) {
        this.wholeLPNPick = wholeLPNPick;
    }

    public Client getClient() {
        if (shipmentLine == null) {
            return null;
        }
        return shipmentLine.getOrderLine().getOrder().getClient();
    }
    public Long getClientId() {
        if (shipmentLine == null) {
            return null;
        }
        return shipmentLine.getShipment().getClientId();
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

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public Cartonization getCartonization() {
        return cartonization;
    }

    public void setCartonization(Cartonization cartonization) {
        this.cartonization = cartonization;
    }

    public String getCartonizationNumber(){
        return Objects.isNull(getCartonization()) ? "" : getCartonization().getNumber();
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }


    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public boolean isConfirmItemFlag() {
        return confirmItemFlag;
    }

    public void setConfirmItemFlag(boolean confirmItemFlag) {
        this.confirmItemFlag = confirmItemFlag;
    }

    public boolean isConfirmLocationFlag() {
        return confirmLocationFlag;
    }

    public void setConfirmLocationFlag(boolean confirmLocationFlag) {
        this.confirmLocationFlag = confirmLocationFlag;
    }

    public boolean isConfirmLocationCodeFlag() {
        return confirmLocationCodeFlag;
    }

    public void setConfirmLocationCodeFlag(boolean confirmLocationCodeFlag) {
        this.confirmLocationCodeFlag = confirmLocationCodeFlag;
    }
    public boolean isConfirmLpnFlag() {
        return confirmLpnFlag;
    }

    public void setConfirmLpnFlag(boolean confirmLpnFlag) {
        this.confirmLpnFlag = confirmLpnFlag;
    }

    // for pick sheet display only
    public String getDefaultPickableStockUomName() {
        if (Objects.isNull(item) ||
                Objects.isNull(item.getDefaultItemPackageType()) ||
                Objects.isNull(item.getDefaultItemPackageType().getStockItemUnitOfMeasure()) ||
                Objects.isNull(item.getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure())) {
            return "";
        }
        else {
            return item.getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure().getName();
        }
    }


    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setPickListNumber(String pickListNumber) {
        this.pickListNumber = pickListNumber;
    }

    public String getBulkPickNumber() {
        return Objects.nonNull(getBulkPick()) ? getBulkPick().getNumber() :
                bulkPickNumber;
    }

    public void setBulkPickNumber(String bulkPickNumber) {
        this.bulkPickNumber = bulkPickNumber;
    }

    public Long getWorkTaskId() {
        return workTaskId;
    }

    public void setWorkTaskId(Long workTaskId) {
        this.workTaskId = workTaskId;
    }

    public WorkTask getWorkTask() {
        return workTask;
    }

    public void setWorkTask(WorkTask workTask) {
        this.workTask = workTask;
    }

    public String getInventoryAttribute() {
        return inventoryAttribute;
    }

    public void setInventoryAttribute(String inventoryAttribute) {
        this.inventoryAttribute = inventoryAttribute;
    }

    public String getQuantityByUOM() {
        return quantityByUOM;
    }

    public void setQuantityByUOM(String quantityByUOM) {
        this.quantityByUOM = quantityByUOM;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
    }

    public String getCustomerName() {
        if (Objects.isNull(getShipmentLine())) {
            return "";
        }
        return getShipmentLine().getShipment().getShipToContactorFirstname() + " , " +
            getShipmentLine().getShipment().getShipToContactorLastname();
    }

    public String getAcknowledgedUsername() {
        return acknowledgedUsername;
    }

    public void setAcknowledgedUsername(String acknowledgedUsername) {
        this.acknowledgedUsername = acknowledgedUsername;
    }
}

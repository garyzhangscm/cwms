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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "item")
//@JsonIgnoreProperties({"hibernateLazyInitializer"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;



    @ManyToOne
    @JoinColumn(name="item_family_id")
    private ItemFamily itemFamily;

    @OneToMany(
            mappedBy = "item",
            //cascade = CascadeType.ALL,
            //orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ItemBarcode> itemBarcodes = new ArrayList<>();

    @OneToMany(
        mappedBy = "item",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<ItemPackageType> itemPackageTypes= new ArrayList<>();

    // by default, we will tracking the size of the inventory
    // of this item.
    // If this flag is set to false, then we don't have to maintain
    // the size of the inventory.
    // If this flag is setup to false for all items in the warehouse, then
    // we don't have to configure the size for the inventory and locations.
    @Column(name="tracking_volume_flag")
    private boolean trackingVolumeFlag = true;

    @Column(name="tracking_lot_number_flag")
    private boolean trackingLotNumberFlag = false;

    @Column(name="tracking_manufacture_date_flag")
    private boolean trackingManufactureDateFlag = false;

    @Column(name="tracking_color_flag")
    private boolean trackingColorFlag = false;
    @Column(name="default_color")
    private String defaultColor;

    @Column(name="tracking_product_size_flag")
    private boolean trackingProductSizeFlag = false;
    @Column(name="default_product_size")
    private String defaultProductSize;

    @Column(name="tracking_style_flag")
    private boolean trackingStyleFlag = false;
    @Column(name="default_style")
    private String defaultStyle;


    @Column(name="tracking_inventory_attribute_1_flag")
    private boolean trackingInventoryAttribute1Flag = false;
    @Column(name="default_inventory_attribute_1")
    private String defaultInventoryAttribute1;

    @Column(name="tracking_inventory_attribute_2_flag")
    private boolean trackingInventoryAttribute2Flag = false;
    @Column(name="default_inventory_attribute_2")
    private String defaultInventoryAttribute2;

    @Column(name="tracking_inventory_attribute_3_flag")
    private boolean trackingInventoryAttribute3Flag = false;
    @Column(name="default_inventory_attribute_3")
    private String defaultInventoryAttribute3;

    @Column(name="tracking_inventory_attribute_4_flag")
    private boolean trackingInventoryAttribute4Flag = false;
    @Column(name="default_inventory_attribute_4")
    private String defaultInventoryAttribute4;

    @Column(name="tracking_inventory_attribute_5_flag")
    private boolean trackingInventoryAttribute5Flag = false;
    @Column(name="default_inventory_attribute_5")
    private String defaultInventoryAttribute5;

    @Column(name="shelf_life_days")
    private Integer shelfLifeDays = 0;

    @Column(name="tracking_expiration_date_flag")
    private boolean trackingExpirationDateFlag = false;


    @Column(name="abc_category_id")
    private Long abcCategoryId;

    @Transient
    @JsonIgnore
    private ABCCategory abcCategory;

    @Column(name="velocity_id")
    private Long velocityId;

    @Transient
    @JsonIgnore
    private Velocity velocity;


    @Column(name="unit_cost")
    private Double unitCost= 0.0;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "company_id")
    private Long companyId;


    @Column(name = "allow_cartonization")
    private Boolean allowCartonization = false;

    @Transient
    private ItemPackageType defaultItemPackageType;

    // those 2 attributes normally work together
    // to let the user allocate the whole LPN
    // in case it is not easy to pick individual
    // uom
    @Column(name = "allow_allocation_by_lpn")
    private Boolean allowAllocationByLPN = false;
    @Column(name = "allocation_round_up_strategy_type")
    @Enumerated(EnumType.STRING)
    private AllocationRoundUpStrategyType allocationRoundUpStrategyType = AllocationRoundUpStrategyType.NONE;

    @Column(name = "allocation_round_up_strategy_value")
    private Double allocationRoundUpStrategyValue = 0.0;


    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "work_order_sop_url")
    private String workOrderSOPUrl;

    @Column(name = "active_flag")
    private Boolean activeFlag = true;


    // quickbook unique id
    @Column(name = "quickbook_listid")
    private String quickbookListId;

    @Column(name="non_inventory_item")
    private Boolean nonInventoryItem;

    @Column(name="receiving_rate_by_unit")
    private Double receivingRateByUnit;

    @Column(name="shipping_rate_by_unit")
    private Double shippingRateByUnit;

    @Column(name="handling_rate_by_unit")
    private Double handlingRateByUnit;


    // for kit item, the parent - children
    // relationship will be maintained by the
    // bill of material
    @Column(name = "kit_item_flag")
    private Boolean kitItemFlag = false;


    @Column(name = "bill_of_material_id")
    private Long billOfMaterialId;
    @Transient
    private BillOfMaterial billOfMaterial;

    @Transient
    private List<Item> kitInnerItems = new ArrayList<>();

    @Transient
    private List<Double> kitInnerItemQuanties = new ArrayList<>();

    // whether allow the operator to break the kit
    // and process the individuals
    // only when it is a kit item
    @Column(name = "kit_allow_break_flag")
    private Boolean kitAllowBreakFlag = false;
    // whether allow the operator to break the kit
    // and ship the individuals
    // only when it is a kit item
    @Column(name = "kit_allow_ship_individual_flag")
    private Boolean kitAllowShipIndividualFlag = false;



    @Transient
    @JsonIgnore
    private Warehouse warehouse;

    @Override
    public boolean equals(Object anotherItem) {
        if (this == anotherItem) {
            return true;
        }
        if (!(anotherItem instanceof Item)) {
            return false;
        }

        Item that = (Item) anotherItem;
        if (Objects.nonNull(id) && Objects.nonNull(that.id)) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name) &&
                Objects.equals(warehouseId, that.warehouseId);
    }
    @Override
    public int hashCode() {
        return this.getName().hashCode();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getClientId() {
        return clientId;
    }

    public AllocationRoundUpStrategyType getAllocationRoundUpStrategyType() {
        return allocationRoundUpStrategyType;
    }

    public void setAllocationRoundUpStrategyType(AllocationRoundUpStrategyType allocationRoundUpStrategyType) {
        this.allocationRoundUpStrategyType = allocationRoundUpStrategyType;
    }

    public Double getAllocationRoundUpStrategyValue() {
        return allocationRoundUpStrategyValue;
    }

    public void setAllocationRoundUpStrategyValue(Double allocationRoundUpStrategyValue) {
        this.allocationRoundUpStrategyValue = allocationRoundUpStrategyValue;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public List<ItemPackageType> getItemPackageTypes() {
        return itemPackageTypes;
    }

    public void setItemPackageTypes(List<ItemPackageType> itemPackageTypes) {
        this.itemPackageTypes = itemPackageTypes;
    }
    public void addItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageTypes.add(itemPackageType);
    }

    public boolean isTrackingLotNumberFlag() {
        return trackingLotNumberFlag;
    }

    public void setTrackingLotNumberFlag(boolean trackingLotNumberFlag) {
        this.trackingLotNumberFlag = trackingLotNumberFlag;
    }

    public boolean isTrackingManufactureDateFlag() {
        return trackingManufactureDateFlag;
    }

    public void setTrackingManufactureDateFlag(boolean trackingManufactureDateFlag) {
        this.trackingManufactureDateFlag = trackingManufactureDateFlag;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getQuickbookListId() {
        return quickbookListId;
    }


    public boolean isTrackingInventoryAttribute1Flag() {
        return trackingInventoryAttribute1Flag;
    }

    public void setTrackingInventoryAttribute1Flag(boolean trackingInventoryAttribute1Flag) {
        this.trackingInventoryAttribute1Flag = trackingInventoryAttribute1Flag;
    }

    public String getDefaultInventoryAttribute1() {
        return defaultInventoryAttribute1;
    }

    public void setDefaultInventoryAttribute1(String defaultInventoryAttribute1) {
        this.defaultInventoryAttribute1 = defaultInventoryAttribute1;
    }

    public boolean isTrackingInventoryAttribute2Flag() {
        return trackingInventoryAttribute2Flag;
    }

    public void setTrackingInventoryAttribute2Flag(boolean trackingInventoryAttribute2Flag) {
        this.trackingInventoryAttribute2Flag = trackingInventoryAttribute2Flag;
    }

    public String getDefaultInventoryAttribute2() {
        return defaultInventoryAttribute2;
    }

    public void setDefaultInventoryAttribute2(String defaultInventoryAttribute2) {
        this.defaultInventoryAttribute2 = defaultInventoryAttribute2;
    }

    public boolean isTrackingInventoryAttribute3Flag() {
        return trackingInventoryAttribute3Flag;
    }

    public void setTrackingInventoryAttribute3Flag(boolean trackingInventoryAttribute3Flag) {
        this.trackingInventoryAttribute3Flag = trackingInventoryAttribute3Flag;
    }

    public String getDefaultInventoryAttribute3() {
        return defaultInventoryAttribute3;
    }

    public void setDefaultInventoryAttribute3(String defaultInventoryAttribute3) {
        this.defaultInventoryAttribute3 = defaultInventoryAttribute3;
    }

    public boolean isTrackingInventoryAttribute4Flag() {
        return trackingInventoryAttribute4Flag;
    }

    public void setTrackingInventoryAttribute4Flag(boolean trackingInventoryAttribute4Flag) {
        this.trackingInventoryAttribute4Flag = trackingInventoryAttribute4Flag;
    }

    public String getDefaultInventoryAttribute4() {
        return defaultInventoryAttribute4;
    }

    public void setDefaultInventoryAttribute4(String defaultInventoryAttribute4) {
        this.defaultInventoryAttribute4 = defaultInventoryAttribute4;
    }

    public Long getBillOfMaterialId() {
        return billOfMaterialId;
    }

    public void setBillOfMaterialId(Long billOfMaterialId) {
        this.billOfMaterialId = billOfMaterialId;
    }

    public Boolean getKitItemFlag() {
        return kitItemFlag;
    }

    public void setKitItemFlag(Boolean kitItemFlag) {
        this.kitItemFlag = kitItemFlag;
    }

    public boolean isTrackingInventoryAttribute5Flag() {
        return trackingInventoryAttribute5Flag;
    }

    public void setTrackingInventoryAttribute5Flag(boolean trackingInventoryAttribute5Flag) {
        this.trackingInventoryAttribute5Flag = trackingInventoryAttribute5Flag;
    }

    public String getDefaultInventoryAttribute5() {
        return defaultInventoryAttribute5;
    }

    public void setDefaultInventoryAttribute5(String defaultInventoryAttribute5) {
        this.defaultInventoryAttribute5 = defaultInventoryAttribute5;
    }

    public void setQuickbookListId(String quickbookListId) {
        this.quickbookListId = quickbookListId;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public boolean isTrackingExpirationDateFlag() {
        return trackingExpirationDateFlag;
    }

    public void setTrackingExpirationDateFlag(boolean trackingExpirationDateFlag) {
        this.trackingExpirationDateFlag = trackingExpirationDateFlag;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
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

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public Boolean getAllowAllocationByLPN() {
        return allowAllocationByLPN;
    }

    public void setAllowAllocationByLPN(Boolean allowAllocationByLPN) {
        this.allowAllocationByLPN = allowAllocationByLPN;
    }

    public boolean isTrackingVolumeFlag() {
        return trackingVolumeFlag;
    }

    public void setTrackingVolumeFlag(boolean trackingVolumeFlag) {
        this.trackingVolumeFlag = trackingVolumeFlag;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public ABCCategory getAbcCategory() {
        return abcCategory;
    }

    public void setAbcCategory(ABCCategory abcCategory) {
        this.abcCategory = abcCategory;
    }

    public List<ItemBarcode> getItemBarcodes() {
        return itemBarcodes;
    }

    public void setItemBarcodes(List<ItemBarcode> itemBarcodes) {
        this.itemBarcodes = itemBarcodes;
    }

    public Velocity getVelocity() {
        return velocity;
    }


    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    public Boolean getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(Boolean activeFlag) {
        this.activeFlag = activeFlag;
    }

    public ItemPackageType getDefaultItemPackageType() {
        // if the default item package type is not setup for this item
        // then return the first available item package type
        /**
        logger.debug("item {}'s default item package type: ", getName());
        logger.debug(">> Objects.nonNull(defaultItemPackageType)? {}", Objects.nonNull(defaultItemPackageType) );
        if (Objects.isNull(defaultItemPackageType)) {
            logger.debug(">> item package type size: {}", getItemPackageTypes().size());
            if (!getItemPackageTypes().isEmpty()) {
                logger.debug("getItemPackageTypes().get(0).getName(): {}", getItemPackageTypes().get(0).getName());
            }
        }
         **/
        return Objects.nonNull(defaultItemPackageType) ?
                defaultItemPackageType :
                getItemPackageTypes().isEmpty() ?
                        null :
                        getItemPackageTypes().get(0);
    }

    public Long getAbcCategoryId() {
        return abcCategoryId;
    }

    public void setAbcCategoryId(Long abcCategoryId) {
        this.abcCategoryId = abcCategoryId;
    }

    public Long getVelocityId() {
        return velocityId;
    }

    public void setVelocityId(Long velocityId) {
        this.velocityId = velocityId;
    }

    public Boolean getNonInventoryItem() {
        return nonInventoryItem;
    }

    public void setNonInventoryItem(Boolean nonInventoryItem) {
        this.nonInventoryItem = nonInventoryItem;
    }

    public boolean isTrackingColorFlag() {
        return trackingColorFlag;
    }

    public void setTrackingColorFlag(boolean trackingColorFlag) {
        this.trackingColorFlag = trackingColorFlag;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }

    public boolean isTrackingProductSizeFlag() {
        return trackingProductSizeFlag;
    }

    public void setTrackingProductSizeFlag(boolean trackingProductSizeFlag) {
        this.trackingProductSizeFlag = trackingProductSizeFlag;
    }

    public String getDefaultProductSize() {
        return defaultProductSize;
    }

    public void setDefaultProductSize(String defaultProductSize) {
        this.defaultProductSize = defaultProductSize;
    }

    public boolean isTrackingStyleFlag() {
        return trackingStyleFlag;
    }

    public void setTrackingStyleFlag(boolean trackingStyleFlag) {
        this.trackingStyleFlag = trackingStyleFlag;
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public void setDefaultItemPackageType(ItemPackageType defaultItemPackageType) {
        this.defaultItemPackageType = defaultItemPackageType;
    }

    public Double getReceivingRateByUnit() {
        return receivingRateByUnit;
    }

    public void setReceivingRateByUnit(Double receivingRateByUnit) {
        this.receivingRateByUnit = receivingRateByUnit;
    }

    public Double getShippingRateByUnit() {
        return shippingRateByUnit;
    }

    public void setShippingRateByUnit(Double shippingRateByUnit) {
        this.shippingRateByUnit = shippingRateByUnit;
    }

    public Double getHandlingRateByUnit() {
        return handlingRateByUnit;
    }

    public void setHandlingRateByUnit(Double handlingRateByUnit) {
        this.handlingRateByUnit = handlingRateByUnit;
    }

    public String getWorkOrderSOPUrl() {
        return workOrderSOPUrl;
    }

    public void setWorkOrderSOPUrl(String workOrderSOPUrl) {
        this.workOrderSOPUrl = workOrderSOPUrl;
    }

    public List<Item> getKitInnerItems() {
        return kitInnerItems;
    }

    public void setKitInnerItems(List<Item> kitInnerItems) {
        this.kitInnerItems = kitInnerItems;
    }
    public void addKitInnerItem(Item kitInnerItem) {
        this.kitInnerItems.add(kitInnerItem);
    }

    public BillOfMaterial getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(BillOfMaterial billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public Double getBillOfMaterialQuantity() {
        if (Objects.isNull(billOfMaterial)) {
            return null;
        }
        return billOfMaterial.getExpectedQuantity();
    }

    public List<Double> getKitInnerItemQuanties() {
        return kitInnerItemQuanties;
    }

    public void setKitInnerItemQuanties(List<Double> kitInnerItemQuanties) {
        this.kitInnerItemQuanties = kitInnerItemQuanties;
    }

    public void addKitInnerItemQuanty(Double quantity) {
        this.kitInnerItemQuanties.add(quantity);
    }

    public Boolean getKitAllowBreakFlag() {
        return kitAllowBreakFlag;
    }

    public void setKitAllowBreakFlag(Boolean kitAllowBreakFlag) {
        this.kitAllowBreakFlag = kitAllowBreakFlag;
    }

    public Boolean getKitAllowShipIndividualFlag() {
        return kitAllowShipIndividualFlag;
    }

    public void setKitAllowShipIndividualFlag(Boolean kitAllowShipIndividualFlag) {
        this.kitAllowShipIndividualFlag = kitAllowShipIndividualFlag;
    }
}

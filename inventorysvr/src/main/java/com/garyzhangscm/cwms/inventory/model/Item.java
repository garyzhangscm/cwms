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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.service.ItemService;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "item")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Item extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

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

    @Column(name="shelf_life_days")
    private Integer shelfLifeDays = 0;

    @Column(name="tracking_expiration_date_flag")
    private boolean trackingExpirationDateFlag = false;


    @ManyToOne
    @JoinColumn(name="abc_category_id")
    private ABCCategory abcCategory;

    @ManyToOne
    @JoinColumn(name="velocity_id")
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

    @Column(name = "active_flag")
    private Boolean activeFlag = true;

    @Transient
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

    public ABCCategory getAbcCategory() {
        return abcCategory;
    }

    public void setAbcCategory(ABCCategory abcCategory) {
        this.abcCategory = abcCategory;
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }
}

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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements Serializable {

    private Long id;

    private Long warehouseId;
    private Long companyId;

    private String name;
    private String description;
    private String quickbookListId;

    private Long clientId;

    private Client client;

    private ItemFamily itemFamily;

    private List<ItemPackageType> itemPackageTypes= new ArrayList<>();
    private ItemPackageType defaultItemPackageType;

    private double unitCost;

    private boolean trackingColorFlag;
    private String defaultColor;

    private boolean trackingProductSizeFlag;
    private String defaultProductSize;

    private boolean trackingStyleFlag;
    private String defaultStyle;

    private boolean trackingInventoryAttribute1Flag;
    private String defaultInventoryAttribute1;

    private boolean trackingInventoryAttribute2Flag;
    private String defaultInventoryAttribute2;

    private boolean trackingInventoryAttribute3Flag;
    private String defaultInventoryAttribute3;

    private boolean trackingInventoryAttribute4Flag;
    private String defaultInventoryAttribute4;

    private boolean trackingInventoryAttribute5Flag;
    private String defaultInventoryAttribute5;

    @Override
    public boolean equals(Object anotherItem) {
        if (this == anotherItem) {
            return true;
        }
        if (!(anotherItem instanceof Item)) {
            return false;
        }
        return this.getName().equals(((Item)anotherItem).getName());
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

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    public String getQuickbookListId() {
        return quickbookListId;
    }

    public void setQuickbookListId(String quickbookListId) {
        this.quickbookListId = quickbookListId;
    }


    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public ItemPackageType getDefaultItemPackageType() {
        // if the default item package type is not setup for this item
        // then return the first available item package type
        return Objects.nonNull(defaultItemPackageType) ?
                defaultItemPackageType :
                getItemPackageTypes().isEmpty() ?
                        null :
                        getItemPackageTypes().get(0);
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
}

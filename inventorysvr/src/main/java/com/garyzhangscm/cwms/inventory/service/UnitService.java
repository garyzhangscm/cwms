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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import com.garyzhangscm.cwms.inventory.model.Unit;
import com.garyzhangscm.cwms.inventory.model.UnitType;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class UnitService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public List<Unit> getAllUnits(Long warehouseId) {
        List<Unit> units =
                commonServiceRestemplateClient.getUnitsByWarehouse(warehouseId);

        // logger.debug("we get {} units from warehouse {}",
        //        units.size(), warehouseId);
        // for (Unit unit : units) {
        //     logger.debug("unit {}, type {}, ratio: {}",
        //             unit.getName(),
        //             unit.getType(),
        //             unit.getRatio());
        // }
        return units;
    }

    private Unit getUnitByName(Long warehouseId, String name) {
        // logger.debug("start to get unit by name {}", name);
        return getAllUnits(warehouseId).stream().filter(
                unit -> {
                    // logger.debug("unit: {} compare to name {}, match? {}",
                    //         unit.getName(),
                    //         name,
                    //         unit.getName().equalsIgnoreCase(name)
                    //         );
                    return unit.getName().equalsIgnoreCase(name);
                }
        ).findFirst().orElse(null);
    }


    public double convert(Long warehouseId,
                          double quantity,
                          String sourceUnitName,
                          String destinationUnitName) {

        Unit sourceUnit = getUnitByName(warehouseId, sourceUnitName);
        Unit destinationUnit = getUnitByName(warehouseId, destinationUnitName);

        if (Objects.isNull(sourceUnit)) {
            throw InventoryException.raiseException("source unit " + sourceUnitName +
                    " is invalid");
        }
        if (Objects.isNull(destinationUnit)) {
            throw InventoryException.raiseException("destination unit " + destinationUnitName +
                    " is invalid");
        }

        return convert(quantity, sourceUnit, destinationUnit);
    }

    public double convert(double quantity,
                          Unit sourceUnit,
                          Unit destinationUnit) {

        // quantity * sourceUnit.getRatio() will convert the quantity into
        // the quantity of base unit
        return quantity * sourceUnit.getRatio() / destinationUnit.getRatio();
    }

    public Unit getCubeFoot(Long warehouseId) {
        return getUnitByName(warehouseId, "cube_foot");
    }

    public Unit getFoot(Long warehouseId) {
        return getUnitByName(warehouseId, "foot");
    }
    public Unit getInch(Long warehouseId) {
        return getUnitByName(warehouseId, "inch");
    }
    public Unit getCubeInch(Long warehouseId) {
        return getUnitByName(warehouseId, "cube_inch");
    }

    public Unit getBaseUnit(Long warehouseId, UnitType type) {
        return getAllUnits(warehouseId).stream().filter(
                unit -> (Boolean.TRUE.equals(unit.getBaseUnitFlag()) ||
                            unit.getRatio() == 1.0
                        )
                        && type.equals(unit.getType())
        ).findFirst().orElse(null);
    }


    /**
     * Get the volume based on the item unit of measure's size and the quantity of this item UOM
     * (item UOM's length * item UOM's width * item UOM's height) * quantity of UOM
     * then convert to the base unit (if defined) or cubic inch(by default)
     * @param warehouseId
     * @param itemUnitOfMeasure
     * @param quantityOfUOM
     * @return
     */
    public double getVolumeByUOM(Long warehouseId, ItemUnitOfMeasure itemUnitOfMeasure, Long quantityOfUOM) {

        // convert the inventory UOM size to  foot
        // by default we will display at cube inch
        Unit baseUnit = getBaseUnit(warehouseId, UnitType.LENGTH);

        Unit unit = baseUnit;
        if (Objects.isNull(unit)) {
            unit = getInch(warehouseId);
        }
        if (Objects.isNull(unit)) {
            throw InventoryException.raiseException("can't convert the length as we are not able to load the unit information");
        }
        // make sure we can get the unit for length / width / height
        if (( Strings.isBlank(itemUnitOfMeasure.getLengthUnit()) ||
                Strings.isBlank(itemUnitOfMeasure.getWidthUnit()) ||
                Strings.isBlank(itemUnitOfMeasure.getHeightUnit())) &
                Objects.isNull(baseUnit)) {

            throw InventoryException.raiseException("unit is not setup for the inventory item unit of measure " +
                    + itemUnitOfMeasure.getId() +
                    " and there's no base unit setup");
        }

        return convert(warehouseId,
                itemUnitOfMeasure.getLength(),
                Strings.isBlank(itemUnitOfMeasure.getLengthUnit()) ?
                        baseUnit.getName() : itemUnitOfMeasure.getLengthUnit(),
                unit.getName()
        ) *
                convert(warehouseId,
                        itemUnitOfMeasure.getWidth(),
                        Strings.isBlank(itemUnitOfMeasure.getWidthUnit()) ?
                                baseUnit.getName() : itemUnitOfMeasure.getWidthUnit(),
                        unit.getName()
                ) *
                convert(warehouseId,
                        itemUnitOfMeasure.getHeight(),
                        Strings.isBlank(itemUnitOfMeasure.getHeightUnit()) ?
                                baseUnit.getName() : itemUnitOfMeasure.getHeightUnit(),
                        unit.getName()
                ) *
                quantityOfUOM;
    }

    public double convertLength(Long warehouseId,
                                double length,
                                String sourceUnitName) {
        Unit lengthBaseUnit = getLengthBaseUnit(warehouseId);
        if (Objects.isNull(lengthBaseUnit)) {
            return length;
        }
        return convertLength(warehouseId, length, sourceUnitName, lengthBaseUnit.getName());
    }
    public double convertLength(Long warehouseId,
                                double length,
                                String sourceUnitName,
                                String destinationUnitName) {
        logger.debug("Start to convert length {} from unit {} to unit {}",
                length,
                Strings.isBlank(sourceUnitName) ? "N/A" : sourceUnitName,
                Strings.isBlank(destinationUnitName) ? "N/A" : destinationUnitName);
        return convert(warehouseId, length,  sourceUnitName, destinationUnitName);
    }
    public double convertWeight(Long warehouseId,
                                double weight,
                                String sourceUnitName) {
        Unit weightBaseUnit = getWeightBaseUnit(warehouseId);
        if (Objects.isNull(weightBaseUnit)) {
            return weight;
        }
        return convertWeight(warehouseId, weight, sourceUnitName, weightBaseUnit.getName());
    }
    public double convertWeight(Long warehouseId,
                                double weight,
                                String sourceUnitName,
                                String destinationUnitName) {
        logger.debug("Start to convert weight {} from unit {} to unit {}",
                weight,
                Strings.isBlank(sourceUnitName) ? "N/A" : sourceUnitName,
                Strings.isBlank(destinationUnitName) ? "N/A" : destinationUnitName);
        return convert(warehouseId, weight, sourceUnitName, destinationUnitName);
    }

    public Unit getLengthBaseUnit(Long warehouseId) {
        return getBaseUnit(warehouseId, UnitType.LENGTH);
    }
    public Unit getWeightBaseUnit(Long warehouseId) {
        return getBaseUnit(warehouseId, UnitType.WEIGHT);
    }
    public Unit getVolumeBaseUnit(Long warehouseId) {
        return getBaseUnit(warehouseId, UnitType.VOLUME);
    }
}

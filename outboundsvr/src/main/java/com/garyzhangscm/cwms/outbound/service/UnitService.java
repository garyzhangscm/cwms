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

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.HualeiRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.Unit;
import com.garyzhangscm.cwms.outbound.model.UnitType;
import com.garyzhangscm.cwms.outbound.model.hualei.*;
import com.garyzhangscm.cwms.outbound.repository.HualeiShipmentRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class UnitService {
    private static final Logger logger = LoggerFactory.getLogger(UnitService.class);

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public List<Unit> getAllUnits(Long warehouseId) {

        return commonServiceRestemplateClient.getAllUnits(warehouseId);
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
        return convert(warehouseId, length, UnitType.LENGTH, sourceUnitName, destinationUnitName);
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
        return convert(warehouseId, weight, UnitType.WEIGHT, sourceUnitName, destinationUnitName);
    }
    public double convertVolume(Long warehouseId,
                                double volume,
                                String sourceUnitName,
                                String destinationUnitName) {
        logger.debug("Start to convert volume {} from unit {} to unit {}",
                volume,
                Strings.isBlank(sourceUnitName) ? "N/A" : sourceUnitName,
                Strings.isBlank(destinationUnitName) ? "N/A" : destinationUnitName);
        return convert(warehouseId, volume, UnitType.VOLUME, sourceUnitName, destinationUnitName);
    }
    public double convert(Long warehouseId, double value, UnitType type,
                          String sourceUnitName, String destinationUnitName) {
        List<Unit> units = getAllUnits(warehouseId).stream().filter(
                unit -> unit.getType().equals(type)
        ).collect(Collectors.toList());

        return convert(value, sourceUnitName, destinationUnitName, units);
    }


    public double convert(double sourceValue,
                          String sourceUnitName,
                          String destinationUnitName,
                          List<Unit> units) {

        Unit baseUnit = getBaseUnit(units);
        Unit sourceUnit = Strings.isBlank(sourceUnitName) ?
                baseUnit : getUnitByName(units, sourceUnitName);

        Unit destinationUnit = Strings.isBlank(destinationUnitName) ?
                baseUnit : getUnitByName(units, destinationUnitName);

        double result = sourceValue * sourceUnit.getRatio() / destinationUnit.getRatio();

        logger.debug("value {} is convert from source unit {} to destination unit {}, result is {}",
                sourceValue, sourceUnit.getName(), destinationUnit.getName(),
                result);
        return result;

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
    public Unit getBaseUnit(Long warehouseId, UnitType unitType) {
        List<Unit> units = getAllUnits(warehouseId).stream().filter(
                unit -> unit.getType().equals(unitType)
        ).collect(Collectors.toList());

        return getBaseUnit(units);
    }
    /**
     * Get the base unit from a list of unit of same type
     * @param units
     * @return
     */
    public Unit getBaseUnit(List<Unit> units) {

        Unit baseLengthUnit = units.stream().filter(
                unit -> Boolean.TRUE.equals(unit.getBaseUnitFlag())
        ).findFirst().orElse(null);

        if (Objects.nonNull(baseLengthUnit)) {
            return  baseLengthUnit;
        }
        return units.stream().filter(
                unit -> unit.getRatio() == 1
        ).findFirst().orElse(null);
    }

    public Unit getUnitByName(List<Unit> units, String name) {
        return units.stream().filter(unit -> unit.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }


}

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
import com.garyzhangscm.cwms.inventory.model.Unit;
import com.garyzhangscm.cwms.inventory.model.UnitType;
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

        return convert(warehouseId, quantity, sourceUnit, destinationUnit);
    }

    public double convert(Long warehouseId,
                          double quantity,
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

    public Unit getBaseUnit(Long warehouseId, UnitType type) {
        return getAllUnits(warehouseId).stream().filter(
                unit -> (Boolean.TRUE.equals(unit.getBaseUnitFlag()) ||
                            unit.getRatio() == 1.0
                        )
                        && type.equals(unit.getType())
        ).findFirst().orElse(null);
    }

}

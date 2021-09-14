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

package com.garyzhangscm.cwms.common.controller;

import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.UnitOfMeasure;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.UnitOfMeasureService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
public class UnitOfMeasureController {
    @Autowired
    UnitOfMeasureService unitOfMeasureService;

    @RequestMapping(value="/unit-of-measures", method = RequestMethod.GET)
    public List<UnitOfMeasure> findAllUnitOfMeasures(@RequestParam Long warehouseId,
                                                     @RequestParam(required = false, name = "name", defaultValue = "") String name) {
        if (StringUtils.isNotBlank(name)) {
            return Collections.singletonList(unitOfMeasureService.findByName(warehouseId, name));
        }
        return unitOfMeasureService.findAll(warehouseId);
    }


    @RequestMapping(value="/unit-of-measures/{id}", method = RequestMethod.GET)
    public UnitOfMeasure findUnitOfMeasure(@PathVariable Long id) {
        return unitOfMeasureService.findById(id);
    }



    @BillableEndpoint
    @RequestMapping(value="/unit-of-measures", method = RequestMethod.POST)
    public UnitOfMeasure addUnitOfMeasure(@RequestBody UnitOfMeasure unitOfMeasure) {
        return unitOfMeasureService.save(unitOfMeasure);
    }

    @BillableEndpoint
    @RequestMapping(value="/unit-of-measures/{id}", method = RequestMethod.PUT)
    public UnitOfMeasure changeUnitOfMeasure(@PathVariable Long id, @RequestBody UnitOfMeasure unitOfMeasure) {
        if (unitOfMeasure.getId() != null && unitOfMeasure.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; unitOfMeasure.getId(): " + unitOfMeasure.getId());
        }
        return unitOfMeasureService.save(unitOfMeasure);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/unit-of-measures")
    public void removeUnitOfMeasures(@RequestParam(name = "unitOfMeasureIds", required = false, defaultValue = "") String unitOfMeasureIds) {
        unitOfMeasureService.delete(unitOfMeasureIds);
    }
}

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

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.UnitOfMeasure;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.UnitOfMeasureService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
public class UnitOfMeasureController {
    @Autowired
    UnitOfMeasureService unitOfMeasureService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/unit-of-measures", method = RequestMethod.GET)
    public List<UnitOfMeasure> findAllUnitOfMeasures(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                                     @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                                     @RequestParam(name="companyItem", required = false, defaultValue = "") Boolean companyUnitOfMeasure,
                                                     @RequestParam(name="warehouseSpecificItem", required = false, defaultValue = "") Boolean warehouseSpecificUnitOfMeasure,
                                                     @RequestParam(required = false, name = "name", defaultValue = "") String name) {

        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }
        return unitOfMeasureService.findAll(companyId, warehouseId, name, companyUnitOfMeasure, warehouseSpecificUnitOfMeasure);
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
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_UnitOfMeasure", allEntries = true),
            }
    )
    public UnitOfMeasure changeUnitOfMeasure(@PathVariable Long id, @RequestBody UnitOfMeasure unitOfMeasure) {
        if (unitOfMeasure.getId() != null && unitOfMeasure.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; unitOfMeasure.getId(): " + unitOfMeasure.getId());
        }
        return unitOfMeasureService.save(unitOfMeasure);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/unit-of-measures")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_UnitOfMeasure", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_UnitOfMeasure", allEntries = true),
            }
    )
    public void removeUnitOfMeasures(@RequestParam(name = "unitOfMeasureIds", required = false, defaultValue = "") String unitOfMeasureIds) {
        unitOfMeasureService.delete(unitOfMeasureIds);
    }
}

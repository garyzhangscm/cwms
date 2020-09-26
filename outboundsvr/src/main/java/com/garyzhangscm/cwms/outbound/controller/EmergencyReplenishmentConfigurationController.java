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

package com.garyzhangscm.cwms.outbound.controller;


import com.garyzhangscm.cwms.outbound.model.EmergencyReplenishmentConfiguration;
import com.garyzhangscm.cwms.outbound.service.EmergencyReplenishmentConfigurationService;
import com.garyzhangscm.cwms.outbound.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class EmergencyReplenishmentConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);

    @Autowired
    private EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService;

    @RequestMapping(value="/emergency-replenishment-configuration", method = RequestMethod.GET)
    public List<EmergencyReplenishmentConfiguration> getAllEmergencyReplenishmentConfiguration(
                                @RequestParam Long warehouseId,
                                @RequestParam(name = "sequence", required = false, defaultValue =  "") Integer sequence,
                                @RequestParam(name = "unitOfMeasureId", required = false, defaultValue =  "") Long unitOfMeasureId,
                                @RequestParam(name = "itemId", required = false, defaultValue =  "") Long itemId,
                                @RequestParam(name = "itemFamilyId", required = false, defaultValue =  "") Long itemFamilyId,
                                @RequestParam(name = "sourceLocationId", required = false, defaultValue =  "") Long sourceLocationId,
                                @RequestParam(name = "sourceLocationGroupId", required = false, defaultValue =  "") Long sourceLocationGroupId,
                                @RequestParam(name = "destinationLocationId", required = false, defaultValue =  "") Long destinationLocationId,
                                @RequestParam(name = "destinationLocationGroupId", required = false, defaultValue =  "") Long destinationLocationGroupId) {
        return emergencyReplenishmentConfigurationService.findAll(warehouseId, sequence,
                unitOfMeasureId, itemId, itemFamilyId, sourceLocationId, sourceLocationGroupId,
                destinationLocationId, destinationLocationGroupId);
    }


    @RequestMapping(value="/emergency-replenishment-configuration", method = RequestMethod.POST)
    public EmergencyReplenishmentConfiguration addAllocationConfiguration(@RequestBody EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {

        logger.debug("Will save emergency replenishment configuration\n{}", emergencyReplenishmentConfiguration);
        return emergencyReplenishmentConfigurationService.saveOrUpdate(emergencyReplenishmentConfiguration);
    }

}

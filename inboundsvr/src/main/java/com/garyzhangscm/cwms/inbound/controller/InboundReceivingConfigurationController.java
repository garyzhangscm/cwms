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

package com.garyzhangscm.cwms.inbound.controller;


import com.garyzhangscm.cwms.inbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.inbound.model.InboundQCConfiguration;
import com.garyzhangscm.cwms.inbound.model.InboundReceivingConfiguration;
import com.garyzhangscm.cwms.inbound.service.InboundQCConfigurationService;
import com.garyzhangscm.cwms.inbound.service.InboundReceivingConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class InboundReceivingConfigurationController {
    @Autowired
    InboundReceivingConfigurationService inboundReceivingConfigurationService;

    @RequestMapping(value="/inbound-receiving-configuration", method = RequestMethod.GET)
    public List<InboundReceivingConfiguration> findAllInboundReceivingConfiguration(
            @RequestParam Long companyId,
            @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
            @RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
            ) {
        return inboundReceivingConfigurationService.findAll(supplierId, itemFamilyId,  itemId,  warehouseId, companyId);
    }

    @RequestMapping(value="/inbound-receiving-configuration/best-match", method = RequestMethod.GET)
    public InboundReceivingConfiguration findBestMatchInboundReceivingConfiguration(
            @RequestParam Long companyId,
            @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
            @RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
    ) {
        return inboundReceivingConfigurationService.getBestMatchedInboundReceivingConfiguration(supplierId, itemFamilyId,  itemId,  warehouseId, companyId);
    }



    @BillableEndpoint
    @RequestMapping(value="/inbound-receiving-configuration", method = RequestMethod.PUT)
    public InboundReceivingConfiguration addInboundReceivingConfiguration(@RequestBody InboundReceivingConfiguration inboundReceivingConfiguration) {
        return inboundReceivingConfigurationService.addInboundReceivingConfiguration(inboundReceivingConfiguration);
    }


    @RequestMapping(value="/inbound-receiving-configuration/{id}", method = RequestMethod.GET)
    public InboundReceivingConfiguration findInboundReceivingConfiguration(@PathVariable Long id) {

        return inboundReceivingConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inbound-receiving-configuration/{id}", method = RequestMethod.POST)
    public InboundReceivingConfiguration changeInboundReceivingConfiguration(@PathVariable Long id,
                                               @RequestBody InboundReceivingConfiguration inboundReceivingConfiguration){
        return inboundReceivingConfigurationService.changeInboundReceivingConfiguration(id, inboundReceivingConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/inbound-receiving-configuration/{id}", method = RequestMethod.DELETE)
    public void removeInboundReceivingConfiguration(@PathVariable Long id) {

        inboundReceivingConfigurationService.delete(id);
    }


}

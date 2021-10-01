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
import com.garyzhangscm.cwms.inbound.service.InboundQCConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class InboundQCConfigurationController {
    @Autowired
    InboundQCConfigurationService inboundQCConfigurationService;

    @RequestMapping(value="/inbound-qc-configuration", method = RequestMethod.GET)
    public List<InboundQCConfiguration> findAllInboundQCConfiguration(
            @RequestParam Long companyId,
            @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
            ) {
        return inboundQCConfigurationService.findAll(supplierId, itemId, warehouseId, companyId);
    }

    @BillableEndpoint
    @RequestMapping(value="/inbound-qc-configuration", method = RequestMethod.PUT)
    public InboundQCConfiguration addInboundQCConfiguration(@RequestBody InboundQCConfiguration inboundQCConfiguration) {
        return inboundQCConfigurationService.addInboundQCConfiguration(inboundQCConfiguration);
    }


    @RequestMapping(value="/inbound-qc-configuration/{id}", method = RequestMethod.GET)
    public InboundQCConfiguration findInboundQCConfiguration(@PathVariable Long id) {

        return inboundQCConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inbound-qc-configuration/{id}", method = RequestMethod.POST)
    public InboundQCConfiguration changeInboundQCConfiguration(@PathVariable Long id,
                                               @RequestBody InboundQCConfiguration inboundQCConfiguration){
        return inboundQCConfigurationService.changeInboundQCConfiguration(id, inboundQCConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/inbound-qc-configuration/{id}", method = RequestMethod.DELETE)
    public void removeInboundQCConfiguration(@PathVariable Long id) {

        inboundQCConfigurationService.delete(id);
    }


}

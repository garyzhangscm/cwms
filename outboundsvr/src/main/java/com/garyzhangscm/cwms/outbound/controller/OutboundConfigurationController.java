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


import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.OutboundConfiguration;
import com.garyzhangscm.cwms.outbound.service.OutboundConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class OutboundConfigurationController {

    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

    @RequestMapping(value="/outbound-configuration", method = RequestMethod.GET)
    public OutboundConfiguration getOutboundConfiguration(
                                @RequestParam Long warehouseId) {
        return outboundConfigurationService.findByWarehouse(warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/outbound-configuration", method = RequestMethod.PUT)
    public OutboundConfiguration addOutboundConfiguration(
            @RequestParam Long warehouseId,
            @RequestBody OutboundConfiguration outboundConfiguration) {
        return outboundConfigurationService.addOutboundConfiguration(outboundConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/outbound-configuration/{id}", method = RequestMethod.POST)
    public OutboundConfiguration changeOutboundConfiguration(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody OutboundConfiguration outboundConfiguration) {
        return outboundConfigurationService.changeOutboundConfiguration(id, outboundConfiguration);
    }



}

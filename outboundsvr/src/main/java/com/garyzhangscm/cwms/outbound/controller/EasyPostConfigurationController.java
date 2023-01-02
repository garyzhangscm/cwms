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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.EasyPostConfigurationService;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EasyPostConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(EasyPostConfigurationController.class);

    @Autowired
    EasyPostConfigurationService easyPostConfigurationService;


    @RequestMapping(value="/easy-post-configuration", method = RequestMethod.GET)
    public EasyPostConfiguration findConfiguration(@RequestParam Long warehouseId) {
        return easyPostConfigurationService.findByWarehouseId(warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/easy-post-configuration", method = RequestMethod.PUT)
    public EasyPostConfiguration addConfiguration(@RequestParam Long warehouseId,
                                                  @RequestBody EasyPostConfiguration easyPostConfiguration) {
        return easyPostConfigurationService.addConfiguration(easyPostConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/easy-post-configuration/{id}", method = RequestMethod.POST)
    public EasyPostConfiguration changeConfiguration(@RequestParam Long warehouseId,
                                                     @RequestBody EasyPostConfiguration easyPostConfiguration){
        return easyPostConfigurationService.changeConfiguration(easyPostConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/easy-post-configuration/{id}/carriers", method = RequestMethod.PUT)
    public EasyPostCarrier addCarrier(@PathVariable Long id,
                                      @RequestParam Long warehouseId,
                                       @RequestBody EasyPostCarrier easyPostCarrier){
        return easyPostConfigurationService.addCarrier(id, easyPostCarrier);
    }

    @BillableEndpoint
    @RequestMapping(value="/easy-post-configuration/{id}/carriers/{carrierId}", method = RequestMethod.POST)
    public EasyPostCarrier changeCarrier(@PathVariable Long id,
                                         @PathVariable Long carrierId,
                                         @RequestParam Long warehouseId,
                                         @RequestBody EasyPostCarrier easyPostCarrier){
        return easyPostConfigurationService.addCarrier(id, easyPostCarrier);
    }

    @BillableEndpoint
    @RequestMapping(value="/easy-post-configuration/{id}/carriers/{carrierId}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper removeCarrier(@PathVariable Long id,
                                             @RequestParam Long warehouseId,
                                             @PathVariable Long carrierId){
        easyPostConfigurationService.removeCarrier(carrierId);
        return ResponseBodyWrapper.success("Carrier " + carrierId  + " is removed");
    }

}

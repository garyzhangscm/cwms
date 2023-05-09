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

import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.service.CarrierService;
import com.garyzhangscm.cwms.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class CarrierController {
    @Autowired
    CarrierService carrierService;

    @RequestMapping(value="/carriers", method = RequestMethod.GET)
    public List<Carrier> findAllCarriers(@RequestParam Long warehouseId,
                                         @RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return carrierService.findAll(warehouseId, name);
    }

    @RequestMapping(value="/carriers/{id}", method = RequestMethod.GET)
    public Carrier findCarrier(@PathVariable Long id) {
        return carrierService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/carriers", method = RequestMethod.POST)
    public Carrier addCarrier(@RequestBody Carrier carrier) {
        return carrierService.addCarrier(carrier);
    }

    @BillableEndpoint
    @RequestMapping(value="/carriers/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Carrier", allEntries = true),
            }
    )
    public Carrier changeCarrier(@PathVariable Long id, @RequestBody Carrier carrier) {
        if (Objects.nonNull(carrier.getId()) && !Objects.equals(carrier.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; carrier.getId(): " + carrier.getId());
        }
        return carrierService.changeCarrier(carrier);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/carriers")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Carrier", allEntries = true),
            }
    )
    public void deleteCarriers(@RequestParam(name = "carrier_ids", required = false, defaultValue = "") String carrierIds) {
        carrierService.delete(carrierIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/carriers/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Carrier", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Carrier", allEntries = true),
            }
    )
    public ResponseBodyWrapper<String> removeCarrier(@PathVariable Long id,
                                             @RequestParam Long warehouseId) {
        carrierService.removeCarrier(id);
        return ResponseBodyWrapper.success("carrier with id " + id + " is removed");
    }


    @BillableEndpoint
    @RequestMapping(value="/carriers/{id}/disable", method = RequestMethod.POST)
    public Carrier disableCarrier(@PathVariable Long id) {
        return carrierService.disableCarrier(id);
    }
    @BillableEndpoint
    @RequestMapping(value="/carriers/{id}/enable", method = RequestMethod.POST)
    public Carrier enableCarrier(@PathVariable Long id) {
        return carrierService.enableCarrier(id);
    }
}

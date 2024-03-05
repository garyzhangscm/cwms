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


import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.ListPickConfiguration;
import com.garyzhangscm.cwms.outbound.service.ListPickConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class ListPickConfigurationController {

    @Autowired
    private ListPickConfigurationService listPickConfigurationService;

    @RequestMapping(value="/list-pick-configuration", method = RequestMethod.GET)
    public List<ListPickConfiguration> getListPickConfiguration(
                                @RequestParam Long warehouseId,
                                @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                @RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
                                @RequestParam(name="customerId", required = false, defaultValue = "") Long customerId,
                                @RequestParam(name="customerName", required = false, defaultValue = "") String customerName) {
        return listPickConfigurationService.findAll(warehouseId, clientId,
                clientName, customerId, customerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/list-pick-configuration", method = RequestMethod.PUT)
    public ListPickConfiguration addListPickConfiguration(@RequestBody ListPickConfiguration listPickConfiguration) {
        return listPickConfigurationService.addListPickConfiguration(listPickConfiguration);
    }

    @RequestMapping(value="/list-pick-configuration/{id}", method = RequestMethod.GET)
    public ListPickConfiguration getListPickConfiguration(
                                @PathVariable Long id,
                                @RequestParam Long warehouseId) {
        return listPickConfigurationService.findById(id);
    }
    @RequestMapping(value="/list-pick-configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeListPickConfiguration(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        listPickConfigurationService.removeListPickConfiguration(id);
        return ResponseBodyWrapper.success("list pick configuration with id " + id + " is removed");
    }

    @BillableEndpoint
    @RequestMapping(value="/list-pick-configuration/{id}", method = RequestMethod.POST)
    public ListPickConfiguration changeListPickConfiguration(
            @PathVariable Long id,
            @RequestBody ListPickConfiguration listPickConfiguration) {
        return listPickConfigurationService.changeListPickConfiguration(id, listPickConfiguration);
    }



}

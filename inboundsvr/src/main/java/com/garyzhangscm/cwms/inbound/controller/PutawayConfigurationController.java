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

import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.model.Inventory;
import com.garyzhangscm.cwms.inbound.model.PutawayConfiguration;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptLine;
import com.garyzhangscm.cwms.inbound.service.PutawayConfigurationService;
import com.garyzhangscm.cwms.inbound.service.ReceiptLineService;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class PutawayConfigurationController {
    @Autowired
    PutawayConfigurationService putawayConfigurationService;


    @RequestMapping(value="/putaway-configuration", method = RequestMethod.GET)
    public List<PutawayConfiguration> findAllPutawayConfiguration(@RequestParam String warehouseName,
                                                                  @RequestParam(name="sequence", required = false, defaultValue = "") Integer sequence,
                                                                  @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                                  @RequestParam(name="itemFamilyName", required = false, defaultValue = "") String itemFamilyName,
                                                                  @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId) {
        return putawayConfigurationService.findAll(warehouseName, sequence, itemName, itemFamilyName, inventoryStatusId);
    }

    @RequestMapping(value="/putaway-configuration", method = RequestMethod.POST)
    public PutawayConfiguration addPutawayConfiguration(@RequestBody PutawayConfiguration putawayConfiguration) {
        return putawayConfigurationService.save(putawayConfiguration);
    }


    @RequestMapping(value="/putaway-configuration/{id}", method = RequestMethod.GET)
    public PutawayConfiguration findPutawayConfiguration(@PathVariable Long id) {
        return putawayConfigurationService.findById(id);
    }

    @RequestMapping(value="/putaway-configuration/{id}", method = RequestMethod.PUT)
    public PutawayConfiguration changePutawayConfiguration(@RequestBody PutawayConfiguration putawayConfiguration){
        return putawayConfigurationService.save(putawayConfiguration);
    }

    @RequestMapping(value="/putaway-configuration", method = RequestMethod.DELETE)
    public void removeReceipts(@RequestParam(name = "putaway_configuration_ids", required = false, defaultValue = "") String putawayConfigurationIds) {
        putawayConfigurationService.delete(putawayConfigurationIds);
    }


    @RequestMapping(value="/putaway-configuration/allocate-location", method = RequestMethod.POST)
    public Inventory allocateLocation(@RequestBody Inventory inventory) {
        try {
            Inventory allocatedInventory = putawayConfigurationService.allocateLocation(inventory);
            return allocatedInventory;
        } catch (Exception ex) {
            throw new GenericException(99999, ex.getMessage());
        }
    }





}

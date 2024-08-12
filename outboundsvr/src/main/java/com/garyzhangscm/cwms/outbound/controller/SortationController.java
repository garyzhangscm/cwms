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

import com.garyzhangscm.cwms.outbound.model.Inventory;
import com.garyzhangscm.cwms.outbound.model.Sortation;
import com.garyzhangscm.cwms.outbound.model.SortationByShipment;
import com.garyzhangscm.cwms.outbound.model.SortationByShipmentLine;
import com.garyzhangscm.cwms.outbound.service.SortationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SortationController {
    @Autowired
    SortationService sortationService;

    @RequestMapping(value="/sortation/by-wave", method = RequestMethod.GET)
    public Sortation getSortationByWaveNumber(@RequestParam Long warehouseId,
                                              @RequestParam String waveNumber,
                                              @RequestParam Long locationId) {
        return sortationService.getByWaveNumber(warehouseId, waveNumber, locationId);
    }

    /**
     * Find matched inventory by item. When the user scan in item from web, we will need to check
     * if there's multiple inventory matched with the item and if there's different attribute
     * * If there's only single inventory attribute, then we will choose any shipment line
     *   to sort
     * * if there're multiple inventory attribute, then we will ask the user to choose the inventory
     *   attribute so that we can find the correct shipment line to sort
     * @param warehouseId
     * @param waveNumber
     * @param itemId
     * @return
     */
    @RequestMapping(value="/sortation/by-wave/picked-inventory-by-item", method = RequestMethod.POST)
    public List<Inventory> findPickedInventoryByItem(@RequestParam Long warehouseId,
                                                     @RequestParam String waveNumber,
                                                     @RequestParam Long itemId,
                                                     @RequestParam Long locationId) {
        return sortationService.findPickedInventoryByItem(warehouseId, waveNumber, itemId, locationId);
    }


    @RequestMapping(value="/sortation/by-wave/by-item", method = RequestMethod.POST)
    public SortationByShipmentLine processWaveSortationByItem(@RequestParam Long warehouseId,
                                                          @RequestParam String number,
                                                          @RequestParam Long itemId,
                                                              @RequestParam(name = "quantity", required = false, defaultValue = "1") Long quantity) {
        return sortationService.processWaveSortationByItem(warehouseId, number, itemId, quantity);
    }

    @RequestMapping(value="/sortation/by-shipment/{id}", method = RequestMethod.GET)
    public SortationByShipment getSortationByShipment(@PathVariable Long id,
                                                      @RequestParam Long warehouseId) {
        return sortationService.getSortationByShipment(id);
    }

    @RequestMapping(value="/sortation/by-shipment-line/{id}", method = RequestMethod.POST)
    public SortationByShipmentLine processShipmentLineSortationById(@PathVariable Long id,
                                                                @RequestParam Long warehouseId,
                                                                @RequestParam(name = "quantity", required = false, defaultValue = "1") Long quantity) {
        return sortationService.processShipmentLineSortationById(id, quantity);
    }

}

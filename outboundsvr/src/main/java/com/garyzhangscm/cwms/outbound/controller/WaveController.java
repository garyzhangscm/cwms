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

import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.model.Shipment;
import com.garyzhangscm.cwms.outbound.model.Wave;
import com.garyzhangscm.cwms.outbound.service.ShipmentService;
import com.garyzhangscm.cwms.outbound.service.WaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WaveController {
    @Autowired
    WaveService waveService;

    @RequestMapping(value="/waves", method = RequestMethod.GET)
    public List<Wave> findAllWaves(@RequestParam(name="number", required = false, defaultValue = "") String number) {
        return waveService.findAll(number);
    }
    @RequestMapping(value="/waves/candidate", method = RequestMethod.GET)
    public List<Order> findWaveCandidate(@RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                         @RequestParam(name="customerName", required = false, defaultValue = "") String customerName) {
        return waveService.findWaveCandidate(orderNumber, customerName);
    }

    @RequestMapping(value="/waves", method = RequestMethod.POST)
    public Wave addWave(@RequestBody Wave wave) {
        return waveService.save(wave);
    }

    @RequestMapping(value="/waves/plan", method = RequestMethod.POST)
    public Wave planWave(@RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                         @RequestParam Long warehouseId,
                         @RequestBody List<Long> orderLineIds) {
        return waveService.planWave(warehouseId, waveNumber, orderLineIds);
    }

    @RequestMapping(value="/waves/{id}/allocate", method = RequestMethod.POST)
    public Wave allocateWave(@PathVariable Long id) {
        return waveService.allocateWave(id);
    }

    @RequestMapping(value="/waves/{id}", method = RequestMethod.GET)
    public Wave findWave(@PathVariable Long id) {
        return waveService.findById(id);
    }

    @RequestMapping(value="/waves/{id}", method = RequestMethod.PUT)
    public Wave changeWave(@RequestBody Wave wave){
        return waveService.save(wave);
    }

    @RequestMapping(value="/waves", method = RequestMethod.DELETE)
    public void removeWaves(@RequestParam(name = "wave_ids", required = false, defaultValue = "") String waveIds) {
        waveService.delete(waveIds);
    }


}

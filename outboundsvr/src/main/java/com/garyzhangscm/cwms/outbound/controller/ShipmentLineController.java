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

import com.garyzhangscm.cwms.outbound.model.ShipmentLine;
import com.garyzhangscm.cwms.outbound.service.ShipmentLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShipmentLineController {
    @Autowired
    ShipmentLineService shipmentLineService;



    @RequestMapping(value="/shipment-lines", method = RequestMethod.GET)
    public List<ShipmentLine> findAllShipmentLines(@RequestParam Long warehouseId,
                                                   @RequestParam(name="number", required = false, defaultValue = "") String number,
                                                   @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                                   @RequestParam(name="orderLineId", required = false, defaultValue = "") Long orderLineId,
                                                   @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                                   @RequestParam(name="waveId", required = false, defaultValue = "") Long waveId,
                                                   @RequestParam(name="orderLineIds", required = false, defaultValue = "") String orderLineIds,
                                                   @RequestParam(name="shipmentLineIds", required = false, defaultValue = "") String shipmentLineIds) {
        return shipmentLineService.findAll(warehouseId, number, orderNumber, orderLineId, orderId, waveId, orderLineIds, shipmentLineIds);
    }



}

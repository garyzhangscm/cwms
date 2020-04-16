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
import com.garyzhangscm.cwms.outbound.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShipmentController {
    @Autowired
    ShipmentService shipmentService;



    @RequestMapping(value="/shipments", method = RequestMethod.GET)
    public List<Shipment> findAllShipments(@RequestParam Long warehouseId,
                                           @RequestParam(name="number", required = false, defaultValue = "") String number,
                                           @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                           @RequestParam(name="stopId", required = false, defaultValue = "") Long stopId,
                                           @RequestParam(name="trailerId", required = false, defaultValue = "") Long trailerId) {
        return shipmentService.findAll(warehouseId, number, orderNumber, stopId, trailerId);
    }

    @RequestMapping(value="/shipments", method = RequestMethod.POST)
    public Shipment addShipments(@RequestBody Shipment shipment) {
        return shipmentService.save(shipment);
    }


    @RequestMapping(value="/shipments/{id}", method = RequestMethod.GET)
    public Shipment findShipment(@PathVariable Long id) {
        return shipmentService.findById(id);
    }

    @RequestMapping(value="/shipments/{id}", method = RequestMethod.PUT)
    public Shipment changeShipment(@RequestBody Shipment shipment){
        return shipmentService.save(shipment);
    }

    @RequestMapping(value="/shipments", method = RequestMethod.DELETE)
    public void removeShipments(@RequestParam(name = "shipment_ids", required = false, defaultValue = "") String shipmentIds) {
        shipmentService.delete(shipmentIds);
    }

    // Plan a list of order lines into shipments. One order per shipment.
    // We will pick / ship based upon shipments.
    @RequestMapping(value="/shipments/plan",  method = RequestMethod.POST)
    public List<Shipment> planShipmetns(@RequestParam Long warehouseId, @RequestBody List<OrderLine> orderLines){
        return shipmentService.planShipments(warehouseId, orderLines);
    }

    @RequestMapping(value="/shipments/{id}/complete", method = RequestMethod.PUT)
    public Shipment completeShipment(@PathVariable Long id){

        return shipmentService.autoCompleteShipment(id);
    }

    @RequestMapping(value="/shipments/{id}/stage", method = RequestMethod.POST)
    public Shipment stageShipment(@PathVariable Long id,
                            @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return shipmentService.stage(id, ignoreUnfinishedPicks);
    }

    @RequestMapping(value="/shipments/{id}/load", method = RequestMethod.POST)
    public Shipment loadShipment(@PathVariable Long id,
                           @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return shipmentService.load(id, ignoreUnfinishedPicks);
    }

    @RequestMapping(value="/shipments/{id}/dispatch", method = RequestMethod.POST)
    public Shipment dispatchShipment(@PathVariable Long id,
                               @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return shipmentService.dispatch(id, ignoreUnfinishedPicks);
    }

    @RequestMapping(value="/shipments/{id}/allocate", method = RequestMethod.PUT)
    public Shipment allocateShipment(@PathVariable Long id){

        return shipmentService.allocateShipment(id);
    }
}

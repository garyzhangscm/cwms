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
import com.garyzhangscm.cwms.outbound.service.ShipmentService;
import com.garyzhangscm.cwms.outbound.service.WaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class WaveController {
    @Autowired
    WaveService waveService;

    @RequestMapping(value="/waves", method = RequestMethod.GET)
    public List<Wave> findAllWaves(@RequestParam Long warehouseId,
                                   @RequestParam(name="number", required = false, defaultValue = "") String number,
                                   @RequestParam(name="waveStatus", required = false, defaultValue = "") String waveStatus,
                                   @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
                                   @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime,
                                   @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(name = "includeCompletedWave", required = false, defaultValue = "false") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Boolean includeCompletedWave,
                                   @RequestParam(name = "includeCancelledWave", required = false, defaultValue = "false") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Boolean includeCancelledWave,
                                   @RequestParam(name = "loadAttribute", required = false, defaultValue = "false") Boolean loadAttribute) {
        return waveService.findAll(warehouseId, number, waveStatus, startTime, endTime, date, includeCompletedWave, includeCancelledWave, loadAttribute);
    }

    @ClientValidationEndpoint
    @RequestMapping(value="/waves/candidate/orders", method = RequestMethod.GET)
    public List<Order> findWaveableOrdersCandidate(@RequestParam Long warehouseId,
                                         @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                         @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                         @RequestParam(name="customerName", required = false, defaultValue = "") String customerName,
                                         @RequestParam(name="customerId", required = false, defaultValue = "") Long customerId,
                                         @RequestParam(name="singleOrderLineOnly", required = false, defaultValue = "") Boolean singleOrderLineOnly,
                                         @RequestParam(name="singleOrderQuantityOnly", required = false, defaultValue = "") Boolean singleOrderQuantityOnly,
                                         @RequestParam(name="singleOrderCaseQuantityOnly", required = false, defaultValue = "") Boolean singleOrderCaseQuantityOnly,
                                         @RequestParam(name = "startCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startCreatedTime,
                                         @RequestParam(name = "endCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endCreatedTime,
                                         @RequestParam(name = "specificCreatedDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specificCreatedDate,
                                         ClientRestriction clientRestriction) {
        return waveService.findWaveableOrdersCandidate(warehouseId,
                orderNumber,  clientId, customerName, customerId,
                startCreatedTime,  endCreatedTime, specificCreatedDate,
                singleOrderLineOnly, singleOrderQuantityOnly, singleOrderCaseQuantityOnly,
                clientRestriction);
    }

    @ClientValidationEndpoint
    @RequestMapping(value="/waves/candidate/shipments", method = RequestMethod.GET)
    public List<Shipment> findWaveableShipmentsCandidate(@RequestParam Long warehouseId,
                                                  @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                                  @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                                  @RequestParam(name="customerName", required = false, defaultValue = "") String customerName,
                                                  @RequestParam(name="customerId", required = false, defaultValue = "") Long customerId,
                                                  @RequestParam(name="singleOrderLineOnly", required = false, defaultValue = "") Boolean singleOrderLineOnly,
                                                  @RequestParam(name="singleOrderQuantityOnly", required = false, defaultValue = "") Boolean singleOrderQuantityOnly,
                                                  @RequestParam(name="singleOrderCaseQuantityOnly", required = false, defaultValue = "") Boolean singleOrderCaseQuantityOnly,
                                                  @RequestParam(name = "startCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startCreatedTime,
                                                  @RequestParam(name = "endCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endCreatedTime,
                                                  @RequestParam(name = "specificCreatedDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specificCreatedDate,
                                                  ClientRestriction clientRestriction) {
        return waveService.findWaveableShipmentsCandidate(warehouseId,
                orderNumber,  clientId, customerName, customerId,
                startCreatedTime,  endCreatedTime, specificCreatedDate,
                singleOrderLineOnly, singleOrderQuantityOnly, singleOrderCaseQuantityOnly,
                clientRestriction);
    }


    @BillableEndpoint
    @RequestMapping(value="/waves", method = RequestMethod.POST)
    public Wave addWave(@RequestBody Wave wave) {
        return waveService.save(wave);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/plan", method = RequestMethod.POST)
    public Wave planWave(@RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                         @RequestParam Long warehouseId,
                         @RequestBody List<Long> orderLineIds) {
        return waveService.planWave(warehouseId, waveNumber, orderLineIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/allocate", method = RequestMethod.POST)
    public Wave allocateWave(@PathVariable Long id,
                             @RequestParam(name = "asynchronous", required = false, defaultValue = "") Boolean asynchronous) {
        return waveService.allocateWave(id, asynchronous);
    }

    @RequestMapping(value="/waves/{id}", method = RequestMethod.GET)
    public Wave findWave(@PathVariable Long id) {
        return waveService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}", method = RequestMethod.PUT)
    public Wave changeWave(@RequestBody Wave wave){
        return waveService.save(wave);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves", method = RequestMethod.DELETE)
    public void removeWaves(@RequestParam(name = "wave_ids", required = false, defaultValue = "") String waveIds) {
        waveService.delete(waveIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/cancel", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> cancelWave(@PathVariable Long id) {
        waveService.cancelWave(id);
        return ResponseBodyWrapper.success("WAVE " + id + " is cancelled");
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/complete", method = RequestMethod.POST)
    public Wave completeWave(@PathVariable Long id) {
        return waveService.completeWave(id);
    }



    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generateWavePickReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale)  {

        return waveService.generateWavePickReport(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/packing-slip", method = RequestMethod.POST)
    public ReportHistory generateWavePackingSlip(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale)  {

        return waveService.generateWavePackingSlip(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/staged-inventory", method = RequestMethod.GET)
    public List<Inventory> getStagedInventory(
            @PathVariable Long id)  {

        return waveService.getStagedInventory(id);
    }
}

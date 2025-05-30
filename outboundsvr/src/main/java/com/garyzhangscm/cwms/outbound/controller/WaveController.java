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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.WaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class WaveController {
    @Autowired
    WaveService waveService;

    @RequestMapping(value="/waves", method = RequestMethod.GET)
    public List<Wave> findAllWaves(@RequestParam Long warehouseId,
                                   @RequestParam(name="ids", required = false, defaultValue = "") String ids,
                                   @RequestParam(name="number", required = false, defaultValue = "") String number,
                                   @RequestParam(name="waveStatus", required = false, defaultValue = "") String waveStatus,
                                   @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
                                   @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime,
                                   @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(name = "includeCompletedWave", required = false, defaultValue = "false") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Boolean includeCompletedWave,
                                   @RequestParam(name = "includeCancelledWave", required = false, defaultValue = "false") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Boolean includeCancelledWave,
                                   @RequestParam(name = "loadAttribute", required = false, defaultValue = "false") Boolean loadAttribute) {
        return waveService.findAll(warehouseId, number, waveStatus, startTime, endTime, date, includeCompletedWave, includeCancelledWave, ids, loadAttribute);
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
                                         @RequestParam(name = "startCreatedTime", required = false, defaultValue = "")   String startCreatedTime,
                                         @RequestParam(name = "endCreatedTime", required = false, defaultValue = "") String endCreatedTime,
                                         @RequestParam(name = "specificCreatedDate", required = false, defaultValue = "") String specificCreatedDate,
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
    @RequestMapping(value="/waves", method = RequestMethod.PUT)
    public Wave addWave(@RequestBody Wave wave) {
        return waveService.save(wave);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/plan/by-order-lines", method = RequestMethod.POST)
    public Wave planWaveByOrderLines(@RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                         @RequestParam(name="comment", required = false, defaultValue = "") String comment,
                         @RequestParam Long warehouseId,
                         @RequestBody List<Long> orderLineIds) {
        return waveService.planWaveByOrderLines(warehouseId, waveNumber, orderLineIds, comment);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/plan/by-shipment-lines", method = RequestMethod.POST)
    public Wave planWaveByShipmentLines(@RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                         @RequestParam(name="comment", required = false, defaultValue = "") String comment,
                         @RequestParam Long warehouseId,
                         @RequestBody List<Long> shipmentLineIds) {
        return waveService.planWaveByShipmentLines(warehouseId, waveNumber, shipmentLineIds, comment);
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
    @RequestMapping(value="/waves/{id}", method = RequestMethod.POST)
    public Wave changeWave(@RequestBody Wave wave){
        return waveService.changeWave(wave);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/change-comment", method = RequestMethod.POST)
    public Wave changeWaveComment(@PathVariable Long id,
                                  @RequestParam  String comment,
                                  @RequestParam Long warehouseId){
        return waveService.changeWaveComment(id, comment);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeWaves(@RequestParam(name = "wave_ids", required = false, defaultValue = "") String waveIds) {
        waveService.delete(waveIds);
        return ResponseBodyWrapper.success("WAVEs with ids " + waveIds + " are removed");
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeWave(@PathVariable Long id) {
        waveService.removeWave(id);
        return ResponseBodyWrapper.success("WAVE " + id + " is removed");
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

    /**
     * Pre print packing list before inventory is picked. We will use order's
     * information to print the packing list
     * @param id
     * @param locale
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/pre-print-packing-slip", method = RequestMethod.POST)
    public ReportHistory generatePrePrintWavePackingSlip(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale)  {

        return waveService.generatePrePrintWavePackingSlip(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/staged-inventory", method = RequestMethod.GET)
    public List<Inventory> getStagedInventory(
            @PathVariable Long id)  {

        return waveService.getStagedInventory(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/deassign-shipment-line", method = RequestMethod.POST)
    public Wave deassignShipmentLine(
            @PathVariable Long id,
            @RequestParam Long shipmentLineId)  {

        return waveService.deassignShipmentLine(id, shipmentLineId);
    }

    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/change-load-number", method = RequestMethod.POST)
    public Wave changeWaveLoadNumber(@PathVariable Long id,
                                  @RequestParam  String loadNumber,
                                  @RequestParam Long warehouseId){
        return waveService.changeWaveLoadNumber(id, loadNumber);
    }
    @BillableEndpoint
    @RequestMapping(value="/waves/{id}/change-bol-number", method = RequestMethod.POST)
    public Wave changeWaveBillOfLadingNumber(@PathVariable Long id,
                                     @RequestParam  String billOfLadingNumber,
                                     @RequestParam Long warehouseId){
        return waveService.changeWaveBillOfLadingNumber(id, billOfLadingNumber);
    }

    @RequestMapping(value="/waves/{id}/get-sortation-locations", method = RequestMethod.GET)
    public List<Location> getSortationLocations(@PathVariable Long id,
                                             @RequestParam Long warehouseId){
        return waveService.getSortationLocations(id);
    }


    @RequestMapping(value="/waves/get-staged-inventory/quantity", method = RequestMethod.GET)
    public List<Pair<Long, Long>> getStagedInventoryQuantity(@RequestParam String waveIds,
                                                @RequestParam Long warehouseId){
        return waveService.getStagedInventoryQuantity(warehouseId, waveIds);
    }

}

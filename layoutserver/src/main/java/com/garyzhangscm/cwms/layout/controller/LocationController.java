/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.layout.controller;

import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.BillableEndpoint;
import com.garyzhangscm.cwms.layout.model.Location;
import com.garyzhangscm.cwms.layout.service.FileService;
import com.garyzhangscm.cwms.layout.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    @Autowired
    LocationService locationService;

    @Autowired
    FileService fileService;

    @RequestMapping(value="/locations/{id}", method = RequestMethod.GET)
    public Location getLocationById(@PathVariable Long id) {
        return locationService.findById(id);
    }


    @RequestMapping(value="/locations/logic/{locationType}", method = RequestMethod.GET)
    public Location getLogicalLocation(@PathVariable String locationType,
                                       @RequestParam Long warehouseId) {
        return locationService.findLogicLocation(locationType, warehouseId);
    }

    @RequestMapping(value="/locations/packing-stations", method = RequestMethod.GET)
    public  List<Location> getPackingStations(@RequestParam Long warehouseId) {

        return locationService.getPackingStations(warehouseId);
    }



    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadLocations(Long warehouseId,
                                               @RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);
        List<Location> locations = locationService.loadLocationData(warehouseId, localFile);
        return  ResponseBodyWrapper.success(locations.size() + "");
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations")
    public List<Location> findLocations(@RequestParam Long warehouseId,
                                        @RequestParam(name = "locationGroupTypeIds", required = false, defaultValue = "") String locationGroupTypeIds,
                                        @RequestParam(name = "locationGroupIds", required = false, defaultValue = "") String locationGroupIds,
                                        @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                        @RequestParam(name = "code", required = false, defaultValue = "") String code,
                                        @RequestParam(name = "locationStatus", required = false, defaultValue = "") String locationStatus,
                                        @RequestParam(name = "beginSequence", required = false, defaultValue = "") Long beginSequence,
                                        @RequestParam(name = "endSequence", required = false, defaultValue = "") Long endSequence,
                                        @RequestParam(name = "beginAisle", required = false, defaultValue = "") String beginAisle,
                                        @RequestParam(name = "endAisle", required = false, defaultValue = "") String endAisle,
                                        @RequestParam(name = "sequenceType", required = false, defaultValue = "") String sequenceType,
                                        @RequestParam(name = "includeEmptyLocation", required = false, defaultValue = "") Boolean includeEmptyLocation,
                                        @RequestParam(name = "emptyLocationOnly", required = false, defaultValue = "") Boolean emptyLocationOnly,
                                        @RequestParam(name = "minEmptyCapacity", required = false, defaultValue = "") Double minEmptyCapacity,
                                        @RequestParam(name = "pickableLocationOnly", required = false, defaultValue = "") Boolean pickableLocationOnly,
                                        @RequestParam(name = "includeDisabledLocation", required = false, defaultValue = "") Boolean includeDisabledLocation,
                                        @RequestParam(name = "maxResultCount", required =  false, defaultValue =  "0") Integer maxResultCount,
                                        @RequestParam(name = "reservedCode", required =  false, defaultValue =  "") String reservedCode,
                                        @RequestParam(name = "emptyReservedCodeOnly", required =  false, defaultValue =  "") Boolean emptyReservedCodeOnly
                                        ) {

        StringBuilder params = new StringBuilder()
                .append("Start to find location with params:")
                .append("\nwarehouseId: ").append(warehouseId)
                .append("\nlocationGroupTypeIds: ").append(locationGroupTypeIds)
                .append("\nlocationGroupIds: ").append(locationGroupIds)
                .append("\nname: ").append(name)
                .append("\ncode: ").append(code)
                .append("\nbeginSequence: ").append(beginSequence)
                .append("\nendSequence: ").append(endSequence)
                .append("\nbeginAisle: ").append(beginAisle)
                .append("\nendAisle: ").append(endAisle)
                .append("\nsequenceType: ").append(sequenceType)
                .append("\nincludeEmptyLocation: ").append(includeEmptyLocation)
                .append("\nemptyLocationOnly: ").append(emptyLocationOnly)
                .append("\nminEmptyCapacity: ").append(minEmptyCapacity)
                .append("\npickableLocationOnly: ").append(pickableLocationOnly)
                .append("\nincludeDisabledLocation: ").append(includeDisabledLocation)
                .append("\nmaxResultCount: ").append(maxResultCount);

        logger.debug(params.toString());


        List<Location> locations = locationService.findAll(
                warehouseId,
                locationGroupTypeIds, locationGroupIds, name,
                beginSequence, endSequence, beginAisle, endAisle, sequenceType,
                includeEmptyLocation, emptyLocationOnly, minEmptyCapacity,pickableLocationOnly,  reservedCode,
                includeDisabledLocation, emptyReservedCodeOnly, code, locationStatus);

        logger.debug(">> Find {} locations", locations.size());
        if (locations.size() == 0) {
            return locations;
        }

        int returnResultCout = locations.size();
        if (maxResultCount > 0) {
            returnResultCout = Math.min(maxResultCount, locations.size());
        }
        logger.debug(">> Will only return {} locations", returnResultCout);
        return locations.subList(0, returnResultCout);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations/dock")
    public List<Location> findDockLocations(@RequestParam Long warehouseId,
                                            @RequestParam(name = "empty", required = false, defaultValue = "false") boolean emptyDockOnly) {

        return locationService.getDockLocations(warehouseId, emptyDockOnly);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/order-locations/{orderNumber}")
    public Location createOrderLocation(@RequestParam Long warehouseId,
                                      @PathVariable String orderNumber) {

        return locationService.createOrderLocation(warehouseId, orderNumber);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/shipped-inventory/trailer/{trailerNumber}")
    public Location createShippedTrailerLocation(@RequestParam Long warehouseId,
                                      @PathVariable String trailerNumber) {

        return locationService.createShippedTrailerLocation(warehouseId, trailerNumber);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/shipped-inventory/trailer-appointment/{trailerAppointmentNumber}")
    public Location createShippedTrailerAppointmentLocation(@RequestParam Long warehouseId,
                                               @PathVariable String trailerAppointmentNumber) {

        return locationService.createShippedTrailerAppointmentLocation(warehouseId, trailerAppointmentNumber);
    }
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/dock/{id}/dispatch-trailer")
    public Location dispatchTrailerFromDock(@PathVariable Long id){
        return locationService.moveTrailerFromDock(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/dock/{id}/check-in-trailer")
    public Location checkInTrailerAtDock(@PathVariable Long id,
                                         @RequestParam Long trailerId){
        return locationService.checkInTrailerAtDock(id, trailerId);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/locations")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public ResponseBodyWrapper removeLocations(@RequestParam Long warehouseId,
                                               @RequestParam String locationIds) {


            locationService.removeLocations(warehouseId, locationIds);
            return  ResponseBodyWrapper.success(locationIds);
    }


    /**
     * @param id
     * @param enabled
     * @param inventoryQuantity
     * @param inventorySize
     * @return
     */
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location updateLocation(@PathVariable Long id,
                                   @RequestParam(name = "enabled", defaultValue = "", required = false) Boolean enabled,
                                   @RequestParam(name = "inventoryQuantity", defaultValue = "", required = false) Long inventoryQuantity,
                                   @RequestParam(name = "inventorySize", defaultValue = "", required = false) Double inventorySize) {

        logger.debug(">>> Start to handle updateLocation request with warehouseId: {}, inventoryQuantity: {} / {}",
                id, inventoryQuantity, inventorySize);

        Location location = locationService.findById(id);

        if (enabled != null) {
            location.setEnabled(enabled);
        }
        // reset the volume is the location is volume tracking

        if (inventoryQuantity != null &&
                inventorySize != null &&
                location.getLocationGroup().getTrackingVolume() == true) {

                location.setCurrentVolume(locationService.getLocationVolume(inventoryQuantity, inventorySize));
        }
        return locationService.saveOrUpdate(location);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location changeLocation(@RequestBody Location location) {

        return locationService.saveOrUpdate(location);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations")
    public Location addLocation(@RequestParam Long warehouseId,
                                @RequestBody Location location) {

        return locationService.addLocation(warehouseId, location);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/rf")
    public Location addRFLocation(@RequestParam Long warehouseId,
                                  @RequestParam String rfCode) {

        return locationService.addRFLocation(warehouseId, rfCode);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locations/customer-return-order-stage-locations")
    public Location addCustomerReturnOrderStageLocation(@RequestParam Long warehouseId,
                                  @RequestParam String name) {

        return locationService.addCustomerReturnOrderStageLocation(warehouseId, name);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/locations/rf")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location removeRFLocation(@RequestParam Long warehouseId,
                                  @RequestParam String rfCode) {

        return locationService.removeRFLocation(warehouseId, rfCode);
    }

    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/reserve")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location reserveLocation(@PathVariable Long id,
                                    @RequestParam(name = "reservedCode") String reservedCode) {


        logger.debug(">>> Start to handle reserveLocation request with id: {}, reservedCode: {}",
                id, reservedCode);
        return locationService.reserveLocation(id, reservedCode);
    }

    /**
     * release location from certain reserve code
     * @param warehouseId warehouse id
     * @param reservedCode reserve code
     * @return all locations that used to have this reserve code
     */
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/unreserve")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public List<Location> unreserveLocation(@RequestParam Long warehouseId,
                                            @RequestParam(name = "reservedCode", required = false, defaultValue = "") String reservedCode,
                                            @RequestParam(name = "locationId", required = false, defaultValue = "") Long locationId,
                                            @RequestParam(name = "clearReservedVolume", required = false, defaultValue = "") Boolean clearReservedVolume) {


        logger.debug(">>> Start to handle unreserveLocation request with warehouseId: {}, reservedCode: {}",
                warehouseId, reservedCode);
        return locationService.unreserveLocation(warehouseId, reservedCode, locationId, clearReservedVolume);
    }

    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/reserveWithVolume")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location reserveLocation(@PathVariable Long id,
                                    @RequestParam(name = "reservedCode") String reservedCode,
                                    @RequestParam(name = "pendingSize") Double pendingSize,
                                    @RequestParam(name = "pendingQuantity") Long pendingQuantity,
                                    @RequestParam(name = "pendingPalletQuantity") Integer pendingPalletQuantity) {


        logger.debug(">>> Start to handle changePendingVolume request with id: {}, reservedCode: {}",
                id, reservedCode);
        return locationService.reserveLocation(id, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
    }

    // Allocate a final destination for a inventory and update the pending volume of the
    // location
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/allocate")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location allocateLocation(@PathVariable Long id,
                                     @RequestParam(name = "inventorySize") Double inventorySize) {

        logger.debug("Start to allocate location with id {}, inventory size is : {}", id, inventorySize);
        return locationService.allocateLocation(id, inventorySize);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/pending-volume")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location changePendingVolume(@PathVariable Long id,
                                        @RequestParam(name = "reduce", required = false, defaultValue = "0.0") Double reducedPendingVolume,
                                        @RequestParam(name = "increase", required = false, defaultValue = "0.0") Double increasedPendingVolume) {

        logger.debug(">>> Start to handle changePendingVolume request with id: {}, increase: {}",
                id, increasedPendingVolume);
        return locationService.changePendingVolume(id, reducedPendingVolume, increasedPendingVolume);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/volume")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location changeVolume(@PathVariable Long id,
                                 @RequestParam(name = "reduce", required = false, defaultValue = "0.0") Double reducedVolume,
                                 @RequestParam(name = "increase", required = false, defaultValue = "0.0") Double increasedVolume,
                                 @RequestParam(name = "fromPendingVolume", required = false, defaultValue = "false") Boolean fromPendingVolume) {

        logger.debug(">>> Start to handle changeVolume request with id: {}, increase: {}",
                id, increasedVolume);
        return locationService.changeLocationVolume(id, reducedVolume, increasedVolume, fromPendingVolume);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/lock")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Location", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Location", allEntries = true),
            }
    )
    public Location processLocationLock(@PathVariable Long id,
                                 @RequestParam Boolean locked) {

        return locationService.processLocationLock(id, locked);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locations/container/{containerName}")
    public Location getOrCreateContainerLocation(@PathVariable String containerName,
                                        @RequestParam Long warehouseId) {

        return locationService.getOrCreateContainerLocation(warehouseId, containerName);
    }


    @RequestMapping(method=RequestMethod.GET, value="/locations/parcel-locations/{carrierName}/{serviceLevelName}")
    public Location getShippedParcelLocation(@RequestParam Long warehouseId,
                                             @PathVariable String carrierName,
                                             @PathVariable String serviceLevelName) {
        return locationService.getShippedParcelLocation(warehouseId, carrierName, serviceLevelName);

    }

    /**
     * Return utilization tracking locations. we normally use those locations to calculate the location
     * utilization and the storage fee for the client
     * @param warehouseId
     * @return A map, key will be the ItemVolumeTrackingLevel, value will be a list of location id separated by comma
     */
    @RequestMapping(method=RequestMethod.GET, value="/locations/utilization-tracking")
    public Map<String, String> getUtilizationTrackingLocations(@RequestParam Long warehouseId) {
        return locationService.getUtilizationTrackingLocations(warehouseId);

    }


    @RequestMapping(method=RequestMethod.GET, value="/locations/receiving-stage")
    public List<Location> findReceivingStageLocations(@RequestParam Long warehouseId) {

        return locationService.findReceivingStageLocations(warehouseId);
    }
}

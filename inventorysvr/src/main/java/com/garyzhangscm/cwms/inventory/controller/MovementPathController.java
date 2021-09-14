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

package com.garyzhangscm.cwms.inventory.controller;


import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.Location;
import com.garyzhangscm.cwms.inventory.model.MovementPath;
import com.garyzhangscm.cwms.inventory.service.MovementPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MovementPathController {
    @Autowired
    MovementPathService movementPathService;

    @RequestMapping(value="/movement-path", method = RequestMethod.GET)
    public List<MovementPath> findAllMovementPaths(
            @RequestParam Long warehouseId,
            @RequestParam(name="fromLocationId", required = false, defaultValue = "") Long fromLocationId,
            @RequestParam(name="fromLocation", required = false, defaultValue = "") String fromLocationName,
            @RequestParam(name="fromLocationGroupId", required = false, defaultValue = "") Long fromLocationGroupId,
            @RequestParam(name="toLocationId", required = false, defaultValue = "") Long toLocationId,
            @RequestParam(name="toLocation", required = false, defaultValue = "") String toLocationName,
            @RequestParam(name="toLocationGroupId", required = false, defaultValue = "") Long toLocationGroupId) {
        return movementPathService.findAll(warehouseId, fromLocationId, fromLocationName, fromLocationGroupId, toLocationId, toLocationName, toLocationGroupId);
    }

    @RequestMapping(value="/movement-path/match", method = RequestMethod.GET)
    public List<MovementPath> findMatchedMovementPaths(
                                                   @RequestParam Long warehouseId,
                                                   @RequestParam(name="fromLocationId", required = false, defaultValue = "") Long fromLocationId,
                                                   @RequestParam(name="fromLocation", required = false, defaultValue = "") String fromLocationName,
                                                   @RequestParam(name="fromLocationGroupId", required = false, defaultValue = "") Long fromLocationGroupId,
                                                   @RequestParam(name="toLocationId", required = false, defaultValue = "") Long toLocationId,
                                                   @RequestParam(name="toLocation", required = false, defaultValue = "") String toLocationName,
                                                   @RequestParam(name="toLocationGroupId", required = false, defaultValue = "") Long toLocationGroupId) {
        return movementPathService.findMatchedMovementPaths(warehouseId, fromLocationId, fromLocationName, fromLocationGroupId, toLocationId, toLocationName, toLocationGroupId);
    }

    @RequestMapping(value="/movement-path/{id}", method = RequestMethod.GET)
    public MovementPath findById(@PathVariable Long id) {
        return movementPathService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/movement-path", method = RequestMethod.POST)
    public MovementPath createMovementPath(@RequestBody MovementPath movementPath) {
        return movementPathService.save(movementPath);
    }

    @BillableEndpoint
    @RequestMapping(value="/movement-path/{id}", method = RequestMethod.PUT)
    public MovementPath changeMovementPath(@PathVariable Long id,
                                           @RequestBody MovementPath movementPath) {
        if (id != movementPath.getId()) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; movementPath.getId(): " + movementPath.getId());
        }
        return movementPathService.save(movementPath);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/movement-path/{id}")
    public void removeMovementPath(@PathVariable Long id) {
        movementPathService.delete(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/movement-path?movement_path_ids")
    public void removeMovementPath(@RequestParam(name="movementPathIds") String movementPathIds) {
        movementPathService.removeMovementPaths(movementPathIds);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/movement-path/reserve")
    public List<Location> reserveHopLocations(@RequestParam(name="fromLocationId") Long fromLocationId,
                                              @RequestParam(name="toLocationId") Long toLocationId,
                                              @RequestBody Inventory inventory) {

        return movementPathService.reserveHopLocations(fromLocationId, toLocationId, inventory);
    }


}

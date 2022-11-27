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

package com.garyzhangscm.cwms.common.controller;

import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.ABCCategory;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Velocity;
import com.garyzhangscm.cwms.common.service.ABCCategoryService;
import com.garyzhangscm.cwms.common.service.VelocityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class VelocityController {
    @Autowired
    VelocityService velocityService;

    @RequestMapping(value="/velocities", method = RequestMethod.GET)
    public List<Velocity> findAllVelocities(@RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                            @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                            @RequestParam(name = "description", required = false, defaultValue = "") String description) {

        return velocityService.findAll(warehouseId, name, description);
    }

    @RequestMapping(value="/velocities/{id}", method = RequestMethod.GET)
    public Velocity findABCCategory(@PathVariable Long id) {
        return velocityService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/velocities", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "inventory_velocity", allEntries = true),
            }
    )
    public Velocity addVelocity(@RequestBody Velocity velocity) {
        return velocityService.addVelocity(velocity);
    }


    @BillableEndpoint
    @RequestMapping(value="/velocities/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "inventory_velocity", allEntries = true),
            }
    )
    public Velocity changeVelocity(@PathVariable Long id, @RequestBody Velocity velocity) {
        if (Objects.nonNull(velocity.getId()) && !Objects.equals(velocity.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; velocity.getId(): " + velocity.getId());
        }
        return velocityService.changeVelocity(velocity);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/velocities/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "inventory_velocity", allEntries = true),
            }
    )
    public void removeVelocity(@PathVariable Long id) {
        velocityService.removeVelocity(id);
    }
}

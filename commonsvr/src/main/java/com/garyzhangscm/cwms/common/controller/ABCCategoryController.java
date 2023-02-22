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

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.service.ABCCategoryService;
import com.garyzhangscm.cwms.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class ABCCategoryController {
    @Autowired
    ABCCategoryService abcCategoryService;

    @RequestMapping(value="/abc-categories", method = RequestMethod.GET)
    public List<ABCCategory> findAllABCCategories(@RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                            @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                            @RequestParam(name = "description", required = false, defaultValue = "") String description) {

        return abcCategoryService.findAll(warehouseId, name, description);
    }

    @RequestMapping(value="/abc-categories/{id}", method = RequestMethod.GET)
    public ABCCategory findABCCategory(@PathVariable Long id) {
        return abcCategoryService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/abc-categories", method = RequestMethod.POST)
    @CacheEvict(cacheNames = "InventoryService_ABCCategories", allEntries = true)
    // @CachePut(cacheNames = "ABCCategory", key="#root.caches[0].id")
    public ABCCategory addABCCategory(@RequestBody ABCCategory abcCategory) {
        return abcCategoryService.addABCCategory(abcCategory);
    }


    @BillableEndpoint
    @RequestMapping(value="/abc-categories/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_ABCCategories", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_ABCCategory", allEntries = true),
            }
    )
    //@CachePut(cacheNames = "ABCCategory", key="#root.caches[0].id")
    public ABCCategory changeABCCategory(@PathVariable Long id, @RequestBody ABCCategory abcCategory) {
        if (Objects.nonNull(abcCategory.getId()) && !Objects.equals(abcCategory.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; abcCategory.getId(): " + abcCategory.getId());
        }
        return abcCategoryService.changeABCCategory(abcCategory);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/abc-categories/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_ABCCategories", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_ABCCategory", allEntries = true),
            }
    )
    public void removeABCCategory(@PathVariable Long id) {
        abcCategoryService.removeABCCategory(id);
    }
}

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

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.service.ItemFamilyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class ItemFamilyController {

    private static Logger logger = LoggerFactory
            .getLogger(ItemFamilyController.class);
    @Autowired
    ItemFamilyService itemFamilyService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/item-families", method = RequestMethod.GET)
    public List<ItemFamily> findAllItemFaimlies(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                                @RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId,
                                                @RequestParam(name="companyItem", required = false, defaultValue = "") Boolean companyItem,
                                                @RequestParam(name="warehouseSpecificItem", required = false, defaultValue = "") Boolean warehouseSpecificItem,
                                                @RequestParam(name="name", required = false, defaultValue = "") String name) {

        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }

        logger.debug("Start to call findAllItemFaimlies with parameters " +
                "company id: {}, warehouse id: {}, name: {}",
                companyId, warehouseId, name);
        return itemFamilyService.findAll(companyId, warehouseId, name, companyItem, warehouseSpecificItem);
    }

    @RequestMapping(value="/item-family/{id}", method = RequestMethod.GET)
    public ItemFamily findItemFamily(@PathVariable Long id) {
        return itemFamilyService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/item-family", method = RequestMethod.POST)
    public ItemFamily createItemFamily(@RequestBody ItemFamily itemFamily) {
        return itemFamilyService.addItemFamily(itemFamily);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/item-family/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_ItemFamily", allEntries = true),
            }
    )
    public ItemFamily changeItemFamily(@PathVariable long id,
                                       @RequestBody ItemFamily itemFamily) {
        if (itemFamily.getId() != null && itemFamily.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; itemFamily.getId(): " + itemFamily.getId());
        }
        return itemFamilyService.save(itemFamily);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/item-family")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemFamily", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_ItemFamily", allEntries = true),
            }
    )
    public void removeItemFamilies(@RequestParam(name = "item_family_ids", required = false, defaultValue = "") String itemFamilyIds) {
        itemFamilyService.removeItemFamilies(itemFamilyIds);
    }

}

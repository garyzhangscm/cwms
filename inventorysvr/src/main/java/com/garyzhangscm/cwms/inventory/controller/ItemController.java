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

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class ItemController {
    @Autowired
    ItemService itemService;

    @Autowired
    FileService fileService;

    @RequestMapping(value="/items", method = RequestMethod.GET)
    public List<Item> findAllItems(@RequestParam Long companyId,
                                   @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                   @RequestParam(name="name", required = false, defaultValue = "") String name,
                                   @RequestParam(name="clientIds", required = false, defaultValue = "") String clientIds,
                                   @RequestParam(name="itemFamilyIds", required = false, defaultValue = "") String itemFamilyIds,
                                   @RequestParam(name="itemIdList", required = false, defaultValue = "") String itemIdList,
                                   @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {
        return itemService.findAll(companyId, warehouseId, name, clientIds, itemFamilyIds, itemIdList, loadDetails);
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.GET)
    public Item findItem(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/items/{id}", method = RequestMethod.DELETE)
    @CacheEvict(cacheNames = "item", key = "#id")
    public Item deleteItem(@PathVariable Long id) {
        return itemService.deleteItem(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items/{id}/images/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_item", allEntries = true),
                    @CacheEvict(cacheNames = "outbound_item", allEntries = true),
                    @CacheEvict(cacheNames = "inbound_item", allEntries = true),
            }
    )
    public Item uploadItemImages(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) throws IOException {


        return  itemService.uploadItemImages(id, file);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/items")
    public void removeItems(@RequestParam(name = "item_ids", required = false, defaultValue = "") String itemIds) {
        itemService.delete(itemIds);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_item", allEntries = true),
                    @CacheEvict(cacheNames = "outbound_item", allEntries = true),
                    @CacheEvict(cacheNames = "inbound_item", allEntries = true),
            }
    )
    public Item addItem(@RequestBody Item item) {
        return itemService.addItem(item);
    }

    @BillableEndpoint
    @RequestMapping(value="/items/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_item", allEntries = true),
                    @CacheEvict(cacheNames = "outbound_item", allEntries = true),
                    @CacheEvict(cacheNames = "inbound_item", allEntries = true),
            }
    )
    public Item changeItem(@PathVariable Long id, @RequestBody Item item) {

        return itemService.changeItem(id, item);
    }

    @RequestMapping(method=RequestMethod.POST, value="/items/validate-new-item-name")
    public ResponseBodyWrapper<String> validateNewItemName(@RequestParam Long warehouseId,
                                                      @RequestParam String itemName)  {

        return ResponseBodyWrapper.success(itemService.validateNewItemName(warehouseId, itemName));
    }


}

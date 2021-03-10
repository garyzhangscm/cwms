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
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Item> findAllItems(@RequestParam Long warehouseId,
                                   @RequestParam(name="name", required = false, defaultValue = "") String name,
                                   @RequestParam(name="clientIds", required = false, defaultValue = "") String clientIds,
                                   @RequestParam(name="itemFamilyIds", required = false, defaultValue = "") String itemFamilyIds) {
        return itemService.findAll(warehouseId, name, clientIds, itemFamilyIds);
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.GET)
    public Item findItem(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.DELETE)
    public Item deleteItem(@PathVariable Long id) {
        return itemService.deleteItem(id);
    }

    @RequestMapping(method=RequestMethod.POST, value="/items/{id}/images/upload")
    public Item uploadItemImages(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) throws IOException {


        return  itemService.uploadItemImages(id, file);
    }





    @RequestMapping(method=RequestMethod.DELETE, value="/items")
    public void removeItems(@RequestParam(name = "item_ids", required = false, defaultValue = "") String itemIds) {
        itemService.delete(itemIds);
    }

    @RequestMapping(method=RequestMethod.POST, value="/items")
    public Item addItem(@RequestBody Item item) {
        return itemService.addItem(item);
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.PUT)
    public Item changeItem(@PathVariable Long id, @RequestBody Item item) {

        return itemService.changeItem(id, item);
    }

    @RequestMapping(method=RequestMethod.POST, value="/items/validate-new-item-name")
    public ResponseBodyWrapper<String> validateNewItemName(@RequestParam Long warehouseId,
                                                      @RequestParam String itemName)  {

        return ResponseBodyWrapper.success(itemService.validateNewItemName(warehouseId, itemName));
    }
}

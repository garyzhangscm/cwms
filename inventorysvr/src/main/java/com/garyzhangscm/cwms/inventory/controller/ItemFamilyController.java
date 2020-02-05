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

import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.service.ItemFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItemFamilyController {
    @Autowired
    ItemFamilyService itemFamilyService;

    @RequestMapping(value="/item-families", method = RequestMethod.GET)
    public List<ItemFamily> findAllItemFaimlies(@RequestParam Long warehouseId,
                                                @RequestParam(name="name", required = false, defaultValue = "") String name) {
        return itemFamilyService.findAll(warehouseId, name);
    }

    @RequestMapping(value="/item-family/{id}", method = RequestMethod.GET)
    public ItemFamily findItemFamily(@PathVariable Long id) {
        return itemFamilyService.findById(id);
    }

    @RequestMapping(value="/item-family", method = RequestMethod.POST)
    public ItemFamily createItemFamily(@RequestBody ItemFamily itemFamily) {
        return itemFamilyService.save(itemFamily);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/item-family/{id}")
    public ItemFamily changeItemFamily(@PathVariable long id,
                                       @RequestBody ItemFamily itemFamily) {
        if (itemFamily.getId() != null && itemFamily.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return itemFamilyService.save(itemFamily);
    }


    @RequestMapping(method=RequestMethod.DELETE, value="/item-family")
    public void removeItemFamilies(@RequestParam(name = "item_family_ids", required = false, defaultValue = "") String itemFamilyIds) {
        itemFamilyService.delete(itemFamilyIds);
    }

}

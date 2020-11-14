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

import com.garyzhangscm.cwms.inventory.model.ItemPackageType;
import com.garyzhangscm.cwms.inventory.service.ItemPackageTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ItemPackageTypeController {
    @Autowired
    ItemPackageTypeService itemPackageTypeService;


    @RequestMapping(value="/itemPackageTypes", method = RequestMethod.GET)
    public List<ItemPackageType> findAllItemPackageTypes(@RequestParam(name = "name", required = false, defaultValue = "")  String name,
                                                         @RequestParam(name = "itemId", required = false, defaultValue = "")  Long itemId,
                                                         @RequestParam Long warehouseId) {
        return itemPackageTypeService.findAll(warehouseId, name, itemId);
    }


    @RequestMapping(value="/itemPackageTypes/{id}/removable", method = RequestMethod.GET)
    public Boolean isItemPackageTypeRemovable(@PathVariable Long id) {

        return itemPackageTypeService.isItemPackageTypeRemovable(id);
    }

}

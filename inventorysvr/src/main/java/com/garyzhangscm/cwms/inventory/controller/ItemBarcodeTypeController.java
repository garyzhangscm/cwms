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

import com.garyzhangscm.cwms.inventory.model.ItemBarcodeType;
import com.garyzhangscm.cwms.inventory.service.ItemBarcodeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItemBarcodeTypeController {
    @Autowired
    ItemBarcodeTypeService itemBarcodeTypeService;

    @RequestMapping(value="/item-barcode-types", method = RequestMethod.GET)
    public List<ItemBarcodeType> findAllItemBarcodeTypes(@RequestParam Long warehouseId,
                                                         @RequestParam(required = false, name = "name", defaultValue = "") String name) {

        return itemBarcodeTypeService.findAll( warehouseId, name);
    }


    @RequestMapping(value="/item-barcode-types/{id}", method = RequestMethod.GET)
    public ItemBarcodeType findItemBarcodeType(@PathVariable Long id) {
        return itemBarcodeTypeService.findById(id);
    }



    @RequestMapping(value="/item-barcode-types", method = RequestMethod.POST)
    public ItemBarcodeType addItemBarcodeType(@RequestBody ItemBarcodeType itemBarcodeType) {
        return itemBarcodeTypeService.addItemBarcodeType(itemBarcodeType);
    }

    @RequestMapping(value="/item-barcode-types/{id}", method = RequestMethod.PUT)
    public ItemBarcodeType changeItemBarcodeType(@PathVariable Long id, @RequestBody ItemBarcodeType itemBarcodeType) {

        return itemBarcodeTypeService.changeItemBarcodeType(itemBarcodeType);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/item-barcode-types/{id}")
    public void removeItemBarcodeType(@PathVariable Long id) {
        itemBarcodeTypeService.delete(id);
    }
}

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

import com.garyzhangscm.cwms.inventory.model.ItemBarcode;
import com.garyzhangscm.cwms.inventory.service.ItemBarcodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItemBarcodeController {
    @Autowired
    ItemBarcodeService itemBarcodeService;

    @RequestMapping(value="/item-barcodes", method = RequestMethod.GET)
    public List<ItemBarcode> findAllItemBarcodes(@RequestParam Long warehouseId,
                                                 @RequestParam(required = false, name = "itemId", defaultValue = "") Long itemId,
                                                 @RequestParam(required = false, name = "itemName", defaultValue = "") String itemName) {

        return itemBarcodeService.findAll( warehouseId, itemId, itemName);
    }


    @RequestMapping(value="/item-barcodes/{id}", method = RequestMethod.GET)
    public ItemBarcode findItemBarcode(@PathVariable Long id) {
        return itemBarcodeService.findById(id);
    }



    @RequestMapping(value="/item-barcodes", method = RequestMethod.POST)
    public ItemBarcode addItemBarcode(@RequestBody ItemBarcode itemBarcode) {
        return itemBarcodeService.addItemBarcode(itemBarcode);
    }

    @RequestMapping(value="/item-barcodes/{id}", method = RequestMethod.PUT)
    public ItemBarcode changeItemBarcode(@PathVariable Long id, @RequestBody ItemBarcode itemBarcode) {

        return itemBarcodeService.changeItemBarcode(itemBarcode);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/item-barcodes/{id}")
    public void removeItemBarcode(@PathVariable Long id) {
        itemBarcodeService.delete(id);
    }
}

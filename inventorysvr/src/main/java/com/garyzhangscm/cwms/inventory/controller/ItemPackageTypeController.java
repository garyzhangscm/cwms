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
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemPackageType;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.ItemPackageTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
public class ItemPackageTypeController {
    @Autowired
    ItemPackageTypeService itemPackageTypeService;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @RequestMapping(value="/itemPackageTypes", method = RequestMethod.GET)
    public List<ItemPackageType> findAllItemPackageTypes(@RequestParam(name = "name", required = false, defaultValue = "")  String name,
                                                         @RequestParam(name = "itemId", required = false, defaultValue = "")  Long itemId,
                                                         @RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                                         @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId) {

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

        return itemPackageTypeService.findAll(companyId, warehouseId, name, itemId);
    }


    @RequestMapping(value="/itemPackageTypes/{id}/removable", method = RequestMethod.GET)
    public Boolean isItemPackageTypeRemovable(@PathVariable Long id) {

        return itemPackageTypeService.isItemPackageTypeRemovable(id);
    }



}

/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.layout.controller;

import com.garyzhangscm.cwms.layout.Exception.GenericException;
import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WarehouseController {

    @Autowired
    WarehouseService warehouseService;


    @RequestMapping(value="/warehouses", method=RequestMethod.GET)
    public List<Warehouse> listWarehouses() {
        return warehouseService.findAll();
    }

    @RequestMapping(value="/warehouses", method=RequestMethod.POST)
    public Warehouse addWarehouses(@RequestBody Warehouse warehouse) {
        return warehouseService.save(warehouse);
    }

    @RequestMapping(value="/warehouse/{id}", method=RequestMethod.PUT)
    public Warehouse changeWarehouse(@PathVariable long id, @RequestBody Warehouse warehouse) {
        if (warehouse.getId() != null && warehouse.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return warehouseService.save(warehouse);
    }

    @RequestMapping(value="/warehouse/{id}", method=RequestMethod.DELETE)
    public Warehouse removeWarehouses(@PathVariable long id) {
        Warehouse removedWarehouse = warehouseService.findById(id);
        warehouseService.delete(id);
        return removedWarehouse;
    }

    @RequestMapping(value="/warehouse/{id}", method = RequestMethod.GET)
    public Warehouse findWarehouseByID(@PathVariable long id) {
        return warehouseService.findById(id);
    }

    @RequestMapping(value="/warehouse/accessible/{username}", method=RequestMethod.GET)
    public List<Warehouse> getAccessibleWarehouse(@PathVariable String username) {
        // TO-DO: return warehouse that the user has access
        return warehouseService.findAll();
    }


}

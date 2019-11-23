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

import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SupplierController {
    @Autowired
    SupplierService supplierService;

    @RequestMapping(value="/suppliers", method = RequestMethod.GET)
    public List<Supplier> findAllClients() {
        return supplierService.findAll();
    }

    @RequestMapping(value="/supplier/{id}", method = RequestMethod.GET)
    public Supplier findSupplier(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @RequestMapping(value="/supplier", method = RequestMethod.POST)
    public Supplier addSupplier(@RequestBody Supplier supplier) {
        return supplierService.save(supplier);
    }

    @RequestMapping(value="/supplier/{id}", method = RequestMethod.PUT)
    public Supplier changeSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        if (supplier.getId() != null && supplier.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return supplierService.save(supplier);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/supplier")
    public void deleteClients(@RequestParam(name = "supplier_ids", required = false, defaultValue = "") String supplierIds) {
        supplierService.delete(supplierIds);
    }
}

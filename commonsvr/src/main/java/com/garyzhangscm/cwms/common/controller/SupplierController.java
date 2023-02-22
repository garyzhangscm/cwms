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

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RestController
public class SupplierController {
    private static final Logger logger = LoggerFactory.getLogger(SupplierController.class);
    @Autowired
    SupplierService supplierService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/suppliers", method = RequestMethod.GET)
    public List<Supplier> findAllSuppliers(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                           @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                           @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                           @RequestParam(name = "quickbookListId", required = false, defaultValue = "") String quickbookListId) {
        try {
            logger.debug("start to find purchase order by supplier {} , after decode {}",
                    name,
                    java.net.URLDecoder.decode(name, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for finding supplier");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }

        return supplierService.findAll(companyId, warehouseId, name, quickbookListId);
    }

    @RequestMapping(value="/suppliers/{id}", method = RequestMethod.GET)
    public Supplier findSupplier(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/suppliers", method = RequestMethod.POST)
    public Supplier addSupplier(@RequestBody Supplier supplier) {
        return supplierService.save(supplier);
    }

    @BillableEndpoint
    @RequestMapping(value="/suppliers/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Supplier", allEntries = true),
            }
    )
    public Supplier changeSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        if (Objects.nonNull(supplier.getId()) && !Objects.equals(supplier.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; supplier.getId(): " + supplier.getId());

        }
        return supplierService.save(supplier);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/suppliers")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Supplier", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Supplier", allEntries = true),
            }
    )
    public void deleteSuppliers(@RequestParam Long warehouseId,
                                @RequestParam(name = "supplierIds", required = false, defaultValue = "") String supplierIds) {
        supplierService.delete(warehouseId, supplierIds);
    }
}

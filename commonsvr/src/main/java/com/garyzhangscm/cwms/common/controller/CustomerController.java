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
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.service.CustomerService;
import com.garyzhangscm.cwms.common.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class CustomerController {
    @Autowired
    CustomerService customerService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/customers", method = RequestMethod.GET)
    public List<Customer> findAllCustomers(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                           @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                           @RequestParam(name = "name", required = false, defaultValue = "") String name) {

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
        return customerService.findAll(companyId, warehouseId, name);
    }

    @RequestMapping(value="/customers/{id}", method = RequestMethod.GET)
    public Customer findCustomer(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/customers", method = RequestMethod.POST)
    public Customer addCustomer(@RequestBody Customer customer) {
        return customerService.save(customer);
    }


    @BillableEndpoint
    @RequestMapping(value="/customers/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Customer", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Customer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Customer", allEntries = true),
            }
    )
    public Customer changeCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        if (Objects.nonNull(customer.getId()) && !Objects.equals(customer.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; customer.getId(): " + customer.getId());
        }
        return customerService.save(customer);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/customers")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Customer", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Customer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Customer", allEntries = true),
            }
    )
    public void removeCustomers(@RequestParam(name = "customerIds", required = false, defaultValue = "") String customerIds) {
        customerService.removeCustomers(customerIds);
    }
}

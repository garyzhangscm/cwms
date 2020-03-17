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
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.service.CustomerService;
import com.garyzhangscm.cwms.common.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CustomerController {
    @Autowired
    CustomerService customerService;

    @RequestMapping(value="/customers", method = RequestMethod.GET)
    public List<Customer> findAllCustomers(@RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return customerService.findAll(name);
    }

    @RequestMapping(value="/customer/{id}", method = RequestMethod.GET)
    public Customer findCustomer(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @RequestMapping(value="/customer", method = RequestMethod.POST)
    public Customer addCustomer(@RequestBody Customer customer) {
        return customerService.save(customer);
    }

    @RequestMapping(value="/customer/{id}", method = RequestMethod.PUT)
    public Customer changeCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        if (customer.getId() != null && customer.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; customer.getId(): " + customer.getId());
        }
        return customerService.save(customer);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/customer")
    public void deleteCustomers(@RequestParam(name = "customer_ids", required = false, defaultValue = "") String customerIds) {
        customerService.delete(customerIds);
    }
}

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
import com.garyzhangscm.cwms.common.model.ClientRestriction;
import com.garyzhangscm.cwms.common.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class ClientController {
    @Autowired
    ClientService clientService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/clients", method = RequestMethod.GET)
    @ClientValidationEndpoint
    public List<Client> findAllClients(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                       @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                       @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                       ClientRestriction clientRestriction) {

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

        return clientService.findAll(companyId, warehouseId, name, clientRestriction);
    }

    @RequestMapping(value="/clients/{id}", method = RequestMethod.GET)
    public Client findClient(@PathVariable Long id) {
        return clientService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/clients", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_client", allEntries = true),
            }
    )
    public Client addClient(@RequestBody Client client) {
        return clientService.save(client);
    }


    @BillableEndpoint
    @RequestMapping(value="/clients/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_client", allEntries = true),
            }
    )
    public Client changeClient(@PathVariable Long id, @RequestBody Client client) {
        if (client.getId() != null && client.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; client.getId(): " + client.getId());
        }
        return clientService.save(client);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/clients")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_client", allEntries = true),
            }
    )
    public void deleteClients(@RequestParam(name = "clientIds", required = false, defaultValue = "") String clientIds) {
        clientService.delete(clientIds);
    }
}

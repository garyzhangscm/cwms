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
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClientController {
    @Autowired
    ClientService clientService;

    @RequestMapping(value="/clients", method = RequestMethod.GET)
    public List<Client> findAllClients(@RequestParam Long warehouseId,
                                       @RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return clientService.findAll(warehouseId, name);
    }

    @RequestMapping(value="/clients/{id}", method = RequestMethod.GET)
    public Client findClient(@PathVariable Long id) {
        return clientService.findById(id);
    }

    @RequestMapping(value="/clients", method = RequestMethod.POST)
    public Client addClient(@RequestBody Client client) {
        return clientService.save(client);
    }

    @RequestMapping(value="/clients/{id}", method = RequestMethod.PUT)
    public Client changeClient(@PathVariable Long id, @RequestBody Client client) {
        if (client.getId() != null && client.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; client.getId(): " + client.getId());
        }
        return clientService.save(client);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/clients")
    public void deleteClients(@RequestParam(name = "clientIds", required = false, defaultValue = "") String clientIds) {
        clientService.delete(clientIds);
    }
}

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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.OperationType;
import com.garyzhangscm.cwms.resources.service.OperationTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class OperationTypeController {

    @Autowired
    private OperationTypeService operationTypeService;

    @RequestMapping(value="/operation-types", method = RequestMethod.GET)
    public List<OperationType> getOperationType(
                                @RequestParam Long warehouseId,
                                @RequestParam(name = "name", defaultValue = "", required = false) String name,
                                @RequestParam(name = "description", defaultValue = "", required = false) String description) {
        return operationTypeService.findAll(warehouseId,
                name, description);
    }

    @RequestMapping(value="/operation-types/{id}", method = RequestMethod.GET)
    public OperationType getOperationType(
            @RequestParam Long warehouseId,
            @PathVariable Long id) {
        return operationTypeService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/operation-types", method = RequestMethod.PUT)
    public OperationType addOperationType(
            @RequestParam Long warehouseId,
            @RequestBody OperationType operationType) {
        return operationTypeService.addOperationType(operationType);
    }

    @BillableEndpoint
    @RequestMapping(value="/operation-types/{id}", method = RequestMethod.POST)
    public OperationType changeOperationType(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody OperationType operationType) {
        return operationTypeService.changeOperationType(id, operationType);
    }



}

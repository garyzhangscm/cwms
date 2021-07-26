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

package com.garyzhangscm.cwms.workorder.controller;


import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.model.ProductionLineCapacity;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import com.garyzhangscm.cwms.workorder.service.ProductionLineCapacityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionLineCapacityController {
    @Autowired
    ProductionLineCapacityService productionLineCapacityService;

    @RequestMapping(value="/production-line-capacities", method = RequestMethod.GET)
    public List<ProductionLineCapacity> findAllProductionLineCapacity(
            @RequestParam Long warehouseId,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="productionLineIds", required = false, defaultValue = "") String productionLineIds,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId ) {
        return productionLineCapacityService.findAll(warehouseId, productionLineId, productionLineIds, itemId);
    }



    @RequestMapping(value="/production-line-capacities", method = RequestMethod.PUT)
    public ProductionLineCapacity addProductionLineCapacity(@RequestBody ProductionLineCapacity productionLineCapacity) {
        return productionLineCapacityService.addProductionCapacity(productionLineCapacity);
    }


    @RequestMapping(value="/production-line-capacities/{id}", method = RequestMethod.GET)
    public ProductionLineCapacity findProductionLineCapacity(@PathVariable Long id) {

        return productionLineCapacityService.findById(id);
    }

    @RequestMapping(value="/production-line-capacities/{id}", method = RequestMethod.POST)
    public ProductionLineCapacity changeProductionLineCapacity(@PathVariable Long id,
                                               @RequestBody ProductionLineCapacity productionLineCapacity){
        return productionLineCapacityService.changeProductionCapacity(id, productionLineCapacity);
    }

    @RequestMapping(value="/production-line-capacities/{id}", method = RequestMethod.DELETE)
    public void removeProductionLineCapacity(@PathVariable Long id) {
        productionLineCapacityService.delete(id);
    }


}

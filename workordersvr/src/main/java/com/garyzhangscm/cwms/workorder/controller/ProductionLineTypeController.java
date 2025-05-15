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


import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.model.ProductionLineType;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import com.garyzhangscm.cwms.workorder.service.ProductionLineTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionLineTypeController {
    @Autowired
    ProductionLineTypeService productionLineTypeService;

    @RequestMapping(value="/production-line-types", method = RequestMethod.GET)
    public List<ProductionLineType> findAllProductionLineTypes(@RequestParam Long warehouseId,
                                     @RequestParam(name="name", required = false, defaultValue = "") String name,
                                     @RequestParam(name="description", required = false, defaultValue = "") String description) {
        return productionLineTypeService.findAll(warehouseId, name, description);
    }



    @BillableEndpoint
    @RequestMapping(value="/production-line-types", method = RequestMethod.POST)
    public ProductionLineType addProductionLineType(@RequestBody ProductionLineType productionLineType) {
        return productionLineTypeService.addProductionLineType(productionLineType);
    }


    @RequestMapping(value="/production-line-types/{id}", method = RequestMethod.GET)
    public ProductionLineType findProductionLineType(@PathVariable Long id) {

        return productionLineTypeService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-types/{id}", method = RequestMethod.PUT)
    public ProductionLineType changeProductionLineType(@PathVariable Long id,
                                               @RequestBody ProductionLineType productionLineType){
        return productionLineTypeService.changeProductionLineType(id, productionLineType);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-types/{id}", method = RequestMethod.DELETE)
    public void removeProductionLineType(@PathVariable Long id) {
        productionLineTypeService.delete(id);
    }


}

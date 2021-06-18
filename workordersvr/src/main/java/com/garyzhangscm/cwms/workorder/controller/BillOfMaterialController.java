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


import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.BillOfMaterial;
import com.garyzhangscm.cwms.workorder.service.BillOfMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BillOfMaterialController {
    @Autowired
    BillOfMaterialService billOfMaterialService;


    @RequestMapping(value="/bill-of-materials", method = RequestMethod.GET)
    public List<BillOfMaterial> findAllBillOfMaterials(@RequestParam Long warehouseId,
                                                       @RequestParam(name="number", required = false, defaultValue = "") String number,
                                                       @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                       @RequestParam(name="genericMatch", required = false, defaultValue = "false") boolean genericQuery) {
        return billOfMaterialService.findAll(warehouseId, number, itemName, genericQuery);
    }

    @RequestMapping(value="/bill-of-materials/matched-with-work-order", method = RequestMethod.GET)
    public BillOfMaterial findMatchedBillOfMaterial(@RequestParam Long workOrderId) {
        return billOfMaterialService.getMatchedBillOfMaterial(workOrderId);
    }

    @RequestMapping(value="/bill-of-materials/matched-with-item", method = RequestMethod.GET)
    public List<BillOfMaterial> findMatchedBillOfMaterialByItemName(@RequestParam Long warehouseId,
                                                    @RequestParam String itemName) {
        return billOfMaterialService.findMatchedBillOfMaterialByItemName(warehouseId, itemName);
    }

    @RequestMapping(value="/bill-of-materials", method = RequestMethod.POST)
    public BillOfMaterial addBillOfMaterials(@RequestBody BillOfMaterial billOfMaterial) {
        return billOfMaterialService.addBillOfMaterials(billOfMaterial);
    }


    @RequestMapping(value="/bill-of-materials/{id}", method = RequestMethod.GET)
    public BillOfMaterial findBillOfMaterial(@PathVariable Long id) {
        return billOfMaterialService.findById(id);
    }

    @RequestMapping(value="/bill-of-materials/{id}", method = RequestMethod.PUT)
    public BillOfMaterial changeBillOfMaterial(@PathVariable Long id,
                                               @RequestBody BillOfMaterial billOfMaterial){
        return billOfMaterialService.changeBillOfMaterial(id, billOfMaterial);
    }

    @RequestMapping(value="/bill-of-materials", method = RequestMethod.DELETE)
    public void removeBillOfMaterials(@RequestParam(name = "billOfMaterialIds", required = false, defaultValue = "") String billOfMaterialIds) {
        billOfMaterialService.delete(billOfMaterialIds);
    }

    @RequestMapping(value="/bill-of-materials/validate-new-number", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateNewBOMNumber(@RequestParam Long warehouseId,
                                                            @RequestParam String number) {
        return ResponseBodyWrapper.success(billOfMaterialService.validateNewBOMNumber(warehouseId, number));
    }

}

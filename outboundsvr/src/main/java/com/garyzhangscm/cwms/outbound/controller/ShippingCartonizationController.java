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

package com.garyzhangscm.cwms.outbound.controller;


import com.garyzhangscm.cwms.outbound.model.PackingResult;
import com.garyzhangscm.cwms.outbound.model.ShippingCartonization;
import com.garyzhangscm.cwms.outbound.service.ShippingCartonizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShippingCartonizationController {
    @Autowired
    ShippingCartonizationService shippingCartonizationService;

    @RequestMapping(value="/shipping-cartonization", method = RequestMethod.POST)
    public ShippingCartonization pack(@RequestParam Long warehouseId,
                                      @RequestParam String inventoryId,
                                      @RequestParam(name = "packingStationsName", required = false, defaultValue = "") String packingStationsName,
                                      @RequestParam(name = "cartonId", required = false, defaultValue = "") Long cartonId,
                                      @RequestParam(name = "cartonName", required = false, defaultValue = "") String cartonName,
                                      @RequestBody List<PackingResult> packingResult) {
        return shippingCartonizationService.pack(warehouseId, inventoryId, packingStationsName, cartonId,  cartonName, packingResult);
    }




}

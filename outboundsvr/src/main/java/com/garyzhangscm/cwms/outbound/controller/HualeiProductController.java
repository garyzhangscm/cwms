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


import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiConfiguration;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiProduct;
import com.garyzhangscm.cwms.outbound.service.HualeiConfigurationService;
import com.garyzhangscm.cwms.outbound.service.HualeiProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class HualeiProductController {

    @Autowired
    private HualeiProductService hualeiProductService;

    @RequestMapping(value="/hualei-products", method = RequestMethod.GET)
    public List<HualeiProduct> getHualeiProducts(
                                @RequestParam Long warehouseId,
                                @RequestParam (name = "productId", defaultValue = "", required = false) String productId,
                                @RequestParam (name = "name", defaultValue = "", required = false) String name,
                                @RequestParam (name = "description", defaultValue = "", required = false) String description) {
        return hualeiProductService.findAll(warehouseId,
                productId, name, description);
    }

    @RequestMapping(value="/hualei-products/{id}", method = RequestMethod.GET)
    public HualeiProduct getHualeiProduct(
            @RequestParam Long warehouseId,
            @PathVariable Long id) {
        return hualeiProductService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/hualei-products", method = RequestMethod.PUT)
    public HualeiProduct addHualeiProduct(
            @RequestParam Long warehouseId,
            @RequestBody HualeiProduct hualeiProduct) {
        return hualeiProductService.addHualeiProduct(hualeiProduct);
    }

    @BillableEndpoint
    @RequestMapping(value="/hualei-products/{id}", method = RequestMethod.POST)
    public HualeiProduct changeHualeiProduct(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody HualeiProduct hualeiProduct) {
        return hualeiProductService.changeHualeiProduct(id, hualeiProduct);
    }
    @BillableEndpoint
    @RequestMapping(value="/hualei-products/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeHualeiProduct(
            @RequestParam Long warehouseId,
            @PathVariable Long id) {
        hualeiProductService.delete(id);
        return ResponseBodyWrapper.success("hualei product: " +
                id + " is removed");
    }



}

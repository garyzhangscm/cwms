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
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiProduct;
import com.garyzhangscm.cwms.outbound.model.hualei.ShipmentResponse;
import com.garyzhangscm.cwms.outbound.service.HualeiProductService;
import com.garyzhangscm.cwms.outbound.service.HualeiShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class HualeiShippingController {

    @Autowired
    private HualeiShippingService hualeiShippingService;

    @RequestMapping(value="/hualei/shipping", method = RequestMethod.POST)
    public ShipmentResponse sendHualeiRequest(Long warehouseId,
                                              String productId, // hualei product id
                                              Long orderId,
                                              double length,
                                              double width,
                                              double height,
                                              double weight) {
        return hualeiShippingService.sendHualeiShippingRequest(warehouseId,
                productId, orderId, length, width, height, weight);
    }


}

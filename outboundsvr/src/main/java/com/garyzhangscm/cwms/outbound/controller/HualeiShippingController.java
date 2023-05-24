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


import com.garyzhangscm.cwms.outbound.model.hualei.ShipmentRequest;
import com.garyzhangscm.cwms.outbound.service.HualeiShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


@RestController
public class HualeiShippingController {

    @Autowired
    private HualeiShippingService hualeiShippingService;

    @RequestMapping(value="/hualei/shipping", method = RequestMethod.POST)
    public ShipmentRequest[] sendHualeiRequest(Long warehouseId,
                                               String productId, // hualei product id
                                               Long orderId,
                                               double length,
                                               double width,
                                               double height,
                                               double weight,
                                               @RequestParam(name = "lengthUnit", required = false, defaultValue = "") String lengthUnit,
                                               @RequestParam(name = "weightUnit", required = false, defaultValue = "") String weightUnit,
                                               @RequestParam(name = "packageCount", required = false, defaultValue = "1") Integer packageCount,
                                               @RequestParam(name = "itemName", required = false, defaultValue = "") String itemName,
                                               @RequestParam(name = "quantity", required = false, defaultValue = "1") Long quantity,
                                               @RequestParam(name = "unitCost", required = false, defaultValue = "1.0") Double unitCost,
                                               @RequestParam(name = "parcelInsured", required = false, defaultValue = "") Boolean parcelInsured,
                                               @RequestParam(name = "parcelInsuredAmount", required = false, defaultValue = "") Double parcelInsuredAmount,
                                               @RequestParam(name = "parcelSignatureRequired", required = false, defaultValue = "") Boolean parcelSignatureRequired) {
        return hualeiShippingService.sendHualeiShippingRequest(warehouseId,
                productId, orderId, length, width, height, weight, packageCount,
                itemName, quantity, unitCost,
                lengthUnit, weightUnit, parcelInsured, parcelInsuredAmount, parcelSignatureRequired);
    }

    @RequestMapping(value="/hualei/shipping/label", method = RequestMethod.GET)
    public ResponseEntity<Resource> getShippingLabel(Long warehouseId,
                                                     Long orderId,
                                                     String productId,
                                                     String hualeiOrderId) throws FileNotFoundException {

        File reportResultFile = hualeiShippingService.getShippingLabelFile(warehouseId, orderId, productId, hualeiOrderId);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportResultFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + reportResultFile.getName())
                .contentLength(reportResultFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}

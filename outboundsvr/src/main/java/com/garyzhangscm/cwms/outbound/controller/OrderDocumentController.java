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
import com.garyzhangscm.cwms.outbound.model.OrderDocument;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.service.OrderDocumentService;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class OrderDocumentController {

    @Autowired
    OrderDocumentService orderDocumentService;


    @RequestMapping(value="/orders/documents", method = RequestMethod.GET)
    public List<OrderDocument> findAllOrderDocuments(@RequestParam Long warehouseId,
                                                 @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                                 @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber,
                                                     @RequestParam(name = "fileName", required = false, defaultValue = "") String fileName) {
        return orderDocumentService.findAll(warehouseId, orderId, orderNumber, fileName);
    }

    @RequestMapping(value="/orders/documents/{id}", method = RequestMethod.GET)
    public OrderDocument findOrderDocument(@PathVariable Long id) {
        return orderDocumentService.findById(id);
    }

    @RequestMapping(value="/orders/documents/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeOrderDocument(@PathVariable Long id) {
        return ResponseBodyWrapper.success(orderDocumentService.removeOrderDocument(id));

    }


    @RequestMapping(value="/orders/{warehouseId}/{orderId}/documents/upload", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> uploadOrderDocument(@PathVariable Long warehouseId,
                                                   @PathVariable Long orderId,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseBodyWrapper.success(orderDocumentService.uploadOrderDocument(warehouseId, orderId, file));
    }

    @RequestMapping(value="/orders/{orderId}/documents", method = RequestMethod.POST)
    public List<OrderDocument> saveOrderDocument(@PathVariable Long orderId,
                                                 @RequestParam Long warehouseId,
                                                 @RequestBody List<OrderDocument> orderDocuments) throws IOException {
        return orderDocumentService.saveOrderDocument(warehouseId, orderId, orderDocuments);
    }



    @RequestMapping(value="/orders/documents/{id}/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadOrderDocumentFile(@PathVariable Long id)
            throws FileNotFoundException {

        File orderDocumentFile = orderDocumentService.getFile(id);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(orderDocumentFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + orderDocumentFile.getName())
                .contentLength(orderDocumentFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(value="/orders/{warehouseId}/{orderId}/documents/download/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadOrderDocumentFile(@PathVariable Long warehouseId,
                                                              @PathVariable Long orderId,
                                                              @PathVariable String fileName)
            throws FileNotFoundException {

        File orderDocumentFile = orderDocumentService.getFile(warehouseId, orderId, fileName);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(orderDocumentFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + orderDocumentFile.getName())
                .contentLength(orderDocumentFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}

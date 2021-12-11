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

package com.garyzhangscm.cwms.inventory.controller;


import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.ItemSampling;
import com.garyzhangscm.cwms.inventory.service.ItemSamplingService;
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
public class ItemSamplingController {
    @Autowired
    ItemSamplingService itemSamplingService;

    @RequestMapping(value="/item-sampling", method = RequestMethod.GET)
    public List<ItemSampling> findAllItemSampling(
                            @RequestParam Long warehouseId,
                            @RequestParam(name="number", required = false, defaultValue = "") String number,
                            @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                            @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled,
                            @RequestParam(name="currentSampleOnly", required = false, defaultValue = "") Boolean currentSampleOnly) {
        return itemSamplingService.findAll(warehouseId, number, itemName,
                itemId,enabled,currentSampleOnly);
    }

    @BillableEndpoint
    @RequestMapping(value="/item-sampling", method = RequestMethod.PUT)
    public ItemSampling addItemSampling(
            @RequestParam Long warehouseId,@RequestBody ItemSampling itemSampling) {
        return itemSamplingService.addItemSampling(itemSampling);
    }


    @RequestMapping(value="/item-sampling/{id}", method = RequestMethod.GET)
    public ItemSampling findItemSampling(@PathVariable Long id) {

        return itemSamplingService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/item-sampling/{id}", method = RequestMethod.POST)
    public ItemSampling changeItemSampling(@PathVariable Long id,
                                               @RequestBody ItemSampling itemSampling){
        return itemSamplingService.changeItemSampling(id, itemSampling);
    }

    @BillableEndpoint
    @RequestMapping(value="/item-sampling/{id}", method = RequestMethod.DELETE)
    public void removeItemSampling(@PathVariable Long id) {
        itemSamplingService.removeItemSampling(id);
    }


    @RequestMapping(value="/item-sampling/images/{warehouseId}/{number}/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getItemSamplingImage(@PathVariable Long warehouseId,
                                                    @PathVariable String number,
                                                    @PathVariable String fileName) throws FileNotFoundException {

        File imageFile = itemSamplingService.getItemSamplingImage(warehouseId, number, fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(imageFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(imageFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/item-sampling/{number}/images")
    public ResponseBodyWrapper uploadQCSampleImage(
            @PathVariable String number,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = itemSamplingService.uploadQCSampleImage(number, file);
        return  ResponseBodyWrapper.success(filePath);
    }



}

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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.BillOfMaterialLineService;
import com.garyzhangscm.cwms.workorder.service.BillOfMaterialService;
import com.garyzhangscm.cwms.workorder.service.FileService;
import com.garyzhangscm.cwms.workorder.service.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class BillOfMaterialController {
    @Autowired
    BillOfMaterialService billOfMaterialService;
    @Autowired
    private BillOfMaterialLineService billOfMaterialLineService;
    @Autowired
    FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;


    @ClientValidationEndpoint
    @RequestMapping(value="/bill-of-materials", method = RequestMethod.GET)
    public List<BillOfMaterial> findAllBillOfMaterials(@RequestParam Long warehouseId,
                                                       @RequestParam(name="number", required = false, defaultValue = "") String number,
                                                       @RequestParam(name="numbers", required = false, defaultValue = "") String numbers,
                                                       @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                       @RequestParam(name="genericMatch", required = false, defaultValue = "false") boolean genericQuery,
                                                       ClientRestriction clientRestriction) {
        return billOfMaterialService.findAll(warehouseId, number, numbers,
                itemName, genericQuery, clientRestriction);
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

    @BillableEndpoint
    @RequestMapping(value="/bill-of-materials", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_BillOfMaterial", allEntries = true),
            }
    )
    public BillOfMaterial addBillOfMaterials(@RequestBody BillOfMaterial billOfMaterial) {
        return billOfMaterialService.addBillOfMaterials(billOfMaterial);
    }


    @RequestMapping(value="/bill-of-materials/{id}", method = RequestMethod.GET)
    public BillOfMaterial findBillOfMaterial(@PathVariable Long id) {
        return billOfMaterialService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/bill-of-materials/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_BillOfMaterial", allEntries = true),
            }
    )
    public BillOfMaterial changeBillOfMaterial(@PathVariable Long id,
                                               @RequestBody BillOfMaterial billOfMaterial){
        return billOfMaterialService.changeBillOfMaterial(id, billOfMaterial);
    }

    @BillableEndpoint
    @RequestMapping(value="/bill-of-materials", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_BillOfMaterial", allEntries = true),
            }
    )
    public void removeBillOfMaterials(@RequestParam(name = "billOfMaterialIds", required = false, defaultValue = "") String billOfMaterialIds) {
        billOfMaterialService.delete(billOfMaterialIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/bill-of-materials/validate-new-number", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateNewBOMNumber(@RequestParam Long warehouseId,
                                                            @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                                            @RequestParam String number) {
        return ResponseBodyWrapper.success(billOfMaterialService.validateNewBOMNumber(warehouseId, clientId, number));
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/bill-of-materials/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InventoryService_BillOfMaterial", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadBillOfMaterials(Long companyId, Long warehouseId,
                                                     @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                                     @RequestParam("file") MultipartFile file) throws IOException {


        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "BOMs", fileService.saveFile(file), ignoreUnknownFields);


            String fileUploadProgressKey = billOfMaterialLineService.saveBOMLineData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);

            // return  ResponseBodyWrapper.success(billOfMaterialLines.size() + "");
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }


    }

    @RequestMapping(method=RequestMethod.GET, value="/bill-of-materials/upload/progress")
    public ResponseBodyWrapper getBillOfMaterialsFileUploadProgress(Long warehouseId,
                                                            String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",billOfMaterialLineService.getBillOfMaterialsFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/bill-of-materials/upload/result")
    public List<FileUploadResult> getBillOfMaterialsFileUploadResult(Long warehouseId,
                                                                     String key) throws IOException {


        return billOfMaterialLineService.getBillOfMaterialsFileUploadResult(warehouseId, key);
    }


}

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

package com.garyzhangscm.cwms.inbound.controller;

import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.EulogiaService;
import com.garyzhangscm.cwms.inbound.service.FileService;
import com.garyzhangscm.cwms.inbound.service.UploadFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class EulogiaController {
    private static final Logger logger = LoggerFactory.getLogger(EulogiaController.class);

    @Autowired
    UploadFileService uploadFileService;
    @Autowired
    FileService fileService;
    @Autowired
    private EulogiaService eulogiaService;

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/eulogia/customer-packing-slip/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadCustomerPackingSlip(Long companyId,
                                                         Long warehouseId,
                                                         @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                                         @RequestParam("file") MultipartFile file) {



        try {
            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "eulogia-customer-packing-slip", fileService.saveFile(file), ignoreUnknownFields);
            // fileService.validateCSVFile(companyId, warehouseId, "receipts", localFile);
            String fileUploadProgressKey = eulogiaService.saveCustomerPackingSlipData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
    }


    @RequestMapping(method=RequestMethod.GET, value="/eulogia/customer-packing-slip/upload/progress")
    public ResponseBodyWrapper getCustomerPackingSlipFileUploadProgress(Long warehouseId,
                                                            String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",eulogiaService.getCustomerPackingSlipFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/eulogia/customer-packing-slip/upload/result")
    public List<FileUploadResult> getCustomerPackingSlipFileUploadResult(Long warehouseId,
                                                             String key) throws IOException {


        return eulogiaService.getCustomerPackingSlipFileUploadResult(warehouseId, key);
    }
}

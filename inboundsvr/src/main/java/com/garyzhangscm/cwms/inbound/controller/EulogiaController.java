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
    @RequestMapping(method=RequestMethod.POST, value="/eulogia/customer_packing_slip/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadCustomerPackingSlip(Long companyId,
                                              Long warehouseId,
                                              @RequestParam("file") MultipartFile file) {



        try {
            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "eulogia_customer_packing_slip", fileService.saveFile(file));
            // fileService.validateCSVFile(companyId, warehouseId, "receipts", localFile);
            String fileUploadProgressKey = eulogiaService.saveCustomerPackingSlipData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
    }
}

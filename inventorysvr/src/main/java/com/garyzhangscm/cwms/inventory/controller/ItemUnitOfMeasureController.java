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
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ItemUnitOfMeasureController {
    @Autowired
    ItemUnitOfMeasureService itemUnitOfMeasureService;

    @Autowired
    private FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/item-unit-of-measures/upload")
    public ResponseBodyWrapper uploadItemUnitOfMeasures(Long companyId, Long warehouseId,
                                                        @RequestParam("file") MultipartFile file) throws IOException {


        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "itemUnitOfMeasure", fileService.saveFile(file));

            String fileUploadProgressKey = itemUnitOfMeasureService.uploadItemUnitOfMeasureData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);

        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }



    }

    @RequestMapping(method=RequestMethod.GET, value="/item-unit-of-measures/upload/progress")
    public ResponseBodyWrapper getFileUploadProgress(Long warehouseId,
                                                     String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",itemUnitOfMeasureService.getFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/item-unit-of-measures/upload/result")
    public List<FileUploadResult> getFileUploadResult(Long warehouseId,
                                                      String key) throws IOException {


        return itemUnitOfMeasureService.getFileUploadResult(warehouseId, key);
    }


}

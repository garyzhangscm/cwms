/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.service.FileUploadColumnMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
public class FileUploadColumnMappingController {
    private static final Logger logger
            = LoggerFactory.getLogger(FileUploadColumnMappingController.class);


    @Autowired
    FileUploadColumnMappingService fileUploadColumnMappingService;



    @RequestMapping(value="/file-upload/column-mapping", method = RequestMethod.GET)
    public List<FileUploadColumnMapping> findAllFileUploadColumnMappings(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam String type,
            @RequestParam(name="columnName", required = false, defaultValue = "") String columnName) {
        return fileUploadColumnMappingService.findAll(companyId, warehouseId, type, columnName);
    }


    @RequestMapping(value="/file-upload/column-mapping/{id}", method = RequestMethod.POST)
    public FileUploadColumnMapping addFileUploadColumnMapping(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody FileUploadColumnMapping fileUploadColumnMapping) {

        return fileUploadColumnMappingService.changeFileUploadColumnMapping(id, fileUploadColumnMapping);
    }

    @RequestMapping(value="/file-upload/column-mapping", method = RequestMethod.PUT)
    public FileUploadColumnMapping addFileUploadColumnMapping(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestBody FileUploadColumnMapping fileUploadColumnMapping) {
        return fileUploadColumnMappingService.addFileUploadColumnMapping(fileUploadColumnMapping);
    }


}

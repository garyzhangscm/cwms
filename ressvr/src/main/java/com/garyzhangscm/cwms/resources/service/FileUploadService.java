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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.MissingInformationException;
import com.garyzhangscm.cwms.resources.model.FileUploadColumnMapping;
import com.garyzhangscm.cwms.resources.model.FileUploadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FileUploadService {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    @Autowired
    private FileUploadColumnMappingService fileUploadColumnMappingService;



    public List<FileUploadType> getFileUploadTypes(Long companyId, Long warehouseId) {

        List<FileUploadType> fileUploadTypes = FileUploadType.getAvailableFileUploadTypes();
        loadFileUploadColumnMapping(companyId, warehouseId, fileUploadTypes);

        return fileUploadTypes;
    }

    private void loadFileUploadColumnMapping(Long companyId, Long warehouseId, List<FileUploadType> fileUploadTypes) {
        fileUploadTypes.forEach(
                fileUploadType -> loadFileUploadColumnMapping(companyId, warehouseId, fileUploadType)
        );
    }

    private void loadFileUploadColumnMapping(Long companyId, Long warehouseId, FileUploadType fileUploadType) {
        if (Objects.isNull(fileUploadType)) {
            return;
        }
        logger.debug("start to load column mapping for type {}", fileUploadType.getName());
        List<FileUploadColumnMapping> fileUploadColumnMappings =
                fileUploadColumnMappingService.findAll(
                        companyId, warehouseId,
                        fileUploadType.getName(),
                        null
                );
        logger.debug("Found {} mapping for type {}", fileUploadColumnMappings.size(), fileUploadType.getName());

        Map<String, String> columnsMapping = new HashMap<>();
        fileUploadColumnMappings.forEach(
                fileUploadColumnMapping -> columnsMapping.put(fileUploadColumnMapping.getColumnName(), fileUploadColumnMapping.getMapToColumnName())
        );
        fileUploadType.setColumnsMapping(
                columnsMapping
        );
    }

    public FileUploadType getFileUploadType(Long companyId, Long warehouseId,  String typename) {
        List<FileUploadType> availableFileUploadTypes = FileUploadType.getAvailableFileUploadTypes();
        FileUploadType fileUploadType = availableFileUploadTypes.stream().filter(
                availableFileUploadType -> availableFileUploadType.getName().equalsIgnoreCase(typename)
        ).findFirst().orElse(null);

        loadFileUploadColumnMapping(companyId, warehouseId, fileUploadType);

        return fileUploadType;
    }


    public boolean validateCSVFile(Long companyId, Long warehouseId,
                                  String typename, String headers) {

        FileUploadType fileUploadType = getFileUploadType(companyId, warehouseId, typename);
        if (Objects.isNull(fileUploadType)) {
            throw MissingInformationException.raiseException("Can't find by type " + typename);
        }
        return validateCSVFile(fileUploadType, headers);
    }

    public boolean validateCSVFile(FileUploadType fileUploadType, String headers) {
        String[] csvFileHeaderNames = headers.split(",");

        // for each header name, make sure it is defined in the column
        // for any header name missing, make sure it is defind as optional

        // convert from the list of column into map to make it easy to process
        // key: header name
        // value: nullable column
        Map<String, Boolean> columnMap = new HashMap<>();
        List<String> missingRequiredColumn = new ArrayList<>();

        fileUploadType.getColumns().forEach(
                column -> {
                    if (!Boolean.TRUE.equals(column.getNullable())) {
                        // current column is defined as required, see if the CSV file
                        // has the column passed in
                        if (Arrays.stream(csvFileHeaderNames).noneMatch(
                                csvFileHeaderName -> csvFileHeaderName.equalsIgnoreCase(column.getName()))) {
                            missingRequiredColumn.add(
                                    // if we have column mapping defined for the CSV, then we will allow the
                                    // user to upload a file with either the required column or the mapping one
                                    fileUploadType.getColumnsMapping().containsKey(column.getName()) ?
                                            column.getName() + "(" + fileUploadType.getColumnsMapping().get(column.getName()) + ")" :
                                            column.getName());
                        }
                    }
                    columnMap.put(
                            column.getName().toLowerCase(Locale.ROOT),
                            column.getNullable()
                    );
                }
        );
        if (!missingRequiredColumn.isEmpty()) {

            throw MissingInformationException.raiseException("CSV file is missing follow columns: " + missingRequiredColumn);
        }

        List<String> invalidColumns = new ArrayList<>();
        for (String csvFileHeaderName : csvFileHeaderNames) {
            if (!columnMap.containsKey(csvFileHeaderName.toLowerCase(Locale.ROOT))) {
                invalidColumns.add(csvFileHeaderName);
            }
        }
        if (!invalidColumns.isEmpty()) {
            throw MissingInformationException.raiseException("CSV file is not in the right format. Unknown columns: " + invalidColumns);
        }

        return true;



    }
}

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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.MissingInformationException;
import com.garyzhangscm.cwms.workorder.exception.SystemFatalException;
import com.garyzhangscm.cwms.workorder.model.FileUploadType;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

@Service
public class UploadFileService {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileService.class);

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private ExcelFileHandler excelFileHandler;
    @Autowired
    private FileService fileService;

    public File convertToCSVFile(Long companyId, Long warehouseId, String type, File file, Boolean ignoreUnknownFields) throws IOException {
        File csvFile = null;
        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("csv")) {
            logger.debug("The file is a CSV file, we will return it without convert");
            csvFile = replaceCSVHeaderByMappingField(companyId, warehouseId, type, file);
        }
        else if (FilenameUtils.isExtension(file.getName(),"xls") || FilenameUtils.isExtension(file.getName(),"xlsx")) {
            logger.debug("The file is a Excel file, we will convert it to CSV file first");
            FileUploadType fileUploadType = getFileUploadType(companyId, warehouseId, type);
            csvFile =  replaceCSVHeaderByMappingField(fileUploadType, excelFileHandler.convertExcelToCSV(file, fileUploadType));
        }
        else {
            throw MissingInformationException.raiseException("Can't recognize the file " + file.getName() +
                    ". The format and extension is not support");
        }
        validateCSVFile(companyId, warehouseId, type, csvFile, ignoreUnknownFields);

        return csvFile;
    }

    public FileUploadType getFileUploadType(Long companyId, Long warehouseId,
                                            String type) {
        return resourceServiceRestemplateClient.getFileUploadType(
                companyId, warehouseId, type
        );
    }

    /**
     * Replace the CSV file's header based on the field mapping defined in the file upload type
     * @param csvFile
     * @param type
     * @return
     */
    public File replaceCSVHeaderByMappingField(Long companyId, Long warehouseId, String type, File csvFile) throws IOException {

        return replaceCSVHeaderByMappingField(getFileUploadType(companyId, warehouseId, type), csvFile);
    }
    /**
     * Replace the CSV file's header based on the field mapping defined in the file upload type
     * @param csvFile
     * @param type
     * @return
     */
    public File replaceCSVHeaderByMappingField(FileUploadType type, File csvFile) throws IOException {
        String header = getCSVFileHeader(csvFile);
        logger.debug("start to replace the original CSV file header with mapping. \n{}", header);
        // let's replace the header with values from the mapping
        String[] columnNames = header.split(",");

        // key: needed by the system, used to map to the POJO
        // value: defined by the user. The user can upload a CSV or Excel with this column and
        //       we will translate it to the column name defined by key
        Map<String, String> columnMappingMap = type.getColumnsMapping();

        logger.debug("column maps >>>");
        columnMappingMap.entrySet().forEach(
                entry -> logger.debug("key: {}, value: {}", entry.getKey(), entry.getValue())
        );
        for(int i = 0; i < columnNames.length; i++) {
            // find the mapping to column
            String columnName = columnNames[i];
            if (columnName.startsWith("\"")) {
                columnName = columnName.substring(1);
            }
            if (columnName.endsWith("\"")) {
                columnName = columnName.substring(0, columnName.length() - 1);
            }

            logger.debug("Check if we will need to replace the column name {}", columnName);
            // we will always assume that
            // 1. the mapping to column(user defined column name) won't conflict with
            //    the system's required column name or other mapping column name
            for (Map.Entry<String, String> entry : columnMappingMap.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(columnName)) {
                    // we found a match
                    logger.debug("Found a match at index {}, original value is {}, we will update to {}",
                            i, columnNames[i], entry.getKey());
                    columnNames[i] = "\"" + entry.getKey() + "\"";
                    break;
                }
            }
            logger.debug("> new column name {}", columnNames[i]);
        }

        // new header
        String newHeader = String.join(",", columnNames);

        logger.debug("> new columns:\n{}", newHeader);
        if (header.equalsIgnoreCase(newHeader)) {

            logger.debug("new header is the same as the original header, no need to process");
            return csvFile;
        }
        else {
            logger.debug("The new header is different from the original one, let's replace it and save it back to the same file");
            return replaceCSVHeader(csvFile, newHeader);
        }


    }

    private File replaceCSVHeader(File csvFile, String newHeader) throws IOException {

        BufferedReader br = null;
        br = new BufferedReader(new FileReader(csvFile));
        // read the header
        br.readLine();

        String line = br.readLine();
        String content = newHeader + "\n";
        while(Strings.isNotBlank(line)) {
            content += line + "\n";
            line = br.readLine();
        }

        logger.debug("after replace the header, the new content is \n{}", content);

        return fileService.saveCSVFile(csvFile.getName(), content);

    }

    private String getCSVFileHeader(File csvFile) throws IOException {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader(csvFile));
        return br.readLine();
    }

    public void validateCSVFile(Long companyId, Long warehouseId,
                                String type,
                                File file, Boolean ignoreUnknownFields) {
        // we will assume the first line of the file is the hader of the CSV file

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String header = br.readLine();
            if (header != null) {
                validateCSVFile(companyId, warehouseId, type, header, ignoreUnknownFields);
            }
            else {
                logger.debug("Can't get header information from file {}", file);

                throw SystemFatalException.raiseException(
                        "CSV file " + file.getName() + " is missing the header");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException(
                    "CSV file " + file.getName() + " is not in the right format for type " + type
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException(
                    "CSV file " + file.getName() + " is not in the right format for type " + type
            );
        }
    }

    public void validateCSVFile(Long companyId, Long warehouseId,
                                String type, String headers, Boolean ignoreUnknownFields) {
        // remove all " before we can validate the CSV file's header
        headers = headers.replace("\"", "");
        String result = resourceServiceRestemplateClient.validateCSVFile(
                companyId, warehouseId, type, headers, ignoreUnknownFields);

        if (Strings.isNotBlank(result)) {
            logger.debug("Get error while validate CSV file of type {}, \n{}",
                    type, result);
            throw SystemFatalException.raiseException(result);
        }
    }
}

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

package com.garyzhangscm.cwms.inventory.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.SystemFatalException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    @Value("${fileupload.temp-file.directory:/upload/tmp/}")
    String destinationFolder;

    @Autowired
    private ExcelFileHandler excelFileHandler;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public File saveFile(MultipartFile file) throws IOException {
        String destination = destinationFolder  + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        return saveFile(file, destination);

    }

    public File saveFile(MultipartFile file, String destination) throws IOException {

        return saveFile(file, new File(destination));

    }

    public File saveFile(MultipartFile file, File destinationFile) throws IOException {


        if (!destinationFile.getParentFile().exists()) {
            logger.debug("save files: parent file {} doesn't exists",
                    destinationFile.getParentFile().getAbsolutePath());
            destinationFile.getParentFile().mkdirs();
        }
        if (!destinationFile.exists()) {
            logger.debug("save files: local file {} doesn't exists",
                    destinationFile.getAbsolutePath());
            destinationFile.createNewFile();
        }
        file.transferTo(destinationFile);
        logger.debug("save files: file copied!");

        return destinationFile;

    }

    public <T> List<T> loadData(File file, Class<T> tClass)throws IOException {
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema bootstrapSchema = CsvSchema.emptySchema() //
                .withHeader() //
                .withColumnSeparator(',');

        ObjectReader reader = csvMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
                .readerFor(tClass) //
                .with(bootstrapSchema);

        MappingIterator<T> iterator;
        try {
            iterator = reader.readValues(new FileInputStream(file));
        } catch (IOException e) {
            throw new IllegalStateException(String.format("could not access file " + file.getName()), e);
        }
        List<T> results = new ArrayList<>();
        iterator.forEachRemaining(results::add);
        return results;
    }

    public <T> List<T> loadData(File file, CsvSchema schema, Class<T> tClass)throws IOException {

        List<T> ts = new ArrayList<>();

        CsvMapper mapper = new CsvMapper();


        // read from file
        try (Reader reader = new FileReader(file)) {
            MappingIterator<T> mappingIterator = mapper.readerFor(tClass).with(schema).readValues(reader);
            return mappingIterator.readAll();
        }
    }


    public <T> List<T> loadData(String csvFileContent, CsvSchema schema, Class<T> tClass)throws IOException {
        List<T> ts = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();

        // read from string
        MappingIterator<T> mappingIterator = mapper.readerFor(tClass).with(schema).readValues(csvFileContent);
        return mappingIterator.readAll();
    }

    public <T> List<T> loadData(InputStream csvInputStream, CsvSchema schema, Class<T> tClass)throws IOException {
        List<T> ts = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();

        // read from input stream
        MappingIterator<T> mappingIterator = mapper.readerFor(tClass).with(schema).readValues(csvInputStream);
        return mappingIterator.readAll();
    }

    public void createCSVFiles(String directory, String fileName, String header, List<String> rows ) throws FileNotFoundException {

        File csvOutputFile = new File(directory + "/" + fileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            // write the header
            pw.println(header);
            // write the details
            rows.stream().forEach(pw::println);
        }
    }

    public void copyFile(String sourceFilePath, String destinationFilePath)
            throws IOException {

        File sourceFile = new File(sourceFilePath);
        File destinationFile = new File(destinationFilePath);

        copyFile(sourceFile, destinationFile);
    }
    public void copyFile(File sourceFile, File destinationFile)
            throws IOException {

        // InputStream in = new BufferedInputStream(
        //        new FileInputStream(sourceFile));


        // copyFile(in, destinationFile);

        FileUtils.copyFile(sourceFile, destinationFile);

    }

    public File saveFile(MultipartFile file, String folder, String fileName) throws IOException {
        if (!folder.endsWith("/")) {
            folder += "/";
        }
        String destination = folder  + fileName;
        File localFile = new File(destination);

        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }
        if (!localFile.exists()) {
            localFile.createNewFile();
        }
        file.transferTo(localFile);

        return localFile;

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
        String result = resourceServiceRestemplateClient.validateCSVFile(
                companyId, warehouseId, type, headers, ignoreUnknownFields);
        if (Strings.isNotBlank(result)) {
            logger.debug("Get error while validate CSV file of type {}, \n{}",
                    type, result);
            throw SystemFatalException.raiseException(result);
        }
    }


    public File saveCSVFile(String fileName, String content) throws IOException {
        String destination = destinationFolder  + System.currentTimeMillis() + "_" + fileName;
        File localFile = new File(destination);

        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }


        Files.write(Paths.get(destination), content.getBytes("UTF-8"));

        localFile = new File(destination);
        logger.debug("The content is saved to the file {}, \n{}",
                destination, content);
        return localFile;
    }
}

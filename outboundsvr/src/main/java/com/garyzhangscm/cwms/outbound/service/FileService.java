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

package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.MissingInformationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.SystemFatalException;
import com.garyzhangscm.cwms.outbound.model.FileUploadType;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Value("${fileupload.temp-file.directory:/upload/tmp/}")
    String destinationFolder;

    @Autowired
    private ExcelFileHandler excelFileHandler;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public File processUploadedFile(String type, MultipartFile file) throws IOException {
        FileUploadType fileUploadType = resourceServiceRestemplateClient.getFileUploadType(type);
        if (Objects.isNull(fileUploadType)) {
            throw ResourceNotFoundException.raiseException("can't recognize the type " + type
             + " to upload file");
        }
        String destination = destinationFolder  + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File localFile = new File(destination);

        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }
        if (!localFile.exists()) {
            localFile.createNewFile();
        }
        file.transferTo(localFile);

        if (!fileUploadType.getColumnsMapping().isEmpty()) {
            logger.debug("Column mapping for CSV type {} is not empty, let's process the local file and replace the header",
                    type);
            localFile = replaceCSVHeader(localFile, fileUploadType.getColumnsMapping());
            logger.debug("after replace, the local file content is >>>");
            displayFileContent(localFile, true);

        }
        return localFile;

    }

    /**
     * Replace CSV header with the column map,
     * key: from column, the column name from the CSV that uploaded
     * value: to column, the column name required by the system
     * @param localFile
     * @param columnsMapping
     * @return
     */
    private File replaceCSVHeader(File localFile, Map<String, String> columnsMapping) throws IOException {
        Scanner scanner = new Scanner(localFile);
        StringBuffer csvContent = new StringBuffer();
        String csvHeader = "";

        int lineNumber = 1;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (Strings.isBlank(csvHeader)) {
                logger.debug("Line # {} is the header", lineNumber);
                csvHeader = line;
            }
            else if (csvHeader.equalsIgnoreCase(line)) {
                // ok we already have a csv header and we meet a second line
                // that has exactly the same content as the header, it may
                // indicate a duplicated header line which we will ignore
                logger.debug("Ignore line # {} as it has the same content as the header", lineNumber);
                lineNumber++;
                continue;
            }
            else {
                // add the content to the line;
                logger.debug("Line # {} is the content", lineNumber);
                csvContent.append( line + System.lineSeparator());
                lineNumber++;
            }
        }
        //closing the Scanner object
        scanner.close();

        if (Strings.isBlank(csvHeader)) {
            logger.debug("Fail to find the header for file {}, we will just return the same file without any further process",
                    localFile.getAbsolutePath());
            return localFile;
        }

        String fileContents = csvContent.toString();
        logger.debug("Contents of the file: "+fileContents);

        String[] oldHeaderColumns = csvHeader.split(",");
        // save the column map to handle case insensitive
        Map<String, String> caseInsensitiveColumnsMapping = new HashMap<>();
        columnsMapping.entrySet().forEach(
                entry ->
                    caseInsensitiveColumnsMapping.put(
                            entry.getKey().toLowerCase(Locale.ROOT).trim(),
                            entry.getValue()
                    )
        );
        String newHeader = Arrays.stream(oldHeaderColumns).map(
                columnName -> caseInsensitiveColumnsMapping.getOrDefault(
                        columnName.toLowerCase(Locale.ROOT).trim(), columnName
                )
        ).collect(Collectors.joining(","));

        logger.debug("========   original header   ========\n {}", csvHeader);
        logger.debug("========   new header   ========\n {}", newHeader);

        //Replacing the old line with new line
        fileContents = newHeader + System.lineSeparator() + fileContents;
        //instantiating the FileWriter class
        FileWriter writer = new FileWriter(localFile.getAbsolutePath());

        writer.append(fileContents);
        writer.flush();

        return localFile;
    }

    public File saveFile(MultipartFile file) throws IOException {
        String destination = destinationFolder  + System.currentTimeMillis() + "_" + file.getOriginalFilename();
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

    public void removeFile(String folder, String fileName) throws IOException {

        if (!folder.endsWith("/")) {
            folder += "/";
        }
        String destination = folder  + fileName;
        File localFile = new File(destination);
        localFile.deleteOnExit();
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
    public boolean fileExists(String folder, String fileName) {
        if (!folder.endsWith("/")) {
            folder += "/";
        }
        String destination = folder  + fileName;
        File localFile = new File(destination);

        return localFile.exists();
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

    public void displayFileContent(File file, boolean withLineNumber) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        int lineNumber = 0;

        while (scanner.hasNextLine()) {
            if (withLineNumber) {

            }
            logger.debug((withLineNumber? "{}. " : "") + scanner.nextLine(), lineNumber++);
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

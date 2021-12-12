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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    @Value("${fileupload.temp-file.directory:/upload/tmp/}")
    String destinationFolder;

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
}

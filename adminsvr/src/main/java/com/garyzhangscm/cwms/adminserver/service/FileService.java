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

package com.garyzhangscm.cwms.adminserver.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

    @Value("${fileupload.temp-file.directory:/upload/tmp/}")
    String destinationFolder;

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


    public void createCSVFile(String filePath, String[] header, List<Object[]> data) throws IOException {
        File file = new File(filePath);


        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }

        // FileWriter out = new FileWriter(file);

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(file), CSVFormat.EXCEL)) {
            printer.printRecord(header);
            for(Object[] line: data) {
                printer.printRecord(line);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public Set<File> listFilesInDirectory(String directoryPath) {
        return Stream.of(new File(directoryPath).listFiles())
                .collect(Collectors.toSet());
    }

    public File zipFilesInDirectory(String zipFileName, String directoryPath) throws IOException {
        Set<File> srcFiles = listFilesInDirectory(directoryPath);

        FileOutputStream fos = new FileOutputStream(zipFileName);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (File srcFile : srcFiles) {

            FileInputStream fis = new FileInputStream(srcFile);
            ZipEntry zipEntry = new ZipEntry(srcFile.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();

        return new File(zipFileName);
    }
}

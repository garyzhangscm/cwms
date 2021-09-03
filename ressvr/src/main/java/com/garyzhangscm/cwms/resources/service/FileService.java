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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {
    private static final Logger logger
            = LoggerFactory.getLogger(FileService.class);

    @Value("${fileupload.temp-file.directory:/upload/tmp/}")
    String destinationFolder;

    public File saveFile(MultipartFile file) throws IOException {
        return saveFile(file, destinationFolder);

    }

    public File saveFile(MultipartFile file, String folder ) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        return saveFile(file, destinationFolder, fileName);

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
    public void copyFile(InputStream sourceFile, File destinationFile)
            throws IOException {

        // create the destination file is it doesn't exists yet
        // Remove if it is already exists
        if (!destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }
        destinationFile.deleteOnExit();
        // create an empty file
        destinationFile.createNewFile();


        BufferedReader in = new BufferedReader(
                  new InputStreamReader(sourceFile, "UTF-8"));

        Path destinationPath = destinationFile.toPath();

        try (BufferedWriter bufferedWriter
                       = Files.newBufferedWriter(
                          destinationPath, StandardCharsets.UTF_8)) {

            String readLine;
            while ((readLine = in.readLine()) != null) {
                // logger.debug("read line: {}" , readLine);
                bufferedWriter.append(readLine);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {

        logger.debug("start to copy directory from absolute path {} to {}",
                sourceDirectory.getAbsolutePath(),
                destinationDirectory.getAbsolutePath());
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdir();
        }
        for (String f : sourceDirectory.list()) {
            logger.debug(">> copy file(directory) {}", f);
            copyDirectoryCompatibityMode(new File(sourceDirectory, f), new File(destinationDirectory, f));
        }
    }
    public void copyDirectoryCompatibityMode(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            logger.debug(">>>> {} is a directory, copy directory", source.getAbsolutePath());
            copyDirectory(source, destination);
        } else {
            logger.debug(">>>> {} is a file, copy file", source.getAbsolutePath());
            copyFile(source, destination);
        }
    }

    public void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {

        logger.debug("start to copy directory from {} to {}",
                sourceDirectoryLocation,
                destinationDirectoryLocation);
        copyDirectory(new File(sourceDirectoryLocation), new File(destinationDirectoryLocation));
    }
}

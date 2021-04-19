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

package com.garyzhangscm.cwms.resources;

import com.garyzhangscm.cwms.resources.model.Report;
import com.garyzhangscm.cwms.resources.service.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class MyApplicationRunner implements ApplicationRunner {
    // WE will init the menu when start the application

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);
    @Autowired
    MenuGroupService menuGroupService;
    @Autowired
    MenuSubGroupService menuSubGroupService;
    @Autowired
    MenuService menuService;
    @Autowired
    ReportService reportService;
    @Autowired
    FileService fileService;

    @Autowired
    private ResourceLoader resourceLoader;


    @Value("${report.template.folder}")
    private String reportTemplateFolder;
    @Value("${report.result.folder}")
    private String reportResultFolder;
    @Value("${report.template.resourceFolder}")
    private String reportTemplateResourceFolder;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Start to init the menus");
        menuGroupService.initTestData( null, "");
        menuSubGroupService.initTestData(null, "");
        menuService.initTestData(null, "");

        initReportData();
    }

    private void initReportData() throws IOException {
        reportService.initTestData(null, "");
        System.out.println("Start to init the standard reports");

        createReportFolders();

        copyReportFiles();

        logger.debug("Java classpath\n {}",
                System.getProperty("java.class.path"));

    }
    private void createReportFolders() {

        // create folder for report template
        File templateFolder = new File(reportTemplateFolder);
        if (!templateFolder.exists()) {
            templateFolder.mkdirs();
        }


        // create folder for report result
        File resultFolder = new File(reportResultFolder);
        if (!resultFolder.exists()) {
            resultFolder.mkdirs();
        }

    }
    private void copyReportFiles() throws IOException {

        Resource[] resources = getAllReportFiles();

        logger.debug("start to copy {} report files",
                resources.length);

        for(Resource resource : resources) {

            copyReportFile(resource);

        }

    }

    private void copyReportFile(Resource resource) throws IOException {

        InputStream inputStream = resource.getInputStream();
        // create the destination directory
        String destinationFilePath =
                reportTemplateFolder + "/" + resource.getFilename();
        logger.debug("copy {} >>>>> {}",
                resource.getURL(), destinationFilePath);
        File destinationFile = new File(destinationFilePath);
        // Remove if it is already exists
        destinationFile.deleteOnExit();
        // create an empty file
        destinationFile.createNewFile();


        fileService.copyFile(inputStream, destinationFile);

    }


    private Resource[]  getAllReportFiles()
            throws IOException {


        logger.debug("start to get files from {}",
                reportTemplateResourceFolder);

        Resource[] resources = loadResources("classpath*:" + reportTemplateResourceFolder + "/*.*");
        logger.debug("Get {} resource", resources.length);
        for (Resource resource : resources) {
            logger.debug(">> {}", resource.getFilename());
        }

        return resources;
    }


    private Resource[] loadResources(String pattern) throws IOException {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(pattern);
    }



}

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

package com.garyzhangscm.cwms.resources.clients;

import com.garyzhangscm.cwms.resources.exception.GenericException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.SystemFatalException;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;


@Component
public class LabelaryRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(LabelaryRestemplateClient.class);


    private String getZPLContentFromFile(File zplFile) {
        String zpl = "";
        Scanner scanner = null;
        try {
            scanner = new Scanner(zplFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw ResourceNotFoundException.raiseException("Can't find the zpl file " + zplFile.getAbsolutePath() +
                    ", can't convert a non exist file to PDF");
        }
        while (scanner.hasNextLine()) {
            zpl += scanner.nextLine();
        }
        scanner.close();
        return zpl;
    }

    public File convertZPLToPDF(File zplFile)  {
        return convertZPLToPDF(zplFile, 4, 6);
    }
    public File convertZPLToPDF(File zplFile, int labelWidth, int labelHeight) {
        String zpl = getZPLContentFromFile(zplFile);

        logger.debug("We will send zpl to labelary for PDF file: \n{}", zpl);
        // labelary only support at max 50 labels in the PDF file so we have to make sure there're at max 50 labels
        // in the ZPl. otherwise we will need to split
        // Anything between ^XA and ^XZ are consider one label
        List<String> labels = splitLabels(zpl, 50);

        /**
        byte[] pdfFileContent = convertZPLToPDF(labels, labelWidth, labelHeight);

        return savePDFContent(zplFile.getParentFile().getAbsolutePath(), zplFile.getName() + ".pdf",
                pdfFileContent);
         **/
        try {
            return convertZPLToPDF(labels, labelWidth, labelHeight,
                    zplFile.getParentFile().getAbsolutePath(), zplFile.getName());
        } catch (IOException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException("Can't convert label file  to PDF file by labelary, error occur: " + e.getMessage());
        }
    }


    public File convertZPLToPDF(List<String> labels, int labelWidth, int labelHeight,
                                String filePath, String fileName) throws IOException {

        logger.debug("We got {} label files" , labels.size());
        if (labels.size() == 1) {
            // if we only have one label group(within the labelary's limit
            // return it directly. We don't have to generate separate PDF files
            // for groups to make each group within labelary's limit(50 labels per request)
            return convertZPLToPDF(labels.get(0), labelWidth, labelHeight,
                    filePath, fileName);
        }

        List<File> pdfFiles = new ArrayList<>();
        for(int i = 0; i < labels.size(); i++) {
            // convert each label group into PDF and then we will need to combine the
            // pdf files into one
            pdfFiles.add(
                    convertZPLToPDF(labels.get(i), labelWidth, labelHeight,
                            filePath, fileName + "_" + i));

        }
        logger.debug("Since we have more than 1 label files, we will need to combine the pdf files");
        return combinePDFFile(filePath, fileName, pdfFiles);
    }

    public File convertZPLToPDF(String zpl, int labelWidth, int labelHeight,
                                String filePath, String fileName)   {

        // adjust print density (8dpmm), label width (4 inches), label height (6 inches), and label index (0) as necessary
        // var uri = URI.create("http://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/");
        var uri = URI.create("http://api.labelary.com/v1/printers/8dpmm/labels/4x6/");
        var request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/pdf") // omit this line to get PNG images back
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(zpl))
                .build();
        var client = HttpClient.newHttpClient();
        HttpResponse<byte[]> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException("Can't send http request to api.labelary.com. IO Exception: " +
                    e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException("Can't send http request to api.labelary.com. InterruptedException: " +
                    e.getMessage());
        }
        logger.debug("Got return from labelary");
        logger.debug("response with status code {}", response.statusCode());
        var body = response.body();
        logger.debug("Body length: {}", body.length);

        if (response.statusCode() == 200) {
            return savePDFContent(filePath, fileName, body);
        } else {
            var errorMessage = new String(body, StandardCharsets.UTF_8);
            throw SystemFatalException.raiseException("Can't convert label file  to PDF file by labelary, error occur: " + errorMessage);
        }


    }

    private File savePDFContent(String filePath, String fileName, byte[] pdfFileContent) {
        File file = new File(filePath, fileName + ".pdf");

        logger.debug("we will save the pdf content into local file: {}, pdf content size: {}", file.getAbsolutePath(),
                pdfFileContent.length);
        file.deleteOnExit();
        try {
            logger.debug("start to write into this local file");
            Files.write(file.toPath(), pdfFileContent);
            logger.debug("writing is done!");
        } catch (IOException e) {
            e.printStackTrace();
            throw SystemFatalException.raiseException("Can't  write the PDF file from labelary into file : " +
                    file.getAbsolutePath() + ", got IOException : " +
                    e.getMessage());
        }

        logger.debug("we successfully convert the zpl file {} to a pdf file {}, new file size is {} kb",
                fileName, file.getAbsolutePath(),
                (double) file.length() / 1024  );
        return file;

    }

    /**
     * the argument ZPL may contains multiple labels in one string. Since labelary has limit on the request call, we
     * may need to split the string so that
     * # Maximum 5 requests per second per client. Additional requests result in a HTTP 429 (Too Many Requests) error.
     * # Maximum 50 labels per request. Additional labels result in a HTTP 413 (Payload Too Large) error. See the FAQ for details.
     * @param zpl
     * @param labelInEachString
     * @return
     */
    private List<String> splitLabels(String zpl, int labelInEachString) {
        // split the zpl into one label per string, then we will
        // group labelInEachString of labels into one string and
        // return
        // anything between ^XA and ^XZ are consider as one label
        List<String> individualLabels = splitLabelsIntoIndividualLabel(zpl);
        if (individualLabels.size() <= labelInEachString) {
            return List.of(zpl);
        }

        // we know we have more labels than required, let's split into
        // a list of label groups
        List<String> result = new ArrayList<>();
        int startIndex = 0;
        int endIndex = labelInEachString;
        while(startIndex < individualLabels.size()) {
            String label = individualLabels.subList(startIndex, endIndex).stream().collect(Collectors.joining());
            result.add(label);
            startIndex = endIndex;
            endIndex = Math.min(endIndex + labelInEachString, individualLabels.size());
        }

        logger.debug("After process, we get {} label group files", result.size());

        for(int i = 0; i < result.size(); i ++) {
            logger.debug(">>>>>>>>>>   labels {}: <<<<<<<<\n {}",
                    i, result.get(i));

        }
        return result;

    }

    /**
     * Split a zpl file into individual labels, one per string
     * the zpl may contains multiple labels
     * anything between ^XA and ^XZ are consider as one label
     * @param zpl
     * @return
     */
    private List<String> splitLabelsIntoIndividualLabel(String zpl) {
        int startIndex = zpl.indexOf("^XA");
        int endIndex = zpl.indexOf("^XZ");
        List<String> result = new ArrayList<>();
        logger.debug("start to split zpl into individuals :\n{}", zpl);
        while(startIndex > 0 && endIndex > 0 && startIndex < endIndex) {
            logger.debug("extract label from the zpl: \n{}",
                    zpl.substring(startIndex, endIndex + 3));
            result.add(
                    zpl.substring(startIndex, endIndex + 3)
            );

            zpl = zpl.substring(endIndex + 3);
            logger.debug("after extract the last label, now we have labels left:\n{}",
                    zpl);

            startIndex = zpl.indexOf("^XA");
            endIndex = zpl.indexOf("^XZ");
        }
        logger.debug("we extract {} labels from the original ZPL string",
                result.size());
        for(int i = 0; i < result.size(); i ++) {
            logger.debug("===========   label {}: =======\n {}",
                    i, result.get(i));

        }
        return result;

    }

    private byte[] combineByteArray(byte[] byteArray1, byte[] byteArray2) {
        if (byteArray1 == null) {
            logger.debug("combineByteArray: byteArray1 is null, return byteArray2 with length {}",
                    byteArray2.length);
            return byteArray2;
        }
        if (byteArray2 == null) {
            logger.debug("combineByteArray: byteArray2 is null, return byteArray1 with length {}",
                    byteArray1.length);
            return byteArray1;
        }
        byte[] combinedByteArray = new byte[byteArray1.length + byteArray2.length];

        logger.debug("combineByteArray: create a combined byte array with length {}",
                combinedByteArray.length);
        ByteBuffer buff = ByteBuffer.wrap(combinedByteArray);
        buff.put(byteArray1);
        buff.put(byteArray2);

        return buff.array();
    }

    private File combinePDFFile(String resultFilePath, String resultFileName, List<File> sourceFiles) throws IOException {
        PDFMergerUtility PDFmerger = new PDFMergerUtility();
        Path filePath = Paths.get(resultFilePath, resultFileName + ".pdf");
        logger.debug("We will combine pdf file {} and save to {}",
                sourceFiles.stream().map(file -> file.getAbsolutePath()).collect(Collectors.joining(";")),
                filePath.toString());

        PDFmerger.setDestinationFileName(filePath.toString());

        for (File sourceFile : sourceFiles) {
            PDFmerger.addSource(sourceFile);

        }

        PDFmerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        return filePath.toFile();
    }


}

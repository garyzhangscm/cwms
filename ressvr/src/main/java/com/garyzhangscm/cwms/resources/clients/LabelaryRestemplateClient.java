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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;


@Component
public class LabelaryRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(LabelaryRestemplateClient.class);


    public File convertZPLToPDF(File zplFile)  {
        return convertZPLToPDF(zplFile, 4, 6);
    }
    public File convertZPLToPDF(File zplFile, int labelWidth, int labelHeight)   {
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


        logger.debug("We will send zpl to labelary for PDF file: \n{}", zpl);
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
            File file = new File(zplFile.getParentFile().getAbsolutePath(), zplFile.getName() + ".pdf");

            logger.debug("we will save the response into local file: {}", file.getAbsolutePath());
            file.deleteOnExit();
            try {
                logger.debug("start to write into this local file");
                Files.write(file.toPath(), body);
                logger.debug("writing is done!");
            } catch (IOException e) {
                e.printStackTrace();
                throw SystemFatalException.raiseException("Can't  write the PDF file from labelary into file : " +
                        file.getAbsolutePath() + ", got IOException : " +
                        e.getMessage());
            }

            logger.debug("we successfully convert the zpl file {} to a pdf file {}, new file size is {} kb",
                    zplFile, file.getAbsolutePath(),
                     (double) file.length() / 1024  );
            return file;
        } else {
            var errorMessage = new String(body, StandardCharsets.UTF_8);
            throw SystemFatalException.raiseException("Can't convert label file " + zplFile.getName() +
                    " to PDF file by labelary, error occur: " + errorMessage);
        }


    }

}

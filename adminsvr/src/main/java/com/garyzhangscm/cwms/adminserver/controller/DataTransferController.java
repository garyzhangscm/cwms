package com.garyzhangscm.cwms.adminserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.exception.SystemFatalException;
import com.garyzhangscm.cwms.adminserver.model.DataInitialRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.service.DataService;
import com.garyzhangscm.cwms.adminserver.service.DataTransferRequestService;
import com.garyzhangscm.cwms.adminserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Create initial data
 */

@RestController
public class DataTransferController {

    @Autowired
    DataTransferRequestService dataTransferRequestService;



    @RequestMapping(value="/data-transfer", method = RequestMethod.GET)
    public List<DataTransferRequest> getDataTransferRequests(@RequestParam(name = "companyCode", required = false, defaultValue = "") String companyCode,
                                                            @RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
                                                            @RequestParam(name = "number", required = false, defaultValue = "") String number,
                                                            @RequestParam(name = "status", required = false, defaultValue = "") String status) throws JsonProcessingException {

        return dataTransferRequestService.findAll(number, companyId, companyCode, status);
    }

    @RequestMapping(value="/data-transfer/{id}", method = RequestMethod.GET)
    public DataTransferRequest getDataTransferRequest(@PathVariable Long id) {

        return dataTransferRequestService.findById(id);
    }

    @RequestMapping(value="/data-transfer", method = RequestMethod.PUT)
    public DataTransferRequest addDataTransferRequest(@RequestParam String number,
                                                      @RequestParam Long companyId,
                                                      @RequestParam String description,
                                                      @RequestParam String type) {

        return dataTransferRequestService.addDataTransferRequest(number, companyId, description, type);
    }



    @RequestMapping(method=RequestMethod.GET, value="/data-transfer/csv-download/{id}/{tableName}")
    public ResponseEntity<Resource> getCSVFile(
            @PathVariable Long id,
            @PathVariable String tableName) throws FileNotFoundException {



        File csvFile = dataTransferRequestService.getCSVFile(id, tableName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(csvFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + csvFile.getName())
                .contentLength(csvFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }



    @RequestMapping(method=RequestMethod.GET, value="/data-transfer/csv-download/{id}")
    public ResponseEntity<Resource> getCSVZipFile(
            @PathVariable Long id) throws IOException {



        File csvZipFile = dataTransferRequestService.getCSVZipFile(id);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(csvZipFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + csvZipFile.getName())
                .contentLength(csvZipFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }



}

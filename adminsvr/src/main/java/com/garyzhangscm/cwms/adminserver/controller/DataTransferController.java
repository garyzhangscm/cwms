package com.garyzhangscm.cwms.adminserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.exception.SystemFatalException;
import com.garyzhangscm.cwms.adminserver.model.DataInitialRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.service.DataService;
import com.garyzhangscm.cwms.adminserver.service.DataTransferRequestService;
import com.garyzhangscm.cwms.adminserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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






}

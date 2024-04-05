package com.garyzhangscm.cwms.adminserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.exception.GenericException;
import com.garyzhangscm.cwms.adminserver.exception.SystemFatalException;
import com.garyzhangscm.cwms.adminserver.model.DataInitialRequest;
import com.garyzhangscm.cwms.adminserver.model.tester.TestScenarioSuit;
import com.garyzhangscm.cwms.adminserver.service.DataService;
import com.garyzhangscm.cwms.adminserver.service.TestScenarioService;
import com.garyzhangscm.cwms.adminserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Create initial data
 */

@RestController
public class DataController {

    @Autowired
    DataService dataService;

    @Autowired
    UserService userService;


    @RequestMapping(value="/data/company/initiate", method = RequestMethod.PUT)
    public Boolean initiateProductionData(@RequestParam String newCompanyName,
                                          @RequestParam String newWarehouseName,
                                          @RequestParam String adminUsername,
                                          @RequestParam(name = "production", required = false, defaultValue = "") Boolean production) throws JsonProcessingException {


        String requestUsername = userService.getCurrentUserName();
        dataService.initiateProductionData(newCompanyName, newWarehouseName, adminUsername, requestUsername, production);
        return true;
    }


    @RequestMapping(value="/data/company/initiate/{id}", method = RequestMethod.GET)
    public DataInitialRequest initiateProductionData(@PathVariable Long id) throws JsonProcessingException {

        return dataService.findInitiateProductionDataById(id);
    }

    @RequestMapping(value="/data/company/initiate", method = RequestMethod.GET)
    public List<DataInitialRequest> getInitiatedProductionData(
            @RequestParam(name = "companyName", required = false, defaultValue = "") String companyName) {
        String requestUsername = userService.getCurrentUserName();
        // make sure the user has access to this endpoint
        if (!dataService.validateUserForInitiateProductionData(requestUsername)) {
            throw SystemFatalException.raiseException("user " + requestUsername + " can't initiate the company data");
        }
        return dataService.findInitiateProductionData(companyName);
    }


}

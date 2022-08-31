package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.RFAppVersion;
import com.garyzhangscm.cwms.resources.model.RFConfiguration;
import com.garyzhangscm.cwms.resources.service.RFAppVersionService;
import com.garyzhangscm.cwms.resources.service.RFConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class RFConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(RFConfigurationController.class);

    @Autowired
    private RFConfigurationService rfConfigurationService;


    @RequestMapping(value = "/rf-configurations", method = RequestMethod.GET)
    public RFConfiguration getRFConfiguration(@RequestParam  Long warehouseId,
                                            @RequestParam(name = "rfCode", required = false, defaultValue = "") String rfCode) {
        return rfConfigurationService.findByRFCode(warehouseId, rfCode);
    }

    @BillableEndpoint
    @RequestMapping(value = "/rf-configurations", method = RequestMethod.POST)
    public RFConfiguration changeRFConfiguration(@RequestParam  Long warehouseId,
                                                 @RequestBody RFConfiguration rfConfiguration) {
        return rfConfigurationService.changeRFConfiguration(warehouseId, rfConfiguration);
    }



}

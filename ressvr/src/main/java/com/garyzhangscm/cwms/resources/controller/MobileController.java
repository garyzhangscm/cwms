package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.SiteInformation;
import com.garyzhangscm.cwms.resources.service.RFService;
import com.garyzhangscm.cwms.resources.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileController {

    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private RFService rfService;



    @RequestMapping(value = "/mobile", method = RequestMethod.GET)
    public SiteInformation getMobileInformation() {
        // return ApplicationInformation.getApplicationInformation();
        return siteInformationService.getDefaultSiteInformation();
    }


}
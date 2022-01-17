package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.MyApplicationRunner;
import com.garyzhangscm.cwms.resources.model.SiteInformation;
import com.garyzhangscm.cwms.resources.service.EMailService;
import com.garyzhangscm.cwms.resources.service.SiteInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EMailController {

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);

    @Autowired
    private EMailService eMailService;



    @RequestMapping(value = "/email/test", method = RequestMethod.GET)
    public String getMobileInformation() {
        // return ApplicationInformation.getApplicationInformation();
        // test email
        logger.debug("start to send test email");
        eMailService.sendMail("gzhang1999@gmail.com", "Test Email", "We are testing email");
        return "success";
    }




}

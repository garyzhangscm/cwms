package com.garyzhangscm.cwms.quickbook.controller;

import com.garyzhangscm.cwms.quickbook.ResponseBodyWrapper;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.garyzhangscm.cwms.quickbook.service.qbo.CDCService;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

@RestController
public class QuickBookOnlineDataController {

    private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineDataController.class);

    @Autowired
    private CDCService cdcService;


    @RequestMapping(value="/data/sync/{entityname}", method = RequestMethod.POST)
    public Integer requestToken(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @PathVariable String entityname,
            @RequestParam(name = "syncTransactionDays", required = false, defaultValue = "1") Integer syncTransactionDays
            ) {

        // get last day's transaction(invoice / purchase order) by default
        if (Objects.isNull(syncTransactionDays)) {
            syncTransactionDays = 1;
        }

         return cdcService.syncEntity(warehouseId, entityname, syncTransactionDays);
    }


}

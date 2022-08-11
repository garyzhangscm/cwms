package com.garyzhangscm.cwms.quickbook.controller;

import com.garyzhangscm.cwms.quickbook.ResponseBodyWrapper;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuickBookOnlineTokenController {

    private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineTokenController.class);

    @Autowired
    private QuickBookOnlineTokenService quickBookOnlineTokenService;


    @RequestMapping(value="/requestToken", method = RequestMethod.POST)
    public QuickBookOnlineToken requestToken(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam String authCode,
            @RequestParam String realmId) throws   OAuthException {


        return quickBookOnlineTokenService.requestToken(
                companyId, warehouseId, authCode, realmId);
    }

    @RequestMapping(value="/refreshToken", method = RequestMethod.POST)
    public QuickBookOnlineToken refreshToken(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam String realmId) throws OAuthException {

        return quickBookOnlineTokenService.requestRefreshToken(
                companyId, warehouseId, realmId);
    }

    @RequestMapping(value="/current-token", method = RequestMethod.GET)
    public QuickBookOnlineToken loadCurrentToken(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId) throws OAuthException {

        return quickBookOnlineTokenService.getByWarehouseId(warehouseId);
    }
}

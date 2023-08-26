package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.CustomRequestScopeAttr;
import com.garyzhangscm.cwms.outbound.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.ShortAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ShortAllocationProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(ShortAllocationProcessingJob.class);

    @Autowired
    AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

    @Autowired
    ShortAllocationService shortAllocationService;


    @Autowired
    @Qualifier("oauth2ClientContext")
    OAuth2ClientContext oauth2ClientContext;



    /**
    @Scheduled(fixedDelay = 15000)
    public void processShortAllocation() throws IOException {
        logger.debug("# start JOB to process short allocation data @ {}", LocalDateTime.now());
        if (!outboundConfigurationService.isShortAutoReallocationEnabled()){
            logger.debug("Short allocation auto allocating is not enabled, return");
            return;
        }
        setupOAuth2Context();

        List<ShortAllocation> shortAllocationList = shortAllocationService.findAll();

        shortAllocationList.forEach(shortAllocation ->
                shortAllocationService.processShortAllocation(shortAllocation)
        );
        logger.debug("# {} short allocation allocated @ {}",
                shortAllocationList.size() , LocalDateTime.now());
    }
     **/



    /**
     * Setup the OAuth2 token for the background job
     * OAuth2 token will be setup automatically in a web request context
     * but for a separate thread outside the web context, we will need to
     * setup the OAuth2 manually
     * @throws IOException
     */
    private void setupOAuth2Context() throws IOException {

        // Setup the request context so we can utilize the OAuth
        // as if we were in a web request context
        RequestContextHolder.setRequestAttributes(new CustomRequestScopeAttr());

        // Get token. We will use a default user to login and get
        // the OAuth2 token by the default user
        String token = authServiceRestemplateClient.getCurrentLoginUser().getToken();
        // logger.debug("# start to setup the oauth2 token for background job: {}", token);
        // Setup the access toke for the current thread
        // oauth2ClientContext is a scope = request bean that hold
        // the Oauth2 token
        oauth2ClientContext.setAccessToken(new DefaultOAuth2AccessToken(token));

    }



}

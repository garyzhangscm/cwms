package com.garyzhangscm.cwms.inbound;


import com.garyzhangscm.cwms.inbound.usercontext.UserContextInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class RestTemplateConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfiguration.class);
    /**
     * no token Rest Template, a rest tempalte without the user auth token
     * We will use this rest template to log in a default user for background job
     * that is not running inside a web request
     * @return
     */

    @Autowired
    RequestInterceptor requestInterceptor;

    // Rest Template when executing http request outside
    // a DispatcherServlet web request
    // it will fill in the pre-defined user's credential
    // to initiate the http request
    @Bean
    @Qualifier("autoLoginRestTemplate")
    public RestTemplate autoLoginRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(
                Arrays.asList(new ClientHttpRequestInterceptor[]{
                        new JsonMimeInterceptor(),
                        new UserContextInterceptor(),
                        requestInterceptor}));
        return restTemplate;
    }

}

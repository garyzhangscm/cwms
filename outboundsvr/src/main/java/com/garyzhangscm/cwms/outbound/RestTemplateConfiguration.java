package com.garyzhangscm.cwms.outbound;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.outbound.usercontext.UserContextInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(
                Arrays.asList(new ClientHttpRequestInterceptor[]{
                        new JsonMimeInterceptor(),
                        new UserContextInterceptor(),
                        requestInterceptor}));
        return restTemplate;
    }



}

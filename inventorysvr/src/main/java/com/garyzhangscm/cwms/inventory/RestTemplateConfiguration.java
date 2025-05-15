package com.garyzhangscm.cwms.inventory;


import com.garyzhangscm.cwms.inventory.usercontext.UserContextInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

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
    UserContextInterceptor userContextInterceptor;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(
                Arrays.asList(new ClientHttpRequestInterceptor[]{
                        new JsonMimeInterceptor(),
                        userContextInterceptor}));
        return restTemplate;
    }

}

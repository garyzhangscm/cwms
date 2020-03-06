package com.garyzhangscm.cwms.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@SpringBootApplication
@EnableScheduling
public class IntegrationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationServerApplication.class, args);
    }

    @Autowired
    RequestInterceptor requestInterceptor;

    @LoadBalanced
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(Collections.singletonList(requestInterceptor));
        return restTemplate;
    }

    // no token Rest Template, a rest tempalte without the user auth token
    @LoadBalanced
    @Bean
    @Qualifier("noTokenRestTemplate")
    public RestTemplate noTokenRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }


    @Bean
    public OAuth2RestTemplate oauth2RestTemplate(OAuth2ClientContext oauth2ClientContext,
                                                 OAuth2ProtectedResourceDetails details) {
        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(details, oauth2ClientContext);
        // Interceptor to add accept = JSON to the http header
        oAuth2RestTemplate.setInterceptors(Collections.singletonList(new RequestInterceptor()));
        return oAuth2RestTemplate;
    }
}

package com.garyzhangscm.cwms.resources;

import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.web.client.RestTemplate;

public class CustomRestTemplateCustomizer implements RestTemplateCustomizer {
    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(new CustomClientHttpRequestInterceptor());
    }
}
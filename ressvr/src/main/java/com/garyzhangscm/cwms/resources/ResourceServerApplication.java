package com.garyzhangscm.cwms.resources;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.util.Collections;


@SpringBootApplication
// Refresh configuration when config server is updated
// only customized configure can be re-fetched from the server
// Database configuration won't be updated in the client even the
// configuration server is updated with new DB information
@RefreshScope
@EnableEurekaClient
@EnableResourceServer
@EnableOAuth2Client
public class ResourceServerApplication {


	public static void main(String[] args) {
		SpringApplication.run(ResourceServerApplication.class, args);
	}


	// Rest Template with Robbin
	@LoadBalanced
	@Bean
	public RestTemplate getRestTemplate(){
		return new RestTemplate();
	}

	@Bean
	public OAuth2RestTemplate oauth2RestTemplate(OAuth2ClientContext oauth2ClientContext,
												 OAuth2ProtectedResourceDetails details) {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(details, oauth2ClientContext);
		// Interceptor to add accept = JSON to the http header
		oAuth2RestTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
		return oAuth2RestTemplate;
	}
}

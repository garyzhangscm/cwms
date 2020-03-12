package com.garyzhangscm.cwms.inventory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@SpringBootApplication
// Refresh configuration when config server is updated
// only customized configure can be re-fetched from the server
// Database configuration won't be updated in the client even the
// configuration server is updated with new DB information
@RefreshScope
@EnableResourceServer
@EnableCaching
// @EnableCircuitBreaker
public class InventoryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServerApplication.class, args);
	}

	@Bean
	@ConfigurationProperties("security.oauth2.client")
	public ClientCredentialsResourceDetails oauth2ClientCredentialsResourceDetails() {
		return new ClientCredentialsResourceDetails();
	}

	@Bean
	@LoadBalanced
	public OAuth2RestOperations  oauth2RestTemplate(RestTemplateCustomizer customizer,
													ClientCredentialsResourceDetails oauth2ClientCredentialsResourceDetails,
													@Qualifier("oauth2ClientContext") OAuth2ClientContext oauth2ClientContext) {
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(oauth2ClientCredentialsResourceDetails, oauth2ClientContext);
		restTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
		customizer.customize(restTemplate);
		return restTemplate;
	}


	/****
	@Bean
	public OAuth2RestTemplate oauth2RestTemplate(OAuth2ClientContext oauth2ClientContext,
												 OAuth2ProtectedResourceDetails details) {
		OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(details, oauth2ClientContext);
		// Interceptor to add accept = JSON to the http header
		oAuth2RestTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
		return oAuth2RestTemplate;
	}
	 ****/
}

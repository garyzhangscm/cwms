package com.garyzhangscm.cwms.outbound;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.outbound.usercontext.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@SpringBootApplication
// Refresh configuration when config server is updated
// only customized configure can be re-fetched from the server
// Database configuration won't be updated in the client even the
// configuration server is updated with new DB information
@RefreshScope
@EnableResourceServer
@EnableCaching
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class OutboundServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OutboundServerApplication.class, args);
	}


	@Bean
	@Qualifier("noTokenRestTemplate")
	public RestTemplate noTokenRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate;
	}

	@Bean
	@ConfigurationProperties("security.oauth2.client")
	public ClientCredentialsResourceDetails oauth2ClientCredentialsResourceDetails() {
		return new ClientCredentialsResourceDetails();
	}

	@Bean
	public CustomRestTemplateCustomizer customRestTemplateCustomizer() {
		return new CustomRestTemplateCustomizer();
	}

	@Bean
	@LoadBalanced
	public OAuth2RestOperations oauth2RestTemplate(CustomRestTemplateCustomizer customizer,
												   ClientCredentialsResourceDetails oauth2ClientCredentialsResourceDetails,
												   @Qualifier("oauth2ClientContext") OAuth2ClientContext oauth2ClientContext) {
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(oauth2ClientCredentialsResourceDetails, oauth2ClientContext);

		List<ClientHttpRequestInterceptor> interceptorList = new ArrayList<>();

		// restTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
		restTemplate.setInterceptors(
				Arrays.asList(new ClientHttpRequestInterceptor[]{
						new JsonMimeInterceptor(),  new UserContextInterceptor()}));

		customizer.customize(restTemplate);
		return restTemplate;
	}

	// To support scheduled job with OAuth2
	/***
	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}
	***/

	// setup the configuration for redis cache

	@Bean
	@Primary
	public ObjectMapper getObjMapper(){
		// JavaTimeModule timeModule = new JavaTimeModule();
		// timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
		// timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

		return new ObjectMapper()
				.registerModule(new ParameterNamesModule())
				.registerModule(new Jdk8Module())
				.registerModule(new JavaTimeModule())
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	/****
	 *
	 * @return
	 */
	@Bean
	public RedisCacheConfiguration cacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(2))
				.disableCachingNullValues()
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
	}
	/**
	 * Class to implement the JPA audit. Auto generate the
	 * created by and last modified by username for any
	 * database entity
	 * @return
	 */
	@Bean
	public AuditorAware<String> auditorAware(){
		return new AuditorAwareImpl();
	}
}

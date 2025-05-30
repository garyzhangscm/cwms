package com.garyzhangscm.cwms.layout;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@SpringBootApplication
// Refresh configuration when config server is updated
// only customized configure can be re-fetched from the server
// Database configuration won't be updated in the client even the
// configuration server is updated with new DB information
@RefreshScope
@EnableCaching
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class LayoutServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LayoutServerApplication.class, args);
	}
/**
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

		// restTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
		restTemplate.setInterceptors(
				Arrays.asList(new ClientHttpRequestInterceptor[]{
						new JsonMimeInterceptor(),  new UserContextInterceptor()}));
		customizer.customize(restTemplate);
		return restTemplate;
	}

 **/
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


	// setup the configuration for redis cache

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

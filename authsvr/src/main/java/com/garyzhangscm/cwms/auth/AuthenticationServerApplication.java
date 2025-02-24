package com.garyzhangscm.cwms.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.auth.usercontext.UserContextInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
@EnableResourceServer
@EnableAuthorizationServer
@RefreshScope
@EnableJpaAuditing
public class AuthenticationServerApplication {

	@RequestMapping(value = { "/user" }, produces = "application/json")
	public Map<String, Object> user(OAuth2Authentication user) {
		System.out.println("inside /user: " + (user == null));
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("user", user.getUserAuthentication().getPrincipal());
		userInfo.put("authorities", AuthorityUtils.authorityListToSet(user.getUserAuthentication().getAuthorities()));
		return userInfo;
	}

	// Rest Template with Robbin
	@LoadBalanced
	@Bean
	public RestTemplate getRestTemplate(){
		RestTemplate template = new RestTemplate();

		List interceptors = template.getInterceptors();
		if (interceptors== null){
			template. setInterceptors(
					Collections.singletonList(
							new UserContextInterceptor()));
		}
		else{
			interceptors.add(new UserContextInterceptor());
			template.setInterceptors(interceptors);
		}
		return template;

	}

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationServerApplication.class, args);
	}


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

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		OAuth2AuthorizedClientProvider authorizedClientProvider =
				OAuth2AuthorizedClientProviderBuilder.builder()
						.authorizationCode()
						.refreshToken()
						.build();

		DefaultOAuth2AuthorizedClientManager authorizedClientManager =
				new DefaultOAuth2AuthorizedClientManager(
						clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		// Assuming the `username` and `password` are supplied as `HttpServletRequest` parameters,
		// map the `HttpServletRequest` parameters to `OAuth2AuthorizationContext.getAttributes()`
		authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());

		return authorizedClientManager;
	}
}

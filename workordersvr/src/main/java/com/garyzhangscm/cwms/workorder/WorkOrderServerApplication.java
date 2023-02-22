package com.garyzhangscm.cwms.workorder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.workorder.usercontext.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
// Refresh configuration when config server is updated
// only customized configure can be re-fetched from the server
// Database configuration won't be updated in the client even the
// configuration server is updated with new DB information
@RefreshScope
@EnableResourceServer
@EnableCaching
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class WorkOrderServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkOrderServerApplication.class, args);
    }


    /**
     * Client credential bean for OAuth2 based Rest Template
     * @return
     */
    @Bean
    @ConfigurationProperties("security.oauth2.client")
    public ClientCredentialsResourceDetails oauth2ClientCredentialsResourceDetails() {
        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public CustomRestTemplateCustomizer customRestTemplateCustomizer() {
        return new CustomRestTemplateCustomizer();
    }



    /**
     * Rest Template with OAuth2 enabled
     * @param customizer
     * @param oauth2ClientCredentialsResourceDetails
     * @param oauth2ClientContext
     * @return
     */
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
    /**
     @Bean
     public RedisCacheConfiguration cacheConfiguration() {
         return RedisCacheConfiguration.defaultCacheConfig()
         .entryTtl(Duration.ofMinutes(2))
         .disableCachingNullValues()
         .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
     }
     **/
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

package com.garyzhangscm.cwms.adminserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
public class AdminserverApplication {

    @Autowired
    RequestInterceptor requestInterceptor;

    public static void main(String[] args) {
        SpringApplication.run(AdminserverApplication.class, args);
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
        restTemplate.setInterceptors(Collections.singletonList(new JsonMimeInterceptor()));
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


    // no token Rest Template, a rest tempalte without the user auth token
    @LoadBalanced
    @Bean
    @Qualifier("noTokenRestTemplate")
    public RestTemplate noTokenRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }



    @LoadBalanced
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(Collections.singletonList(requestInterceptor));
        return restTemplate;
    }

    // setup the configuration for redis cache

    /****
     *
     * @return
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
/*
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(){
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        // RedisSerializer<Object> serializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
        configuration = configuration.serializeValuesWith(
                RedisSerializationContext.SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer)).entryTtl(Duration.ofMinutes(5));
        return configuration;
    }
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        //初始化一个RedisCacheWriter
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);

        //设置CacheManager的值序列化方式为 fastJsonRedisSerializer,但其实RedisCacheConfiguration默认使用StringRedisSerializer序列化key，
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        RedisSerializationContext.SerializationPair<Object> pair =
                RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer);
        RedisCacheConfiguration defaultCacheConfig=RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);
    }*/

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

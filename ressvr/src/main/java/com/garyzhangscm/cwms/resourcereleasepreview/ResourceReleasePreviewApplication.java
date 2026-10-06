package com.garyzhangscm.cwms.resourcereleasepreview;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.resources.AuditorAwareImpl;
import com.garyzhangscm.cwms.resources.clients.KafkaReceiver;
import com.garyzhangscm.cwms.resources.controller.UserPasswordResetController;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

/** Full dependency startup check without scheduled jobs or Kafka consumers. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages="com.garyzhangscm.cwms.resources",excludeFilters={
 @ComponentScan.Filter(type=FilterType.ANNOTATION,classes=RestController.class),
 @ComponentScan.Filter(type=FilterType.REGEX,pattern={"com\\.garyzhangscm\\.cwms\\.resources\\.ResourceServerApplication", "com\\.garyzhangscm\\.cwms\\.resources\\.MyApplicationRunner"}),
 @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes=KafkaReceiver.class)
})
@EntityScan("com.garyzhangscm.cwms.resources.model")
@EnableJpaRepositories("com.garyzhangscm.cwms.resources.repository")
@EnableJpaAuditing(auditorAwareRef="auditorAware")
@EnableCaching
@Import(UserPasswordResetController.class)
public class ResourceReleasePreviewApplication {
 public static void main(String[] args){SpringApplication.run(ResourceReleasePreviewApplication.class,args);}
 @RestController public static class Health {
  @GetMapping("/healthz") public Map<String,Object> health(){return Map.of("result",0,"data","ready");}
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
	/***
	@Bean
	public JavaMailSender getJavaMailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com");
		mailSender.setPort(587);

		mailSender.setUsername("");
		mailSender.setPassword("");

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.debug", "true");

		return mailSender;
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

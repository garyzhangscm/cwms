package com.garyzhangscm.cwms.APIGateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication

public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Autowired
	private AccessControlFilter accessControlFilter;
	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(p -> p
						.path("/api/resource/**")
						.filters(f -> f.filter(accessControlFilter).stripPrefix(2))
						// .filters(f -> f.addRequestHeader("service", "resource").stripPrefix(2))
						.uri("http://resourceservice:8280"))
				.route(p -> p
						.path("/api/layout/**")
						.filters(f -> f.filter(accessControlFilter).stripPrefix(2))
						//.filters(f -> f.addRequestHeader("service", "layout").stripPrefix(2))
						.uri("http://layoutservice:8180"))
				.build();
	}
/**
	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(p -> p
						.path("/api/layout/")
						.uri("http://layoutservice:8180"))
				.route(p -> p
						.path("/api/auth/")
						.uri("http://authserver:8901"))
				.route(p -> p
						.path("/api/resource/")
						.uri("http://resourceservice:8280"))
				.route(p -> p
						.path("/api/common/")
						.uri("http://commonservice:8380"))
				.route(p -> p
						.path("/api/inventory/")
						.uri("http://inventoryservice:8480"))
				.route(p -> p
						.path("/api/inbound/")
						.uri("http://inboundservice:8580"))
				.route(p -> p
						.path("/api/outbound/")
						.uri("http://outboundservice:8680"))
				.route(p -> p
						.path("/api/workorder/")
						.uri("http://workorderservice:8780"))
				.route(p -> p
						.path("/api/integration/")
						.uri("http://integrationservice:8880"))
				.route(p -> p
						.path("/api/admin/")
						.uri("http://adminservice:8078"))
				.route(p -> p
						.path("/api/dblink/")
						.uri("http://dblinkserver:11808"))
				.route(p -> p
						.path("/api/quickbook/")
						.uri("http://quickbook:11818"))
				.build();
	}
	*/

}

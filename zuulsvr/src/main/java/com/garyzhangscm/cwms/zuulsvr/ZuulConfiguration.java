package com.garyzhangscm.cwms.zuulsvr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulController;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/***
 * Start from springboot 2.5, spring doesn't support ZUUL any more. As a temporary
 * solution, we will need to add this class
 * We will need to consider to switch to Spring Cloud Gateway and Ribbon
 * Replacing: Zuul (Api gateway) by Spring Cloud Gateway and Ribbon(Load balancer) by Spring Cloud Loadbalancer.
 * You can go through this blog to get instructions:
 * https://spring.io/blog/2018/12/12/spring-cloud-greenwich-rc1-available-now#spring-cloud-netflix-projects-entering-maintenance-mode
 */
@Configuration
public class ZuulConfiguration {
	/**
	 * The path returned by ErrorController.getErrorPath() with Spring Boot < 2.5
	 * (and no longer available on Spring Boot >= 2.5).
	 */
	private static final String ERROR_PATH = "/error";
	private static final String METHOD = "lookupHandler";

	/**
	 * Constructs a new bean post-processor for Zuul.
	 *
	 * @param routeLocator    the route locator.
	 * @param zuulController  the Zuul controller.
	 * @param errorController the error controller.
	 * @return the new bean post-processor.
	 */
	@Bean
	public ZuulPostProcessor zuulPostProcessor(@Autowired RouteLocator routeLocator,
											   @Autowired ZuulController zuulController,
											   @Autowired(required = false) ErrorController errorController) {
		return new ZuulPostProcessor(routeLocator, zuulController, errorController);
	}

	private enum LookupHandlerCallbackFilter implements CallbackFilter {
		INSTANCE;

		@Override
		public int accept(Method method) {
			if (METHOD.equals(method.getName())) {
				return 0;
			}
			return 1;
		}
	}

	private enum LookupHandlerMethodInterceptor implements MethodInterceptor {
		INSTANCE;

		@Override
		public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			if (ERROR_PATH.equals(args[0])) {
				// by entering this branch we avoid the ZuulHandlerMapping.lookupHandler method to trigger the
				// NoSuchMethodError
				return null;
			}
			return methodProxy.invokeSuper(target, args);
		}
	}

	private static final class ZuulPostProcessor implements BeanPostProcessor {

		private final RouteLocator routeLocator;
		private final ZuulController zuulController;
		private final boolean hasErrorController;

		ZuulPostProcessor(RouteLocator routeLocator, ZuulController zuulController, ErrorController errorController) {
			this.routeLocator = routeLocator;
			this.zuulController = zuulController;
			this.hasErrorController = (errorController != null);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (hasErrorController && (bean instanceof ZuulHandlerMapping)) {
				Enhancer enhancer = new Enhancer();
				enhancer.setSuperclass(ZuulHandlerMapping.class);
				enhancer.setCallbackFilter(LookupHandlerCallbackFilter.INSTANCE); // only for lookupHandler
				enhancer.setCallbacks(new Callback[] {LookupHandlerMethodInterceptor.INSTANCE, NoOp.INSTANCE});
				Constructor<?> ctor = ZuulHandlerMapping.class.getConstructors()[0];
				return enhancer.create(ctor.getParameterTypes(), new Object[] {routeLocator, zuulController});
			}
			return bean;
		}
	}
}
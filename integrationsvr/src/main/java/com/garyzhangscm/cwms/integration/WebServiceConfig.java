package com.garyzhangscm.cwms.integration;


import com.garyzhangscm.cwms.integration.controller.QBWebConnectorEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.ws.wsdl.wsdl11.Wsdl11Definition;

import javax.servlet.Servlet;

@EnableWs
@Configuration
public class WebServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebServiceConfig.class);

    @Bean
    public ServletRegistrationBean<Servlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet =
                new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);

        return new ServletRegistrationBean<>(servlet,
                "/quickbook/ws/*");
    }

    @Bean(name = "qbWebConnector")
    public Wsdl11Definition defaultWsdl11Definition() {

        logger.debug("=====   get default WSDL  ======");
        SimpleWsdl11Definition wsdl11Definition =
                new SimpleWsdl11Definition();
        wsdl11Definition
                .setWsdl(new ClassPathResource("/wsdl/qbWebConnector.wsdl"));

        logger.debug(wsdl11Definition.toString());
        return wsdl11Definition;
    }

}

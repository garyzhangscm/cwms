package com.garyzhangscm.cwms.integration.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
public class IntegrationServerConfiguration extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // 设置资源服务器的 id
        resources.resourceId("wms-layout");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
        .authorizeRequests()
                .antMatchers("/app").permitAll()
                .antMatchers("/actuator").permitAll()
                .antMatchers("/quickbook/ws/**").permitAll()
                // .antMatchers("/integration-data/**").permitAll()
                .antMatchers("/probe/**").permitAll()
                .antMatchers("/tiktok/**").permitAll()
          .anyRequest()
          .authenticated();
    }

}

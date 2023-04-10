package com.garyzhangscm.cwms.layout.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
public class LayoutServerConfiguration extends ResourceServerConfigurerAdapter {
// public class LayoutServerConfiguration{
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
                .antMatchers(HttpMethod.GET, "/companies").permitAll()
           .antMatchers("/companies/validate").permitAll()
                .antMatchers("//companies/*/is-enabled").permitAll()
          .antMatchers("/warehouses/accessible/**").permitAll()
                .antMatchers("/probe/**").permitAll()
                .antMatchers("/warehouses/*/company-id").permitAll() 

          .anyRequest()
          .authenticated();
    }

}

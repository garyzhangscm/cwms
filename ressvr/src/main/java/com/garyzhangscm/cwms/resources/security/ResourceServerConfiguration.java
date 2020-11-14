package com.garyzhangscm.cwms.resources.security;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // 设置资源服务器的 id
        resources.resourceId("wms-resource");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
        .authorizeRequests()
          .antMatchers("/app").permitAll()
          .antMatchers("/login").permitAll()
          .antMatchers("/actuator").permitAll()
          .antMatchers("/assets/**").permitAll()
                .antMatchers("/site-information/default").permitAll()
                .antMatchers("/mobile").permitAll()
          .anyRequest()
          .authenticated();
    }

}

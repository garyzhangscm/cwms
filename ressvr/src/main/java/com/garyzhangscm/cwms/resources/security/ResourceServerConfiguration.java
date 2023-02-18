package com.garyzhangscm.cwms.resources.security;

import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {


    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // 设置资源服务器的 id
        resources.resourceId("wms-resource");
    }

    public void configure(HttpSecurity http) throws Exception{
        http
        .authorizeRequests()
          .antMatchers("/app").permitAll()
          .antMatchers("/login").permitAll()
          .antMatchers("/actuator").permitAll()
          .antMatchers("/assets/**").permitAll()
           .antMatchers("/validate/**").permitAll()
                .antMatchers("/probe/**").permitAll()
          .antMatchers("/site-information/default").permitAll()
          .antMatchers("/mobile").permitAll()
          .antMatchers("/report-histories/download/**").permitAll()
          .antMatchers(HttpMethod.GET, "/reports/templates/upload/**").permitAll()
          .antMatchers(HttpMethod.GET, "/reports/templates").permitAll()
                .antMatchers(HttpMethod.GET, "/rf-apk-files/**").permitAll()
                .antMatchers(HttpMethod.GET, "/rf-apk-files").permitAll()
                .antMatchers(HttpMethod.GET, "/email/test").permitAll()
                .antMatchers(HttpMethod.GET, "/rf-app-version/latest-version").permitAll()
                .antMatchers(HttpMethod.GET, "/printing-request/pending").permitAll()
                .antMatchers(HttpMethod.GET, "/users/is-system-admin").permitAll()
          .anyRequest()
          .authenticated();
    }

}

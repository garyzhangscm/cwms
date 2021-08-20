package com.garyzhangscm.cwms.auth.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {


    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
        .authorizeRequests()
                .antMatchers("/app").permitAll()
          .antMatchers("/login/**").permitAll()
          .antMatchers("/oauth/**").permitAll()
          .antMatchers("/actuator").permitAll()
                .antMatchers("/probe/**").permitAll()
          .anyRequest()
          .authenticated();
    }

}

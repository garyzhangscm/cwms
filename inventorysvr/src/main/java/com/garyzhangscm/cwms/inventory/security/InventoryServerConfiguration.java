package com.garyzhangscm.cwms.inventory.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class InventoryServerConfiguration extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // 设置资源服务器的 id
        resources.resourceId("wms-inventory");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
        .authorizeRequests()
                .antMatchers("/app").permitAll()
          .antMatchers("/actuator").permitAll()
                .antMatchers("/upload/**").permitAll()
                .antMatchers("/images/**").permitAll()
                .antMatchers("/probe/**").permitAll()
          .anyRequest()
          .authenticated();
    }
}

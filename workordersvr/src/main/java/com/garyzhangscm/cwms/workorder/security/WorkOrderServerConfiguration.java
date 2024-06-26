package com.garyzhangscm.cwms.workorder.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class WorkOrderServerConfiguration extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // 设置资源服务器的 id
        resources.resourceId("wms-workorder");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
                .authorizeRequests()
                .antMatchers("/app").permitAll()
                .antMatchers("/actuator").permitAll()
                .antMatchers("/probe/**").permitAll()
                .antMatchers("/production-line-monitor-transactions").permitAll()
                .antMatchers("/production-line-monitors/heart-beat").permitAll()
                .antMatchers("/qc-samples/images/**").permitAll()
                .anyRequest()
                .authenticated();
    }
}

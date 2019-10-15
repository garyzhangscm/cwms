package com.garyzhangscm.cwms.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;


@Configuration
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // Only for un-encrypted password
    @Bean
    public static NoOpPasswordEncoder passwordEncoder() {
        return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
    }
   @Override
    @Bean
    public UserDetailsService userDetailsServiceBean() throws Exception {
        return super.userDetailsServiceBean();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .withUser("rwu").password("rwu").roles("USER")
                .and()
                .withUser("gzhang").password("gzhang").roles("USER", "ADMIN");
    }

    /***
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.formLogin().loginPage("/login.html").loginProcessingUrl("/login").permitAll().and().authorizeRequests()
                .antMatchers("/login", "/oauth/authorize", "/oauth/confirm_access","/oauth/token","/auth/oauth/token").permitAll().anyRequest()
                .authenticated().and().csrf().disable();

    }
    ***/
}

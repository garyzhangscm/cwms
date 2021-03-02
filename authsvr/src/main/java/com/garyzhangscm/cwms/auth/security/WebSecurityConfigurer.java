package com.garyzhangscm.cwms.auth.security;

import com.garyzhangscm.cwms.auth.controller.LoginController;
import com.garyzhangscm.cwms.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfigurer.class);

    @Autowired
    UserRepository userRepository;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // Only for un-encrypted password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();

    }

    @Override
    @Bean
    public UserDetailsService userDetailsServiceBean() throws Exception {
        return super.userDetailsServiceBean();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(
                new UserDetailsService() {
                    @Override
                    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                        logger.debug(">>> Start to loadUserByUsername {}", username);
                        Long companyId = -1L;
                        String actualUsername = username;
                        if (username.contains("#")) {
                            String[] usernameTokens = username.split("#");
                            if (usernameTokens.length == 2) {
                                companyId = Long.parseLong(usernameTokens[0]);
                                actualUsername = usernameTokens[1];
                            }
                        }
                        logger.debug(">>> Will find user by company ID {}, username {}", companyId, actualUsername);
                        UserDetails userDetails = userRepository.findByCompanyIdAndUsername(companyId,  actualUsername);
                        System.out.println("uscder details: \n" +
                                " >> username: " + userDetails.getUsername() +
                                " >> password: " + userDetails.getPassword());
                        return userDetails;
                    }
                }).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception{
        http
                .authorizeRequests()
                .antMatchers("/site-information/default").permitAll()
                .anyRequest()
                .authenticated();
    }

}

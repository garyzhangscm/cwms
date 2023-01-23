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

import java.util.Objects;


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
        // return new BCryptPasswordEncoder();
    }

    @Override
    @Bean
    public UserDetailsService userDetailsServiceBean() throws Exception {
        return super.userDetailsServiceBean();
    }


    /**
     * setup the OAUTH2 server to validate the user's login against the user_auth
     * table. The username passed in will be COMPANY_ID#USERNAME
     * @param auth
     * @throws Exception
     */
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
                        // if we can't find the user from a specific company, there's still a good chance that the
                        // user is an system admin, which doesn't belong to any company. Then the company ID will be -1
                        if (Objects.isNull(userDetails)) {

                            logger.debug(">>> Can't find user by company ID {}, username {}, " +
                                    " will check if the user is a system admin(company code = -1",
                                    companyId, actualUsername);
                            userDetails = userRepository.findByCompanyIdAndUsername(-1l, actualUsername);
                        }
                        logger.debug("user details: \n" +
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
                .antMatchers("/probe/**").permitAll()
                .antMatchers("/users/company-access-validation").permitAll()
                .anyRequest()
                .authenticated();
    }

}

package com.garyzhangscm.cwms.auth.security;

import com.garyzhangscm.cwms.auth.repository.UserRepository;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfigurer.class);

    @Autowired
    UserRepository userRepository;


    @Autowired
    private UserService userService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/app", "/login/**", "/oauth/**",
                                        "/actuator", "/probe/**", "/users/company-access-validation",
                                        "/users/username-by-token").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No sessions
                )
                .authenticationProvider(authenticationProvider()); // Custom authentication provider
                // .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // return new BCryptPasswordEncoder(); // Password encoding
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return new CWMSAuthenticationManager();
    }



    /**
     * setup the OAUTH2 server to validate the user's login against the user_auth
     * table. The username passed in will be COMPANY_ID#USERNAME
     * @param auth
     * @throws Exception
     */
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



}

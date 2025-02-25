package com.garyzhangscm.cwms.auth.security;

import com.garyzhangscm.cwms.auth.model.CWMSAuthentication;
import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

/**
 * Customized authentication manager to authenticate user
 * by company id, user name and password
 */
public class CWMSAuthenticationManager implements AuthenticationManager {

    @Autowired
    private UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof CWMSAuthentication) {
            CWMSAuthentication cwmsAuthentication = (CWMSAuthentication) authentication;
            User user = userService.findByUsername(cwmsAuthentication.getCompanyId(), cwmsAuthentication.getUsername());
            if (Objects.isNull(user)) {
                // throw new UsernameNotFoundException("user " + cwmsAuthentication.getUsername() + " not exists");

                authentication.setAuthenticated(false);
            }
            else if (!passwordEncoder.matches(cwmsAuthentication.getPassword(), user.getPassword())) {
                // throw new BadCredentialsException("username and password doesn't match");
                authentication.setAuthenticated(false);
            }
            else {
                authentication.setAuthenticated(true);
            }
        }
        else {
            // throw new AuthenticationServiceException("not able to authenticate the user " + authentication.getPrincipal());
            authentication.setAuthenticated(false);

        }
        return authentication;
    }
}

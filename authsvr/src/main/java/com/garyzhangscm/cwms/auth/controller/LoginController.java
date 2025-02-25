/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.auth.model.CWMSAuthentication;
import com.garyzhangscm.cwms.auth.model.*;
import com.garyzhangscm.cwms.auth.service.JwtService;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

@RestController
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // Custmoized JSON mapper
    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;



    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    UserService userService;


    /***
    @RequestMapping(method =  RequestMethod.POST)
    public LoginResponseWrapper login(@RequestParam("username") String username,
                                      @RequestParam("password") String password) {
        OAuth2Token oAuth2Token = oAuth2Service.getOAuth2Token(username, password);
        oAuth2Token.setUser(userService.findByUsername(username));
        return LoginResponseWrapper.of("ok", OAuth2TokenWrapper.of(oAuth2Token));
    }
     **/
    @RequestMapping(value = "/login", method =  RequestMethod.POST)
    public LoginResponseWrapper login(@RequestBody User user)  {

        logger.debug("start to login with user {}", user);
        Authentication authentication = authenticationManager.authenticate(
                new CWMSAuthentication(user.getCompanyId(),
                        user.getUsername(), user.getPassword())
        );

        logger.debug("user is authenticated? {}", authentication.isAuthenticated());
        if (authentication.isAuthenticated()) {
            String jwtToken = jwtService.generateToken(user.getUsername());
            logger.debug("will return JWT token: " + jwtToken);
            return LoginResponseWrapper.of(0, "", OAuth2TokenWrapper.of(oAuth2Token));
        } else {
            throw new UsernameNotFoundException("Username and password doesn't match!");
        }

        /**
         * OBSOLETED: The old way is to use simple token. we will switch to JWT
        try {
            logger.debug("start to login with user {}", user);
            OAuth2Token oAuth2Token = oAuth2Service.getOAuth2Token(user.getCompanyId(), user.getUsername(), user.getPassword());

            oAuth2Token.setUser(userService.findByUsername(user.getCompanyId(), user.getUsername()));
            userService.recordLoginEvent(user.getCompanyId(), user.getLoginWarehouseId(),
                    user.getUsername(), oAuth2Token.getAccess_token());
            return LoginResponseWrapper.of(0, "", OAuth2TokenWrapper.of(oAuth2Token));
        }
        catch(HttpClientErrorException.Unauthorized unauthorizedException) {
            logger.debug("login error / Unauthorized: {}", unauthorizedException.getMessage());
            return LoginResponseWrapper.of(ExceptionCode.LOGIN_ERROR.getCode(), "Username and Password not match", null);
        }
        catch(HttpClientErrorException.BadRequest badRequest) {
            logger.debug("login error / BadRequest: \n{}\n{}", badRequest.getMessage(), badRequest.getResponseBodyAsString());
            return LoginResponseWrapper.of(ExceptionCode.LOGIN_ERROR.getCode(), getErrorMessage(badRequest), null);
        }
         **/
    }

    private String getErrorMessage(HttpClientErrorException.BadRequest badRequest) {
        try {

            LoginError loginError =
                    objectMapper.readValue(badRequest.getResponseBodyAsString(), LoginError.class);

            return loginError.getError() + ": " + loginError.getErrorDescription();

        } catch (JsonMappingException e) {
            return "Login error";
        } catch (JsonProcessingException e) {
            return "Login error";
        }
    }

}

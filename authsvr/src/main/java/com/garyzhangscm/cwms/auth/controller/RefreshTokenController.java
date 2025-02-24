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


import com.garyzhangscm.cwms.auth.model.LoginResponseWrapper;
import com.garyzhangscm.cwms.auth.model.OAuth2Token;
import com.garyzhangscm.cwms.auth.model.OAuth2TokenWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping(value = "/refresh")
public class RefreshTokenController {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenController.class);

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @GetMapping("/")
    public LoginResponseWrapper refreshToken(Authentication authentication,
                        HttpServletRequest servletRequest,
                        HttpServletResponse servletResponse) {

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("cwms-client")
                .principal(authentication)
                .attributes(attrs -> {
                    attrs.put(HttpServletRequest.class.getName(), servletRequest);
                    attrs.put(HttpServletResponse.class.getName(), servletResponse);
                })
                .build();
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);

        OAuth2Token oAuth2Token = new OAuth2Token();
        oAuth2Token.setAccess_token(authorizedClient.getAccessToken().getTokenValue());
        oAuth2Token.setRefresh_token(authorizedClient.getRefreshToken().getTokenValue());
        oAuth2Token.setToken_type("");


        return LoginResponseWrapper.of(0, "", OAuth2TokenWrapper.of(oAuth2Token));
    }

}

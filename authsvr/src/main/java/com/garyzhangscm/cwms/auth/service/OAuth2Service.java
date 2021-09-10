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

package com.garyzhangscm.cwms.auth.service;

import com.garyzhangscm.cwms.auth.exception.ExceptionCode;
import com.garyzhangscm.cwms.auth.model.LoginResponseWrapper;
import com.garyzhangscm.cwms.auth.model.OAuth2Token;
import com.garyzhangscm.cwms.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuth2Service {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);
    @Autowired
    RestTemplate restTemplate;

    public OAuth2Token getOAuth2Token(Long  companyId, String username, String password) {
        String oauth2URL
       //         = "http://AUTHSERVICE/oauth/token";
                  = "http://authserver:8901/oauth/token";
        logger.debug("Start to get auth information from {}", oauth2URL);

        HttpHeaders headers = new HttpHeaders();
        String clientID = "cwms";
        String secret = "gz-cwms";
        String auth = clientID + ":" + secret;
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));


        MultiValueMap<String, String> formData= new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "password");
        formData.add("scope", "webclient");
        formData.add("username", companyId + "#" + username);
        System.out.println("Start to verify by password: " + password);
        formData.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(formData, headers);

        ResponseEntity<OAuth2Token> responseObj = restTemplate.postForEntity(oauth2URL, request, OAuth2Token.class);
        return responseObj.getBody();
    }
}

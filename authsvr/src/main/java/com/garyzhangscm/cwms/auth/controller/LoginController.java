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
import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.service.OAuth2Service;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/login")
public class LoginController {

    @Autowired
    OAuth2Service oAuth2Service;

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @RequestMapping(method =  RequestMethod.POST)
    public LoginResponseWrapper login(@RequestParam("username") String username,
                                      @RequestParam("password") String password) {
        OAuth2Token oAuth2Token = oAuth2Service.getOAuth2Token(username, password);
        oAuth2Token.setUser(userService.findByUsername(username));
        return LoginResponseWrapper.of("ok", OAuth2TokenWrapper.of(oAuth2Token));
    }

    @RequestMapping("/mock")
    public User generateMocaUser(@RequestParam("username") String username,
                                 @RequestParam("password") String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(username + "@gmail.com" );
        user.setEnabled(true);
        user.setFirstname(username);
        user.setLastname(username);
        user.setLocked(false);

        return userService.save(user);
    }


    @RequestMapping("/allusers")
    public List<User> getAllUsers() {
          return userService.findAll();
    }

}

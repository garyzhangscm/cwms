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

import com.garyzhangscm.cwms.auth.ResponseBodyWrapper;
import com.garyzhangscm.cwms.auth.model.BillableEndpoint;
import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    UserService userService;

    @RequestMapping(method =  RequestMethod.GET)
    public ResponseBodyWrapper findUsers(@RequestParam Long companyId,
                                         @RequestParam(name="usernames", defaultValue = "", required = false) String usernames)   {

        return new ResponseBodyWrapper(0, "",
                userService.findAll(companyId, usernames));
    }


    @BillableEndpoint
    @RequestMapping(method =  RequestMethod.POST)
    public ResponseBodyWrapper changeUser(@RequestBody User user) {

        return new ResponseBodyWrapper(0, "",userService.saveOrUpdate(user));
    }


    @RequestMapping(value = "/company-access-validation", method =  RequestMethod.GET)
    public ResponseBodyWrapper validateCompanyAccess(@RequestParam Long companyId,
                                         @RequestParam String token) {
        return new ResponseBodyWrapper(0, "",userService.validateCompanyAccess(companyId, token));
    }


    @RequestMapping(value = "/username-by-token", method =  RequestMethod.GET)
    public ResponseBodyWrapper getUserNameByToken(@RequestParam Long companyId,
                                         @RequestParam String token) {
        User user = userService.findByToken(companyId, token);
        if (Objects.nonNull(user)) {
            return  new ResponseBodyWrapper(0, "",user.getUsername());
        }
        else {
            return new ResponseBodyWrapper(0, "","");
        }
    }

}

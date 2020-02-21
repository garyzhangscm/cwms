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

import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(method =  RequestMethod.GET)
    public List<User> findUsers(@RequestParam(name="usernames", defaultValue = "", required = false) String usernames) {
        return userService.findAll(usernames);
    }


    @RequestMapping(method =  RequestMethod.POST)
    public User changeUser(@RequestBody User user) {
        return userService.saveOrUpdate(user);
    }

}

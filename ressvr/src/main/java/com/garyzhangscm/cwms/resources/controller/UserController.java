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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(value="/users", method = RequestMethod.GET)
    public List<User> findAllUsers(@RequestParam(name="username", required = false, defaultValue = "") String username,
                                   @RequestParam(name="firstname", required = false, defaultValue = "") String firstname,
                                   @RequestParam(name="lastname", required = false, defaultValue = "") String lastname,
                                   @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled,
                                   @RequestParam(name="locked", required = false, defaultValue = "") Boolean locked) {
        return userService.findAll(username, firstname, lastname, enabled, locked);
    }

    @RequestMapping(value="/users/{id}", method = RequestMethod.GET)
    public User findUser(@PathVariable Long id) {
        return userService.findById(id);
    }



}

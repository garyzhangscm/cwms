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

package com.garyzhangscm.cwms.auth;

import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("MyApplicationRunner...");
        List<User> users = userService.findAll();
        for(User user : users) {
            System.out.println("user: "+ user.getUsername());
            if (!user.getPassword().startsWith("{") ||
                 user.getPassword().indexOf("}") < 0) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                User newUser = userService.save(user);
                System.out.println("==> encoding password for user: " + newUser.getUsername() + newUser.getPassword());
            }
        }
    }
}

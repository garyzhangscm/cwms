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

import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return findAll(null);
    }
    public List<User> findAll(String usernames) {
        return userRepository.findAll(
            (Root<User> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (!StringUtils.isBlank(usernames)) {
                    CriteriaBuilder.In<String> inUsernames = criteriaBuilder.in(root.get("username"));
                    for(String username : usernames.split(",")) {
                        inUsernames.value(username);
                    }
                    predicates.add(criteriaBuilder.and(inUsernames));
                }

                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

    }
    public User findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User save(User user) {
        encryptPassword(user);
        return userRepository.save(user);
    }
    @Transactional
    public User saveOrUpdate(User user) {
        // In case the user's password is passed in, encrypt and save
        // otherwise, we will save other information without
        // changing the password
        if (!StringUtils.isBlank(user.getPassword())) {
            encryptPassword(user);
        }
        if (Objects.isNull(user.getId()) &&
                !Objects.isNull(findByUsername(user.getUsername()))) {
            user.setId(findByUsername(user.getUsername()).getId());
            if (StringUtils.isBlank(user.getPassword())) {
                user.setPassword(findByUsername(user.getUsername()).getPassword());
            }
        }

        return userRepository.save(user);
    }
    public void encryptPassword(User user) {
        System.out.println("user: "+ user.getUsername());
        if (!user.getPassword().startsWith("{") ||
                user.getPassword().indexOf("}") < 0) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            logger.debug("User {}'s password was encrypt to {}",
                    user.getUsername(), user.getPassword());
        }
    }

}

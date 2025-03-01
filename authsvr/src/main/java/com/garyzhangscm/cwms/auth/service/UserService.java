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

import com.garyzhangscm.cwms.auth.clients.KafkaSender;
import com.garyzhangscm.cwms.auth.exception.SystemFatalException;
import com.garyzhangscm.cwms.auth.model.JWTTokenWrapper;
import com.garyzhangscm.cwms.auth.model.User;
import com.garyzhangscm.cwms.auth.model.UserLoginEvent;
import com.garyzhangscm.cwms.auth.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Value("${auth.jwt.refresh_token.expire_time_in_minutes:30}")
    public int jwtRefreshTokenExpireTimeInMinutes;
    @Value("${auth.jwt.token.expire_time_in_minutes:30}")
    public int jwtTokenExpireTimeInMinutes;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private JwtService jwtService;

    @Autowired
    HttpServletRequest request;

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


    public List<User> findAll() {
        return findAll(null, null);
    }
    public List<User> findAll(Long companyId, String usernames) {
        return userRepository.findAll(
            (Root<User> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                if (Objects.nonNull(companyId)) {

                    // company id may be a actual company id, or -1 for global user/system admin
                    CriteriaBuilder.In<Long> inCompanyIds = criteriaBuilder.in(root.get("companyId"));
                    inCompanyIds.value(companyId);
                    inCompanyIds.value(-1l);
                    predicates.add(criteriaBuilder.and(inCompanyIds));

                }

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

    public User findByUsername(Long companyId, String username){
        User user =  userRepository.findByCompanyIdAndUsername(companyId, username);
        if (Objects.isNull(user)) {
            user = userRepository.findByCompanyIdAndUsername(-1l, username);
        }
        return user;
    }

    public User findByToken(Long companyId, String token){
        User user =  userRepository.findByCompanyIdAndCurrentToken(companyId, token);
        if (Objects.isNull(user)) {
            user = userRepository.findByCompanyIdAndCurrentToken(-1l, token);
        }
        return user;
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
                !Objects.isNull(findByUsername(user.getCompanyId(), user.getUsername()))) {
            user.setId(findByUsername(user.getCompanyId(), user.getUsername()).getId());
            if (StringUtils.isBlank(user.getPassword())) {
                user.setPassword(findByUsername(user.getCompanyId(), user.getUsername()).getPassword());
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


    public String getCurrentUserName() {
        if (Objects.nonNull(request) && Strings.isNotBlank(request.getHeader("username"))) {

            return request.getHeader("username");
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }


    /**
     * Record the event that a certain user login to some company / warehouse
     * @param companyId
     * @param loginWarehouseId
     * @param username
     */
    public void recordLoginEvent(Long companyId, Long loginWarehouseId,
                                 String username, String token) {

        logger.debug("Start to record login event for user: {}, token: {}, company id: {}, warehouse id: {}",
                username, token, companyId, loginWarehouseId);
        UserLoginEvent userLoginEvent = new UserLoginEvent(
                companyId, loginWarehouseId,
                username, token
        );

        kafkaSender.send(userLoginEvent);

        // save the token
        User user = findByUsername(companyId, username);
        if (Objects.nonNull(user)) {
            user.setCurrentToken(token);
            saveOrUpdate(user);
        }


    }

    public Boolean validateCompanyAccess(Long companyId, String token) {
        return Objects.nonNull(findByToken(companyId, token));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsername(1l, username);

    }


    public JWTTokenWrapper generateJWTToken(Long companyId, String username) {
        User user = findByUsername(companyId, username);
        if(Objects.isNull(user)) {
            throw SystemFatalException.raiseException("can't find user " + username);
        }

        String jwtToken = jwtService.generateToken(user.getCompanyId(), user.getUsername());
        String refreshToken = UUID.randomUUID().toString();

        logger.debug("will return JWT token: " + jwtToken + " with refresh token: " + refreshToken);

        user.setCurrentToken(jwtToken);
        user.setCurrentTokenExpireTime(jwtService.extractExpiration(jwtToken).toInstant().atZone(ZoneOffset.UTC));
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpireTime(ZonedDateTime.now().plusMinutes(jwtRefreshTokenExpireTimeInMinutes));

        saveOrUpdate(user);

        JWTTokenWrapper jwtTokenWrapper = new JWTTokenWrapper(user);
        jwtTokenWrapper.setRefreshIn(jwtTokenExpireTimeInMinutes / 2);
        return jwtTokenWrapper;
    }
}

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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.GenericException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService  implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MenuGroupService menuGroupService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    private FileService fileService;


    @Value("${fileupload.test-data.users:users}")
    String testDataFile;
    @Value("${site.company.singleCompany}")
    private Boolean singleCompanySite;
    @Value("${site.company.defaultCompanyCode}")
    private String defaultCompanyCode;



    public User findById(Long id) {
        return findById(id, true);
    }
    public User findById(Long id, boolean loadAttribute) {
        User user =  userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("user not found by id: " + id));
        if (loadAttribute) {
            loadAttribute(user);
        }
        return user;
    }

    public User findByUsername(String username) {
        return findByUsername(username, true);
    }
    public User findByUsername(String username, boolean loadAttribute) {
        User user =  userRepository.findByUsername(username);
        if (user != null && loadAttribute) {
            loadAttribute(user);
        }
        return user;
    }

    public List<User> findAll() {

        List<User> users =   userRepository.findAll();

        loadAttribute(users);
        return users;
    }

    public List<User> findAll(String username,
                              String rolename,
                              String workingTeamName,
                              String firstname,
                              String lastname,
                              Boolean enabled,
                              Boolean locked) {

        List<User> users =  userRepository.findAll(
                (Root<User> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (!StringUtils.isBlank(username)) {
                        predicates.add(criteriaBuilder.equal(root.get("username"), username));
                    }
                    if (!StringUtils.isBlank(rolename)) {
                        Join<User,Role> joinRole = root.join("roles",JoinType.LEFT);
                        predicates.add(criteriaBuilder.equal(joinRole.get("name"), rolename));
                    }
                    if (!StringUtils.isBlank(workingTeamName)) {
                        Join<User,WorkingTeam> joinWorkingTeam = root.join("workingTeams",JoinType.LEFT);
                        predicates.add(criteriaBuilder.equal(joinWorkingTeam.get("name"), workingTeamName));
                    }

                    if (!StringUtils.isBlank(firstname)) {
                        predicates.add(criteriaBuilder.equal(root.get("firstname"), firstname));
                    }

                    if (!StringUtils.isBlank(lastname)) {
                        predicates.add(criteriaBuilder.equal(root.get("lastname"), lastname));
                    }

                    if (enabled != null) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                    }

                    if (locked != null) {
                        predicates.add(criteriaBuilder.equal(root.get("locked"), locked));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        loadAttribute(users);
        return users;


    }

    public void loadAttribute(List<User> users) {
        String usernames = users.stream().map(User::getUsername).collect(Collectors.joining(","));
        List<UserAuth> userAuths = authServiceRestemplateClient.getUserAuthByUsernames(usernames);
        Map<String, UserAuth> userAuthMap = new HashMap<>();
        userAuths.stream().forEach(userAuth -> userAuthMap.put(userAuth.getUsername(), userAuth));

        users.stream().forEach(user -> setUserAuthInformation(user, userAuthMap.get(user.getUsername())));

    }
    public void loadAttribute(User user) {
        // load the auth information for each user
        UserAuth userAuth = authServiceRestemplateClient.getUserAuthByUsername(user.getUsername());

        setUserAuthInformation(user, userAuth);
    }
    private void setUserAuthInformation(User user, UserAuth userAuth) {
        user.setEmail(userAuth.getEmail());
        user.setPassword(userAuth.getPassword());
        user.setLocked(userAuth.isLocked());
        user.setEnabled(userAuth.isEnabled());
    }
    public User save(User user) {

        return userRepository.save(user);
    }

    public User saveOrUpdate(User user) {
        if (Objects.isNull(user.getId()) &&
                !Objects.isNull(findByUsername(user.getUsername()))) {
            user.setId(findByUsername(user.getUsername()).getId());
        }
        return save(user);
    }
    public SiteInformation getSiteInformaiton(String username) {
        return getSiteInformaiton(findByUsername(username));

    }
    public SiteInformation getSiteInformaiton(User user) {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setUser(user);

        List<MenuGroup> menuGroups = menuGroupService.getAccessibleMenus(user);
        siteInformation.setMenuGroups(menuGroups);

        logger.debug("Objects.isNull(singleCompanySite) ? : {}", Objects.isNull(singleCompanySite)  );
        logger.debug("singleCompanySite ? : {}", singleCompanySite );

        siteInformation.setSingleCompanySite(
                Objects.isNull(singleCompanySite) ? false : singleCompanySite
        );
        if (siteInformation.getSingleCompanySite() == true) {
            // If this is a single company site, then get the only one
            // company and return it as the default company
            siteInformation.setDefaultCompanyCode(defaultCompanyCode);

        }
        return siteInformation;
    }

    public List<User> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("username").
                addColumn("password").
                addColumn("email").
                addColumn("firstname").
                addColumn("lastname").
                addColumn("admin").
                addColumn("enabled").
                addColumn("locked").
                build().withHeader();

        return fileService.loadData(inputStream, schema, User.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<User> users = loadData(inputStream);
            users.stream().forEach(user -> saveWithCredential(user));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private void saveWithCredential(User user) {
            saveOrUpdate(user);

            // save the password , email, enabled, locked in the auth server
            UserAuth userAuth = user.getUserAuth();
            authServiceRestemplateClient.changeUserAuth(userAuth);
    }


    public String getCurrentUserName() {
        logger.debug("SecurityContextHolder.getContext().getAuthentication().getName(): {}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            return "";
        }
        else {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
    }

    public User getCurrentUser() {
        return findByUsername(getCurrentUserName());
    }

    @Transactional
    public User addUser(User user) {
        // Save the user without role first
        // since we are adding new user, the user doesn't have an ID yet.
        // user / role are many to many relationship so we need both
        // user and role having the ID so that the relationship can be
        // persist in the user_role table
        // So we will save the user without role,
        // then attach all the roles to the saved user and persist the
        // relationship again
        List<Role> roles = user.getRoles();
        user.setRoles(new ArrayList<>());
        User newUser = save(user);

        // save the password , email, enabled, locked in the auth server
        UserAuth userAuth = user.getUserAuth();

        authServiceRestemplateClient.changeUserAuth(userAuth);

        // refresh role information from database
        roles.forEach(role -> {
            Role newRole = roleService.findById(role.getId());
            newUser.assignRole(newRole);
        });
        return saveOrUpdate(newUser);
    }

    @Transactional
    public User processRoles(Long userId, String assignedRoleIds, String deassignedRoleIds) {

        User user = findById(userId);

        if (!StringUtils.isBlank(assignedRoleIds)) {
            Arrays.stream(assignedRoleIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(roleId -> {
                        Role role = roleService.findById(roleId);
                        user.assignRole(role);
                    });
        }
        if (!StringUtils.isBlank(deassignedRoleIds)) {
            Arrays.stream(deassignedRoleIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(roleId -> {
                        Role role = roleService.findById(roleId);
                        user.deassignRole(role);
                    });
        }
        return saveOrUpdate(user);

    }

    /**
     * Check if the current login user can access the url
     * @param url
     * @return true if the current login user can access the url
     */
    public boolean validateURLAccess(String url) {

        User user = getCurrentUser();
        logger.debug("Check if user {} has access to url: {}", user.getUsername(), url);
        return validateURLAccess(user, url);

    }
    public boolean validateURLAccess(User user, String url) {

        // Get the menu that associated with the URL

        try {
            Menu menu = menuService.getMenuByUrl(url);

            // loop through each role of the user and see
            // if the role has access to the menu
            // we will short circuit after we find the first
            // role that has access to the menu
            user.getRoles().stream()
                    .filter(role -> role.getMenus().contains(menu))
                    .findFirst()
                    .orElseThrow(() -> UserOperationException.raiseException("The user doesn't have access to the url: " + url));

            return true;

        }
        catch (ResourceNotFoundException ex) {
            logger.debug("ResourceNotFoundException while validateURLAccess: {}", ex);
            // ResourceNotFoundException when we can't find the menu by URL
            // so probably this is not a menu. At this moment we allow all
            // web service call as long as the user log in
            return true;
        }
        catch (GenericException ex) {
            logger.debug("GenericException while validateURLAccess: {}", ex);
            return false;

        }
    }

    public List<User> disableUsers(String userIds) {

        return Arrays.stream(userIds.split(","))
                .mapToLong(Long::parseLong)
                .mapToObj(userId -> disableUser(userId, false))
                .collect(Collectors.toList());
    }
    public List<User> enableUsers(String userIds) {

        return Arrays.stream(userIds.split(","))
                .mapToLong(Long::parseLong)
                .mapToObj(userId -> disableUser(userId, true))
                .collect(Collectors.toList());
    }

    public User disableUser(Long userId, boolean enabled) {
        User user = findById(userId);
        // Disable the user @Auth service
        user.setEnabled(enabled);
        UserAuth userAuth = user.getUserAuth();
        authServiceRestemplateClient.changeUserAuth(userAuth);
        logger.debug("Disable User: {}  / {}", user.getId(), user.getUsername());
        return user;
    }

    public List<User> lockUsers(String userIds) {

        return Arrays.stream(userIds.split(","))
                .mapToLong(Long::parseLong)
                .mapToObj(userId -> lockUser(userId,true))
                .collect(Collectors.toList());
    }
    public List<User> unlockUsers(String userIds) {

        return Arrays.stream(userIds.split(","))
                .mapToLong(Long::parseLong)
                .mapToObj(userId -> lockUser(userId,false))
                .collect(Collectors.toList());
    }

    public User lockUser(Long userId, boolean locked) {
        User user = findById(userId);
        // lock the user @Auth service
        user.setLocked(locked);
        UserAuth userAuth = user.getUserAuth();
        authServiceRestemplateClient.changeUserAuth(userAuth);
        logger.debug("Lock User: {}  / {}", user.getId(), user.getUsername());
        return saveOrUpdate(user);
    }

}

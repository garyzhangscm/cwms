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
import com.garyzhangscm.cwms.resources.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.resources.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.GenericException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    private WebClientConfigurationService webClientConfigurationService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    private SystemConfigurationService systemConfigurationService;
    @Autowired
    private FileService fileService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private WorkTaskService workTaskService;

    @Autowired
    HttpServletRequest request;

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

    public User findByUsername(Long companyId, String username) {
        return findByUsername(companyId, username, true);
    }
    public User findByUsername(Long companyId, String username, boolean loadAttribute) {
        User user =  userRepository.findByCompanyIdAndUsername(companyId, username);
        if (Objects.isNull(user)) {
            // in case the user is a system admin, the company id will be -1 for
            // this user, to indicate that the user doesn't belong to any company
            user = userRepository.findByCompanyIdAndUsername(-1l, username);
        }
        logger.debug("we find user by company id {}, username {}? user != null: {}",
                companyId, username, user != null );
        if (user != null && loadAttribute) {
            loadAttribute(user);
        }
        return user;
    }

    public List<User> findAll(Long companyId) {

        List<User> users =   userRepository.findByCompanyId(companyId);

        loadAttribute(companyId, users);
        return users;
    }

    public List<User> findAll(Long companyId,
                              String username,
                              String rolename,
                              String workingTeamName,
                              String firstname,
                              String lastname,
                              Boolean enabled,
                              Boolean locked,
                              String token,
                              Long assignableToWorkTaskId) {

        List<User> users =  userRepository.findAll(
                (Root<User> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    // company id may be a actual company id, or -1 for global user/system admin
                    CriteriaBuilder.In<Long> inCompanyIds = criteriaBuilder.in(root.get("companyId"));
                    inCompanyIds.value(companyId);
                    inCompanyIds.value(-1l);
                    predicates.add(criteriaBuilder.and(inCompanyIds));

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
                    if (Strings.isNotBlank(token)) {
                        predicates.add(criteriaBuilder.equal(root.get("lastLoginToken"), token));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "companyId", "username")
        );

        if (Objects.nonNull(assignableToWorkTaskId)) {
            // only return the user that can be assigned to the work task
            users = users.stream().filter(
                    user -> workTaskService.validateWorkTaskAgainstUser(
                            assignableToWorkTaskId, user.getId()
                    )
            ).collect(Collectors.toList());

        }

        loadAttribute(companyId, users);
        return users;


    }

    public void loadAttribute(Long companyId, List<User> users) {
        String usernames = users.stream().map(User::getUsername).collect(Collectors.joining(","));
        List<UserAuth> userAuths = authServiceRestemplateClient.getUserAuthByUsernames(companyId, usernames);
        Map<String, UserAuth> userAuthMap = new HashMap<>();
        userAuths.stream().forEach(userAuth -> userAuthMap.put(userAuth.getUsername(), userAuth));
        users.stream().forEach(user -> setUserAuthInformation(user, userAuthMap.get(user.getUsername())));

    }
    public void loadAttribute(User user) {
        // load the auth information for each user
        UserAuth userAuth = authServiceRestemplateClient.getUserAuthByUsername(
                user.getCompanyId(), user.getUsername());

        setUserAuthInformation(user, userAuth);
    }
    private void setUserAuthInformation(User user, UserAuth userAuth) {
        if (Objects.isNull(userAuth)) {
            logger.debug("!!!Error!!!, user {} doesn't have auth information",
                    user.getUsername());
            return;
        }
        // user.setEmail(userAuth.getEmail());
        user.setPassword(userAuth.getPassword());
        user.setLocked(userAuth.isLocked());
        user.setEnabled(userAuth.isEnabled());
    }
    public User save(User user) {

        return userRepository.save(user);
    }

    public User saveOrUpdate(User user) {
        if (Objects.isNull(user.getId()) &&
                !Objects.isNull(findByUsername(user.getCompanyId(), user.getUsername()))) {
            user.setId(findByUsername(user.getCompanyId(), user.getUsername()).getId());
        }
        return save(user);
    }
    public SiteInformation getSiteInformaiton(Long companyId, Long warehouseId, String username) {
        User user = findByUsername(companyId, username);
        logger.debug("we find user? {} by username {}, companeId: {}",
                user != null, username, companyId);
        if (Objects.isNull(user)) {
            throw UserOperationException.raiseException("Can't find user by username " + username);
        }
        return getSiteInformaiton(companyId, warehouseId, user);

    }

    public SiteInformation getMobileSiteInformation(Long companyId, String username) {

        return getSiteInformaiton(companyId, null, findByUsername(companyId, username), true);
    }
    public SiteInformation getMobileSiteInformation(User user) {
        return getSiteInformaiton(null, null, user, true);

    }
    public SiteInformation getSiteInformaiton(Long companyId, Long warehouseId, User user) {
        return getSiteInformaiton(companyId, warehouseId, user, false);
    }
    public SiteInformation getSiteInformaiton(Long companyId, Long warehouseId, User user, boolean mobile) {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setUser(user);

        MenuType menuType = mobile ?  MenuType.MOBILE : MenuType.WEB;
        List<MenuGroup> menuGroups = menuGroupService.getAccessibleMenus(companyId, user, menuType);
        logger.debug("We find {} menus for the user {}",
                menuGroups == null ? 0 : menuGroups.size(), user.getUsername());
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

        // get server side printing flag from the warehouse configuration instead

        Boolean serversidePrintingFlag  = null;
        if(Objects.nonNull(warehouseId)) {

            WarehouseConfiguration warehouseConfiguration = layoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);
            if (Objects.nonNull(warehouseConfiguration) && Objects.nonNull(warehouseConfiguration.getPrintingStrategy())) {
                serversidePrintingFlag = warehouseConfiguration.getPrintingStrategy().equals(PrintingStrategy.SERVER_PRINTER) ||
                        warehouseConfiguration.getPrintingStrategy().equals(PrintingStrategy.LOCAL_PRINTER_SERVER_DATA);
            }
        }

        SystemConfiguration systemConfiguration
                = systemConfigurationService.findByCompanyAndWarehouse(
                companyId, warehouseId
        );
        if (Objects.isNull(systemConfiguration)) {
            logger.debug("Configuration is not setup for system of company / warehouse {} / {}",
                    companyId, warehouseId);
            logger.debug("will default to side side printing and NOT allow data initial");
            siteInformation.setServerSidePrinting(
                    Objects.isNull(serversidePrintingFlag) ? true : serversidePrintingFlag
            );
            siteInformation.setAllowDataInitialFlag(false);
        }
        else {
            logger.debug("Configuration is setup for system of company / warehouse {} / {}",
                    companyId, warehouseId);

            siteInformation.setServerSidePrinting(

                    Objects.isNull(serversidePrintingFlag) ? systemConfiguration.getServerSidePrinting() : serversidePrintingFlag

            );
            siteInformation.setAllowDataInitialFlag(
                    systemConfiguration.getAllowDataInitialFlag()
            );

        }


        WebClientConfiguration webClientConfiguration =
                mobile ? new WebClientConfiguration(): getWebClientConfiguration(companyId, warehouseId, user);
        siteInformation.setWebClientConfiguration(webClientConfiguration);
        return siteInformation;
    }

    private WebClientConfiguration getWebClientConfiguration(Long companyId, Long warehouseId, User user) {

        return webClientConfigurationService.getWebClientConfiguration(companyId, warehouseId, user);
    }

    public List<User> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("companyId").
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

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

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

        if (Objects.isNull(request)) {
            return "ANONYMOUS";
        }
        return request.getHeader("username");
        /**
        logger.debug("SecurityContextHolder.getContext().getAuthentication().getName(): {}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            return "";
        }
        else {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
         **/
    }

    public User getCurrentUser(Long companyId) {
        return findByUsername(companyId, getCurrentUserName());
    }

    @Transactional
    public User addUser(User user) {
        // make sure the user doesn't exists yet
        if (Objects.nonNull(findByUsername(user.getCompanyId(), user.getUsername()))) {
            throw UserOperationException.raiseException("User name " + user.getUsername() + " already exists!");
        }

        // make sure the username have the right format
        // 1. clear all the empty space in the begin or end of the username
        user.setUsername(user.getUsername().trim());

        return changeUser(user);

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
    public boolean validateURLAccess(Long companyId, String url) {

        User user = getCurrentUser(companyId);
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
                    .filter(role -> role.canAccessMenu(menu.getId()))
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

    public String validateNewUsername(Long companyId, Long warehouseId, String username) {

        User user =
                findByUsername(companyId, username, false);

        return Objects.isNull(user) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    public User changeUser(User user) {

        // we can't change the admin user or global user from the
        // client. Once it is created, we will need to change from the
        // database side
        if (Boolean.TRUE.equals(user.getSystemAdmin())) {

            throw UserOperationException.raiseException("Can't remove system admin user");
        }
        if (user.getCompanyId() < 0){
            throw UserOperationException.raiseException("Can't remove user that created for the system");
        }

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
        User newUser = saveOrUpdate(user);

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

    public Boolean validateSystemAdminUser(String username) {
        // by default, system admin will not belong to any company. So
        // its company id will be -1
        User user = findByUsername(-1l, username);
        return Objects.nonNull(user);
    }

    public void changePassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPassword(newPassword);
        user.setChangePasswordAtNextLogon(false);


        UserAuth userAuth = user.getUserAuth();

        authServiceRestemplateClient.changeUserAuth(userAuth);

        saveOrUpdate(user);


    }

    public User recordLoginEvent(UserLoginEvent userLoginEvent) {
        User user = findByUsername(
                userLoginEvent.getLastLoginCompanyId(),
                userLoginEvent.getUsername(), false);
        user.setLastLoginCompanyId(userLoginEvent.getLastLoginCompanyId());
        user.setLastLoginWarehouseId(userLoginEvent.getLastLoginWarehouseId());
        user.setLastLoginToken(userLoginEvent.getToken());
        return  saveOrUpdate(user);
    }

    public User findUserByToken(String username, String token) {
        return userRepository.findByUsernameAndLastLoginToken(username, token);

    }

    public User addTempUser(Long companyId, String username, String firstname, String lastname) {
        // make user the user is not exists yet
        if (Objects.nonNull(findByUsername(companyId, username, false))) {
            throw UserOperationException.raiseException("The user " + username + " already exists");
        }
        User user = new User();
        user.setCompanyId(companyId);
        user.setUsername(username.trim());
        user.setPassword(username);
        // create a new user but lock it so it won't be able to login anywhere
        user.setEnabled(true);
        user.setLocked(true);
        user.setAdmin(false);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        return addUser(user);
    }

    /**
     * Copy the existing user into a new user with the specific username. The new user's password
     * will be the same as the new username and the user will be forced to change password during
     * next logon
     * @param id
     * @param username
     * @return
     */
    public User copyUser(Long id, String username,
                         String firstname,
                         String lastname) {

        User existingUser = findById(id);
        // make user the user is not exists yet
        if (Objects.nonNull(findByUsername(existingUser.getCompanyId(), username, false))) {
            throw UserOperationException.raiseException("The user " + username + " already exists");
        }

        User newUser = existingUser.copy(username, firstname, lastname);
        newUser.setCreatedTime(ZonedDateTime.now(ZoneOffset.UTC));
        newUser.setLastModifiedTime(ZonedDateTime.now(ZoneOffset.UTC));

        return addUser(newUser);


    }
    public User changeEmail(Long companyId, String username, String email) {
        User user = findByUsername(companyId, username, false);

        user.setEmail(email);
        logger.debug("User {}'s email is changed to {}", username, email);
        return saveOrUpdate(user);
    }

    /**
     * Get current user's permission by web page. Permission in web page normally means if the
     * user has access to certain button or link
     * @param companyId
     * @param warehouseId
     * @param webPageUrl
     * @return
     */
    public List<UserPermission> getUserPermissionByWebPage(Long companyId, Long warehouseId, String webPageUrl) {
        User user = findByUsername(companyId, getCurrentUserName(), false);
        return getUserPermissionByWebPage(user, webPageUrl);
    }

    public List<UserPermission> getUserPermissionByWebPage(User user, String webPageUrl) {

        return getUserPermissionByWebPage(
                user,
                menuService.getMenuByUrl(webPageUrl)
        );

    }

    /**
     * Get the user's permission on the web page of the menu
     * @param user
     * @param menu
     * @return
     */
    public List<UserPermission> getUserPermissionByWebPage(User user, Menu menu) {
        // get all the permission that belong to the menu

        List<Permission> permissions = permissionService.findAll(menu, null, null);
        // loop through each permission and see if current user has access
        // to the permission
        // the user is blocked from the permission only if it is explicitly
        // disallowed
        return permissions.stream().map(
                permission -> {
                    UserPermission userPermission = new UserPermission();
                    userPermission.setUsername(user.getUsername());
                    userPermission.setPermission(permission);
                    userPermission.setAllowAccess(isPermissionAccessAllowed(user, permission));
                    return userPermission;
                }
        ).collect(Collectors.toList());
    }

    private Boolean isPermissionAccessAllowed(User user, Permission permission) {

        logger.debug("Start to check if the user {} can access permission {} - {} / {}",
                user.getUsername(),
                permission.getMenu().getName(),
                permission.getMenu().getLink(),
                permission.getName());
        // admin and system admin has access to everything
        if (user.getAdmin() || user.getSystemAdmin()) {
            logger.debug("Current user is admin / system admin, allow any permission");
            return true;
        }
        // if we have at least one role of the user allowed to access the permission
        // then the user is allowed to access the permission
        // else if there's no role allowed to access the permission but there's role
        // that not allowed to access the permission explicitly, then the user doesn't
        // allow to access the permission
        int accessAllowedRoleCount = 0;
        int accessDisallowedRoleCount = 0;

        for (Role role : user.getRoles()) {
            RolePermission rolePermission = rolePermissionService.findByRoleAndPermission(
                    role, permission
            );

            if (Objects.nonNull(rolePermission)) {
                logger.debug("Role {} / Permission {} - {} / {} is defined as {}",
                        role.getName(),
                        permission.getMenu().getName(),
                        permission.getMenu().getLink(),
                        permission.getName(),
                        rolePermission.getAllowAccess());
                if (Boolean.FALSE.equals(rolePermission.getAllowAccess())) {
                    accessDisallowedRoleCount++;
                }
                else {
                    accessAllowedRoleCount++;
                }
            }
            else {
                logger.debug("Role {} / Permission {} - {} / {} is not defined ",
                        role.getName(),
                        permission.getMenu().getName(),
                        permission.getMenu().getLink(),
                        permission.getName());
            }
        }

        if (accessDisallowedRoleCount == 0) {
            // no role has explicitly disallowed the permission, by default
            // the permission is allowed
            logger.debug("There's no role that block the access, we will allow the access by default");
            return true;
        }
        // we have roles that explicitly disallow the permission,
        // we will still allow the permission only if there's other roles that
        // explicitly allow the permission
        logger.debug("There's roles that block the access, We will still allow access only if there's role" +
                " that allow the permission? {}", (accessAllowedRoleCount > 0));
        return accessAllowedRoleCount > 0;
    }
}

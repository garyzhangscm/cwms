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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(value="/users", method = RequestMethod.GET)
    public List<User> findAllUsers(@RequestParam Long companyId,
                                   @RequestParam(name="username", required = false, defaultValue = "") String username,
                                   @RequestParam(name="rolename", required = false, defaultValue = "") String rolename,
                                   @RequestParam(name="workingTeamName", required = false, defaultValue = "") String workingTeamName,
                                   @RequestParam(name="firstname", required = false, defaultValue = "") String firstname,
                                   @RequestParam(name="lastname", required = false, defaultValue = "") String lastname,
                                   @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled,
                                   @RequestParam(name="locked", required = false, defaultValue = "") Boolean locked,
                                   @RequestParam(name="token", required = false, defaultValue = "") String token) {
        return userService.findAll(companyId, username, rolename, workingTeamName,  firstname, lastname, enabled, locked,
                token);
    }

    /**
     * Find by username and token. We assume the token should be unique so we can identify
     * the identical user by token
     * @param username
     * @param token
     * @return
     */
    @RequestMapping(value="/users-by-token", method = RequestMethod.GET)
    public User findUserByToken(@RequestParam String username,
                                @RequestParam String token) {
        return userService.findUserByToken( username, token);
    }

    @BillableEndpoint
    @RequestMapping(value="/users/validate-url", method = RequestMethod.POST)
    public Boolean validateURLAccess(@RequestParam Long companyId,
                                     @RequestBody String url) {
        return userService.validateURLAccess(companyId, url);
    }

    @RequestMapping(value="/users/{id}", method = RequestMethod.GET)
    public User findUser(@PathVariable Long id) {
        return userService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/users", method = RequestMethod.PUT)
    public User addUser(@RequestBody User user) {
        return userService.addUser(user);
    }


    @BillableEndpoint
    @RequestMapping(value="/users/{id}", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public User changeUser(@PathVariable Long id,
                           @RequestBody User user) {
        return userService.changeUser(user);
    }

    @BillableEndpoint
    @RequestMapping(value="/users/{id}/copy", method = RequestMethod.POST)
    public User copyUser(@PathVariable Long id,
                         @RequestParam String username,
                         @RequestParam String firstname,
                         @RequestParam String lastname) {
        // copy the existing user to create a new user with the new name
        // the password will be default to the username
        // and the user will be required to change password when next logon
        return userService.copyUser(id, username, firstname, lastname);
    }



    @BillableEndpoint
    @RequestMapping(value="/users/{id}/roles", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public ResponseBodyWrapper processRoles(@PathVariable Long id,
                                            @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedRoleIds,
                                            @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedRoleIds) {

        userService.processRoles(id, assignedRoleIds, deassignedRoleIds);
        return ResponseBodyWrapper.success("success");
    }


    @BillableEndpoint
    @RequestMapping(value="/users/{id}/password", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> changePassword(@PathVariable Long id,
                                            @RequestParam String newPassword) {

        userService.changePassword(id, newPassword);
        return ResponseBodyWrapper.success("success");
    }

    @BillableEndpoint
    @RequestMapping(value="/users/disable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public List<User> disableUsers(@RequestParam String userIds) {

        return userService.disableUsers(userIds);
    }
    @BillableEndpoint
    @RequestMapping(value="/users/enable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public List<User> enableUsers(@RequestParam String userIds) {

        return userService.enableUsers(userIds);
    }


    @BillableEndpoint
    @RequestMapping(value="/users/lock", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public List<User> lockUsers(@RequestParam String userIds) {

        return userService.lockUsers(userIds);
    }
    @BillableEndpoint
    @RequestMapping(value="/users/unlock", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public List<User> unlockUsers(@RequestParam String userIds) {

        return userService.unlockUsers(userIds);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/users/validate-new-username")
    public ResponseBodyWrapper<String> validateNewUsername(@RequestParam Long companyId,
                                                           @RequestParam Long warehouseId,
                                                           @RequestParam String username)  {

        return ResponseBodyWrapper.success(userService.validateNewUsername(companyId, warehouseId, username));
    }

    @RequestMapping(method=RequestMethod.GET, value="/users/is-system-admin")
    public Boolean validateSystemAdminUser(@RequestParam String username)  {

        return  userService.validateSystemAdminUser(username);
    }

    @BillableEndpoint
    @RequestMapping(value="/user/new-temp-user", method = RequestMethod.POST)
    public User addTempUser(@RequestParam Long companyId,
                            @RequestParam String username,
                            @RequestParam String firstname,
                            @RequestParam String lastname) {
        return userService.addTempUser(companyId, username, firstname, lastname);
    }

    @BillableEndpoint
    @RequestMapping(value="/user/change-email", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_UserByName", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_User", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_User", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_User", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_isSystemAdmin", allEntries = true),
            }
    )
    public User changeEmail(@RequestParam Long companyId,
                            @RequestParam String username,
                            @RequestParam String email) {
        return userService.changeEmail(companyId, username, email);
    }



}

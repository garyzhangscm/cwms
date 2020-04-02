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
import com.garyzhangscm.cwms.resources.model.MenuGroup;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.RoleService;
import com.garyzhangscm.cwms.resources.service.UserService;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RoleController {

    @Autowired
    RoleService roleService;
    @Autowired
    MenuGroupService menuGroupService;

    @RequestMapping(value="/roles", method = RequestMethod.GET)
    public List<Role> findAllRoles(@RequestParam(name="name", required = false, defaultValue = "") String name,
                                   @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled) {
        return roleService.findAll(name, enabled);
    }

    @RequestMapping(value="/roles", method = RequestMethod.PUT)
    public Role addRole(@RequestBody Role role) {
        return roleService.addRole(role);
    }

    @RequestMapping(value="/roles/{id}", method = RequestMethod.GET)
    public Role findRole(@PathVariable Long id) {
        return roleService.findById(id);
    }


    @RequestMapping(value="/roles/{id}/disable", method = RequestMethod.POST)
    public Role disableRole(@PathVariable Long id) {
        return roleService.disableRole(id);
    }
    @RequestMapping(value="/roles/{id}/enable", method = RequestMethod.POST)
    public Role enableRole(@PathVariable Long id) {
        return roleService.enableRole(id);
    }


    @RequestMapping(value="/roles/{id}/menus", method = RequestMethod.GET)
    public List<MenuGroup> getAccessibleMenus(@PathVariable Long id) {

        return menuGroupService.getAccessibleMenus(roleService.findById(id));
    }

    @RequestMapping(value="/roles/{id}/menus", method = RequestMethod.POST)
    public ResponseBodyWrapper processMenus(@PathVariable Long id,
                                        @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedMenuIds,
                                        @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedMenuIds) {

        roleService.processMenus(id, assignedMenuIds, deassignedMenuIds);
        return ResponseBodyWrapper.success("success");
    }

    @RequestMapping(value="/roles/{id}/users", method = RequestMethod.POST)
    public ResponseBodyWrapper processUsers(@PathVariable Long id,
                                            @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedUserIds,
                                            @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedUserIds) {

        roleService.processUsers(id, assignedUserIds, deassignedUserIds);
        return ResponseBodyWrapper.success("success");
    }


}

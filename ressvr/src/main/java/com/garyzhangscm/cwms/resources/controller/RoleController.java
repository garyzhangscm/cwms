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
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RoleMenuRepository;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.RoleMenuService;
import com.garyzhangscm.cwms.resources.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RoleController {

    @Autowired
    RoleService roleService;
    @Autowired
    MenuGroupService menuGroupService;
    @Autowired
    private RoleMenuService roleMenuService;

    @RequestMapping(value="/roles", method = RequestMethod.GET)
    public List<Role> findAllRoles(@RequestParam Long companyId,
                                   @RequestParam(name="name", required = false, defaultValue = "") String name,
                                   @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled) {
        return roleService.findAll(companyId, name, enabled);
    }

    @BillableEndpoint
    @RequestMapping(value="/roles", method = RequestMethod.PUT)
    public Role addRole(@RequestBody Role role) {
        return roleService.addRole(role);
    }

    @RequestMapping(value="/roles/{id}", method = RequestMethod.GET)
    public Role findRole(@PathVariable Long id) {
        return roleService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/disable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public Role disableRole(@PathVariable Long id) {
        return roleService.disableRole(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/enable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public Role enableRole(@PathVariable Long id) {
        return roleService.enableRole(id);
    }


    @RequestMapping(value="/roles/{id}/menus", method = RequestMethod.GET)
    public List<MenuGroup> getAccessibleMenus(@PathVariable Long id) {

        Role role = roleService.findById(id);
        return menuGroupService.getAccessibleMenus(role.getCompanyId(), role);
    }

    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/menus", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public ResponseBodyWrapper processMenus(@PathVariable Long id,
                                            @RequestParam(name = "assignedFullyFunctionalMenuIds", required = false, defaultValue = "") String assignedFullyFunctionalMenuIds,
                                            @RequestParam(name = "assignedDisplayOnlyMenuIds", required = false, defaultValue = "") String assignedDisplayOnlyMenuIds,
                                            @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedMenuIds) {

        // roleService.processMenus(id, assignedMenuIds, deassignedMenuIds);

        roleService.processMenuAssignment(id,
                assignedFullyFunctionalMenuIds,
                assignedDisplayOnlyMenuIds,
                deassignedMenuIds);

        return ResponseBodyWrapper.success("success");
    }


    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/menus", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public ResponseBodyWrapper processOperationTypes(
            @PathVariable Long id,
            @RequestParam(name = "newlyAssignedOperationTypeIds", required = false, defaultValue = "") String newlyAssignedOperationTypeIds) {

        // roleService.processMenus(id, assignedMenuIds, deassignedMenuIds);

        roleService.processOperationTypes(id,
                newlyAssignedOperationTypeIds);

        return ResponseBodyWrapper.success("success");
    }

    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/users", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public ResponseBodyWrapper processUsers(@PathVariable Long id,
                                            @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedUserIds,
                                            @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedUserIds) {

        roleService.processUsers(id, assignedUserIds, deassignedUserIds);
        return ResponseBodyWrapper.success("success");
    }

    @BillableEndpoint
    @RequestMapping(value="/roles/{id}/clients", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "CommonService_Role", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Role", allEntries = true),
            }
    )
    public ResponseBodyWrapper processClients(@PathVariable Long id,
                                              @RequestParam Boolean nonClientDataAccessible,
                                              @RequestParam Boolean allClientAccess,
                                            @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedClientIds,
                                            @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedClientIds) {

        roleService.processClients(id, assignedClientIds, deassignedClientIds,
                nonClientDataAccessible, allClientAccess);
        return ResponseBodyWrapper.success("success");
    }


    @RequestMapping(value="/roles/{id}/permissions", method = RequestMethod.POST)
    public List<RolePermission> processPermissions(@PathVariable Long id,
                                                   @RequestParam Long companyId,
                                                   @RequestParam Long warehouseId,
                                                   @RequestBody List<RolePermission> rolePermissions) {

        return roleService.processPermissions(id, rolePermissions);
    }

}

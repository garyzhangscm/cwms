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

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RolePermissionRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RolePermissionService {
    private static final Logger logger = LoggerFactory.getLogger(RolePermissionService.class);
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private  PermissionService permissionService;
    @Autowired
    private MenuService menuService;

    public RolePermission findByRoleAndPermission(Role role, Permission permission) {
        return rolePermissionRepository.findByRoleAndPermission(role, permission);
    }

    public RolePermission save(RolePermission rolePermission) {
        return rolePermissionRepository.save(rolePermission);
    }

    public RolePermission saveOrUpdate(RolePermission rolePermission) {
        if (Objects.isNull(rolePermission.getId()) &&
                Objects.nonNull(findByRoleAndPermission(rolePermission.getRole(), rolePermission.getPermission()))) {
            rolePermission.setId(
                    findByRoleAndPermission(rolePermission.getRole(),rolePermission.getPermission()).getId()
            );
        }
        return save(rolePermission);
    }

    public boolean isAccessible(Role role, Permission permission) {
        RolePermission rolePermission =
                rolePermissionRepository.findByRoleAndPermission(
                        role, permission
                );

        // by default, we will always allow the user to access any
        // button / link, unless we explicitly disallow it
        if (Objects.isNull(rolePermission)) {
            return true;
        }
        return rolePermission.getAllowAccess();
    }

    public RolePermission processPermission(Role role, RolePermission rolePermission) {
        // setup role and menu, if necessary
        // if the role permission is assigned or unassigned from the web page
        // then the menu may be empty but either menu id or menu name should be
        // passed in
        rolePermission.setRole(role);
        if (Objects.nonNull(rolePermission.getPermission().getId())) {
            Permission permission = permissionService.findById(rolePermission.getPermission().getId());
            rolePermission.setPermission(permission);
            return saveOrUpdate(rolePermission);
        }

        // the permission's id is not setup yet, let's see if we can find menu
        // then we can find the permission by menu and the permission name
        // first of all, make sure the permission name is setup
        if (Strings.isBlank(rolePermission.getPermission().getName())) {
            throw ResourceNotFoundException.raiseException("permission's name is not passed in , " +
                    "not able to assign to the role " + role.getName());
        }
        Permission permission = null;
        if (Objects.nonNull(rolePermission.getPermission().getMenu()) &&
                Objects.nonNull(rolePermission.getPermission().getMenu().getId())) {
            permission = permissionService.findByMenuAndName(
                    rolePermission.getPermission().getMenu(),
                    rolePermission.getPermission().getName()
            );
            logger.debug("found permission by menu {}, name {}? {}",
                    rolePermission.getPermission().getMenu().getName(),
                    rolePermission.getPermission().getName(),
                    Objects.nonNull(permission));
        }
        else if (Objects.nonNull(rolePermission.getPermission().getMenuId())) {

            Menu menu = menuService.findById(
                    rolePermission.getPermission().getMenuId()
            );
            if (Objects.isNull(menu)) {
                throw ResourceNotFoundException.raiseException("can't find menu by id  " +
                        rolePermission.getPermission().getMenuId());
            }
            permission = permissionService.findByMenuAndName(
                    menu,
                    rolePermission.getPermission().getName()
            );
            logger.debug("found permission by menu id {}, name {}? {}",
                    rolePermission.getPermission().getMenuId(),
                    rolePermission.getPermission().getName(),
                    Objects.nonNull(permission));
        }
        else if (Strings.isNotBlank(rolePermission.getPermission().getMenuName())) {

            Menu menu = menuService.findByName(
                    rolePermission.getPermission().getMenuName()
            );
            if (Objects.isNull(menu)) {
                throw ResourceNotFoundException.raiseException("can't find menu by name  " +
                        rolePermission.getPermission().getMenuName());
            }
            permission = permissionService.findByMenuAndName(
                    menu,
                    rolePermission.getPermission().getName()
            );
            logger.debug("found permission by menu name {}, name {}? {}",
                    rolePermission.getPermission().getMenuName(),
                    rolePermission.getPermission().getName(),
                    Objects.nonNull(permission));
        }
        if (Objects.isNull(permission)) {

            throw ResourceNotFoundException.raiseException("can't find permission by name " +
                    rolePermission.getPermission().getName());
        }
        rolePermission.setPermission(permission);
        return saveOrUpdate(rolePermission);
    }
}

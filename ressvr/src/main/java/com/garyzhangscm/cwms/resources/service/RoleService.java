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
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RoleRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class RoleService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MenuService menuService;
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.roles:roles}")
    String testDataFile;


    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("role  not found by id: " + id));
    }

    public List<Role> findAll(Long companyId, String name, Boolean enabled) {

        return roleRepository.findAll(
                (Root<Role> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (!StringUtils.isBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }


                    if (enabled != null) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


    }

    public Role findByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }

    public Role saveOrUpdate(Role role) {
        if (Objects.isNull(role.getId()) && findByName(role.getName()) != null) {
            role.setId(findByName(role.getName()).getId());
        }
        return save(role);
    }


    public List<Role> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("companyId").
                addColumn("name").
                addColumn("description").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Role.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Role> roles = loadData(inputStream);
            roles.stream().forEach(role -> saveOrUpdate(role));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    public void processMenus(Long roleId, String assignedMenuIds, String deassignedMenuIds) {

        Role role = findById(roleId);
        if (!StringUtils.isBlank(assignedMenuIds)) {
            Arrays.stream(assignedMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(menuId -> {
                        Menu menu = menuService.findById(menuId);
                        role.assignMenu(menu);
                    });
        }
        if (!StringUtils.isBlank(deassignedMenuIds)) {
            Arrays.stream(deassignedMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(menuId -> {
                        Menu menu = menuService.findById(menuId);
                        role.deassignMenu(menu);
                    });
        }
        saveOrUpdate(role);

    }

    public void processUsers(Long roleId, String assignedUserIds, String deassignedUserIds) {

        Role role = findById(roleId);
        if (!StringUtils.isBlank(assignedUserIds)) {
            Arrays.stream(assignedUserIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(userId -> {
                        User user = userService.findById(userId, false);
                        user.assignRole(role);
                        userService.saveOrUpdate(user);
                    });
        }
        if (!StringUtils.isBlank(deassignedUserIds)) {
            Arrays.stream(deassignedUserIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(userId -> {
                        User user = userService.findById(userId, false);
                        user.deassignRole(role);
                        userService.saveOrUpdate(user);
                    });
        }

    }

    @Transactional
    public Role addRole(Role role) {

        logger.debug("start to add role: \n{}", role);
        // assign the menu to the role
        if (role.getMenuGroups().size() > 0) {
            role.getMenuGroups().forEach(menuGroup -> {
                menuGroup.getMenuSubGroups().forEach(menuSubGroup -> {
                    menuSubGroup.getMenus().forEach(menu -> {
                        logger.debug("Assign menu: {} to the role {}", menu, role.getName());
                        Menu assignedMenu = menuService.findById(menu.getId());
                        role.assignMenu(assignedMenu);
                    });
                });
            });
        }

        // create the role with menus
        Role newRole = save(role);

        // assign the role to the user
        if (role.getUsers().size() >0) {
            role.getUsers().forEach(user -> {

                logger.debug("Assign role: {} to the user {}", role.getName(), user);
                user = userService.findById(user.getId());
                user.assignRole(newRole);
                userService.saveOrUpdate(user);
            });
        }
        return newRole;



    }

    public Role disableRole(Long id){
        Role role = findById(id);
        role.setEnabled(false);
        return saveOrUpdate(role);
    }
    public Role enableRole(Long id){
        Role role = findById(id);
        role.setEnabled(true);
        return saveOrUpdate(role);
    }


}

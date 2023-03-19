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
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RoleRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    @Autowired
    private RoleClientAccessService roleClientAccessService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.roles:roles}")
    String testDataFile;


    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("role  not found by id: " + id));
    }

    public List<Role> findAll(Long companyId, String name,  Boolean enabled) {

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

    public Role findByName(Long companyId, String name) {
        return roleRepository.findByCompanyIdAndName(companyId, name);
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }

    public Role saveOrUpdate(Role role) {
        if (Objects.isNull(role.getId()) && findByName(role.getCompanyId(), role.getName()) != null) {
            role.setId(findByName(role.getCompanyId(), role.getName()).getId());
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

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Role> roles = loadData(inputStream);
            roles.stream().forEach(role -> saveOrUpdate(role));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
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

    public void processMenuAssignment(Long roleId, String assignedFullyFunctionalMenuIds,
                                      String assignedDisplayOnlyMenuIds, String deassignedMenuIds) {


        Role role = findById(roleId);

        // process the non display only menus
        if (!StringUtils.isBlank(assignedFullyFunctionalMenuIds)) {
            Arrays.stream(assignedFullyFunctionalMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(menuId -> {
                        // see if the menu is already assigned
                        RoleMenu roleMenu = getRoleMenuAccess(role, menuId);
                        if (Objects.isNull(roleMenu)) {
                            // the role doesn't have access to the menu before, let's
                            // assign the menu to this role
                            assignNewMenu(role, menuId, false);
                        }
                        else {
                            // the menu is already assigned to the role, we will only need to
                            // setup the display only flag
                            roleMenu.setDisplayOnlyFlag(false);
                        }
                    });
        }
        if (!StringUtils.isBlank(assignedDisplayOnlyMenuIds)) {
            Arrays.stream(assignedDisplayOnlyMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(menuId -> {
                        // see if the menu is already assigned
                        RoleMenu roleMenu = getRoleMenuAccess(role, menuId);
                        if (Objects.isNull(roleMenu)) {
                            // the role doesn't have access to the menu before, let's
                            // assign the menu to this role
                            assignNewMenu(role, menuId, true);
                        }
                        else {
                            // the menu is already assigned to the role, we will only need to
                            // setup the display only flag
                            roleMenu.setDisplayOnlyFlag(true);
                        }
                    });
        }
        if (!StringUtils.isBlank(deassignedMenuIds)) {
            Arrays.stream(deassignedMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(menuId -> {
                        deassignMenu(role, menuId);
                    });
        }
        saveOrUpdate(role);

    }
    private void deassignMenu(Role role, long menuId) {
        Iterator<RoleMenu> roleMenuIterator = role.getRoleMenus().iterator();
        while(roleMenuIterator.hasNext()) {
            RoleMenu roleMenu = roleMenuIterator.next();
            if (roleMenu.getMenu().getId().equals(menuId)) {
                roleMenuIterator.remove();
                // we should only have one menu with the specific menu id that is
                // assigned to the role
                break;
            }
        }
    }

    private void assignNewMenu(Role role, long menuId, boolean displayOnly) {
        Menu menu = menuService.findById(menuId);
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRole(role);
        roleMenu.setMenu(menu);
        roleMenu.setDisplayOnlyFlag(displayOnly);

        role.addRoleMenu(roleMenu);
    }

    private RoleMenu getRoleMenuAccess(Role role, Long menuId) {
        return role.getRoleMenus().stream().filter(
                roleMenu -> roleMenu.getMenu().getId().equals(menuId)
        ).findFirst().orElse(null);
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
                        // Menu assignedMenu = menuService.findById(menu.getId());
                        // role.assignMenu(assignedMenu);
                        assignNewMenu(role, menu.getId(), false);
                    });
                });
            });
        }
        // setup the client access, if not done yet
        logger.debug("start to setup client access");
        role.getClientAccesses().forEach(
                clientAccess -> {

                    if (Objects.nonNull(role.getId())) {
                        // this is an existing role, let's setup the
                        RoleClientAccess existingRoleClientAccess =
                                roleClientAccessService.findByRoleAndClient(role.getId(), clientAccess.getClientId());
                        if (Objects.nonNull(existingRoleClientAccess)) {
                            clientAccess.setId(existingRoleClientAccess.getId());
                        }
                    }
                    clientAccess.setRole(role);
                }
        );

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


    public Role processClients(Long roleId, String assignedClientIds,
                               String deassignedClientIds, Boolean nonClientDataAccessible,
                               Boolean allClientAccess) {
        Role role = findById(roleId);
        role.setNonClientDataAccessible(nonClientDataAccessible);
        role.setAllClientAccess(allClientAccess);


        Iterator<RoleClientAccess> existingRoleClientAccessIterator = role.getClientAccesses().iterator();
        Set<Long> deassignedClientIdSet = new HashSet<>();

        if (Strings.isNotBlank(deassignedClientIds)) {
            Arrays.stream(deassignedClientIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(clientId -> {
                        logger.debug("We will remove client {} for role {}",
                                clientId, roleId);
                        deassignedClientIdSet.add(clientId);
                    });
            // remove the deassigned clients
            while(existingRoleClientAccessIterator.hasNext()) {
                RoleClientAccess roleClientAccess = existingRoleClientAccessIterator.next();
                if (deassignedClientIdSet.contains(roleClientAccess.getClientId())) {
                    existingRoleClientAccessIterator.remove();
                }
            }
        }


        // add the assigned client id;
        if (Strings.isNotBlank(assignedClientIds)) {
            Arrays.stream(assignedClientIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(clientId -> {
                        RoleClientAccess roleClientAccess = new RoleClientAccess(
                                role,clientId
                        );
                        role.addClientAccess(roleClientAccess);
                    });
        }
        return saveOrUpdate(role);

    }
}

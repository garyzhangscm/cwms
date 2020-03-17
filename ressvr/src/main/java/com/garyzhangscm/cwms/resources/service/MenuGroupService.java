/**
 * Copyright 2018
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
import com.garyzhangscm.cwms.resources.repository.MenuGroupRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class MenuGroupService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(MenuGroupService.class);

    @Autowired
    private MenuGroupRepository menuGroupRepository;
    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.menu-groups:menu-groups}")
    String testDataFile;

    public MenuGroup findById(Long id) {
        return menuGroupRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Menu group not found by id: " + id));
    }

    public List<MenuGroup> findAll() {

        return menuGroupRepository.findAll();
    }

    public MenuGroup findByName(String name) {

        return menuGroupRepository.findByName(name);
    }

    public MenuGroup save(MenuGroup menuGroup) {
        return menuGroupRepository.save(menuGroup);
    }

    public List<MenuGroup> getAccessibleMenus(User user) {
        List<MenuGroup> menuGroups;
        logger.debug("User {} is admin? {}", user.getUsername(), user.getAdmin());
        if (user.getAdmin()) {
            // If the user is admin, he/she has access to all menus
            menuGroups = findAll();
        }
        else {
            menuGroups = getAccessibleMenus(user.getRoles());
        }
        menuGroups.sort(Comparator.comparing(MenuGroup::getSequence));

        return menuGroups;
    }

    public List<MenuGroup> getAccessibleMenus(Role role) {

        List<MenuGroup> menuGroups = getAccessibleMenus(
                Collections.singletonList(role)
        );;
        menuGroups.sort(Comparator.comparing(MenuGroup::getSequence));

        return menuGroups;
    }

    // Get all the accessible menu based upon the list of roles
    private List<MenuGroup> getAccessibleMenus(List<Role> roles) {

        // Save the id of menus that are accessible from the list
        // of roles, to make it easy for checking
        Map<Long, Long> accessibleMenuIdMap = getAccessibleMenuIdMap(roles);

        // Let's get all the menu groups and then loop one by one
        // to see if the list of roles has access to the menu
        // As long as there's one role has access, we will keep
        // the menu.
        // As long as there's one menu in the group left, we will
        // keep the menu group
        List<MenuGroup> menuGroups = findAll();
        Iterator<MenuGroup> menuGroupIterator = menuGroups.iterator();
        while(menuGroupIterator.hasNext()) {
            MenuGroup menuGroup = menuGroupIterator.next();
            Iterator<MenuSubGroup> menuSubGroupIterator = menuGroup.getMenuSubGroups().iterator();
            while(menuSubGroupIterator.hasNext()) {
                MenuSubGroup menuSubGroup = menuSubGroupIterator.next();
                Iterator<Menu> menuIterator = menuSubGroup.getMenus().iterator();
                while(menuIterator.hasNext()) {
                    Menu menu = menuIterator.next();
                    if (!accessibleMenuIdMap.containsKey(menu.getId())) {
                        menuIterator.remove();
                    }
                }
                if (menuSubGroup.getMenus().size() == 0) {
                    menuSubGroupIterator.remove();
                }
            }
            if (menuGroup.getMenuSubGroups().size() == 0) {
                menuGroupIterator.remove();
            }
        }
        return menuGroups;

    }

    private Map<Long, Long> getAccessibleMenuIdMap(List<Role> roles) {
        Map<Long, Long> accessibleMenuIdMap = new HashMap<>();
        roles.stream().forEach(role -> {

            role.getMenus().forEach(menu -> accessibleMenuIdMap.put(menu.getId(), menu.getId()));
                }
        );
        return accessibleMenuIdMap;
    }

    // As long as one role in the list has access to the menu,
    // we will return true;
    private boolean isAccessible(Menu menu, List<Role> roles) {
        for(Role role : roles) {
            if (isAccessible(menu, role)) {
                return true;
            }
        }
        return false;

    }
    private boolean isAccessible(Menu menu, Role role) {
        for(Menu assignedMenu: role.getMenus()) {
            if (assignedMenu.equals(menu)) {
                return true;
            }
        }
        return false;

    }

    public List<MenuGroup> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("text").
                addColumn("name").
                addColumn("i18n").
                addColumn("group").
                addColumn("hideInBreadcrumb").
                addColumn("sequence").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MenuGroup.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MenuGroup> menuGroups = loadData(inputStream);
            menuGroups.stream().forEach(menuGroup -> save(menuGroup));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

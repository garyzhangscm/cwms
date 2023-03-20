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
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.MenuGroupRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuGroupService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(MenuGroupService.class);

    @Autowired
    private MenuGroupRepository menuGroupRepository;
    @Autowired
    private RoleService roleService;
    @Autowired
    private CompanyMenuService companyMenuService;

    @Autowired
    private RoleMenuService roleMenuService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    @Value("${fileupload.test-data.menu-groups:menu-groups}")
    String testDataFile;

    public MenuGroup findById(Long id) {
        return menuGroupRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Menu group not found by id: " + id));
    }

    public List<MenuGroup> findAll() {
        // return findAll(MenuType.WEB);
        return menuGroupRepository.findAll();
    }
    public List<MenuGroup> findAll(MenuType menuType) {
        if (Objects.isNull(menuType)) {
            return findAll();
        }

        return menuGroupRepository.findByType(menuType);
    }


    public MenuGroup findByName(String name) {

        return menuGroupRepository.findByName(name);
    }

    public MenuGroup save(MenuGroup menuGroup) {
        return menuGroupRepository.save(menuGroup);
    }

    public MenuGroup saveOrUpdate(MenuGroup menuGroup) {
        if (Objects.nonNull(findByName(menuGroup.getName()))) {
            menuGroup.setId(
                    findByName(menuGroup.getName()).getId()
            );
        }
        return menuGroupRepository.save(menuGroup);
    }


    /**
     * Get accessible menu by certain user
     * @param companyId
     * @param username
     * @return
     */
    public List<MenuGroup> getAccessibleMenus(Long companyId, String username) {
        if(Strings.isBlank(username)) {
            username = userService.getCurrentUserName();

        }
        User user = userService.findByUsername(companyId, username);
        List<MenuGroup> menuGroups = new ArrayList<>();
        // get the accessible menu from all menu types
        for (MenuType menuType : MenuType.values()) {
            menuGroups.addAll(
                    getAccessibleMenus(companyId, user, menuType)
            );
        }
        return menuGroups;
    }
    public List<MenuGroup> getAccessibleMenus(Long companyId, String username, MenuType menuType) {
        if(Strings.isBlank(username)) {
            username = userService.getCurrentUserName();

        }
        User user = userService.findByUsername(companyId, username);
        return getAccessibleMenus(companyId, user, menuType);
    }
    public List<MenuGroup> getAccessibleMenus(Long companyId, User user, MenuType menuType) {
        List<MenuGroup> menuGroups;
        logger.debug("User {} is system admin? {}", user.getUsername(), user.getSystemAdmin());
        logger.debug("User {} is admin? {}", user.getUsername(), user.getAdmin());
        if (user.getSystemAdmin()) {
            // If the user is system admin, he/she has access to all menus
            menuGroups = findAll(menuType);
        }

        else {
            menuGroups = getAccessibleMenus(companyId, user.getAdmin(), user.getRoles(), menuType);
        }
        menuGroups.sort(Comparator.comparing(MenuGroup::getSequence));

        return menuGroups;
    }

    public List<MenuGroup> getAccessibleMenus(Long companyId, Role role) {

        List<MenuGroup> menuGroups = getAccessibleMenus(companyId,
                Collections.singletonList(role)
        );;
        menuGroups.sort(Comparator.comparing(MenuGroup::getSequence));

        return menuGroups;
    }

    // Get all the accessible menu based upon the list of roles
    private List<MenuGroup> getAccessibleMenus(Long companyId,List<Role> roles) {
        return getAccessibleMenus(companyId, false, roles, null);
    }
    private List<MenuGroup> getAccessibleMenus(Long companyId, Boolean isAdmin, List<Role> roles, MenuType menuType) {

        // Let's get all the menu groups and then loop one by one
        // to see if the list of roles has access to the menu
        // As long as there's one role has access, we will keep
        // the menu.
        // As long as there's one menu in the group left, we will
        // keep the menu group
        List<MenuGroup> menuGroups = findAll(menuType);

        List<CompanyMenu> companyMenus = companyMenuService.findAll(companyId);
        // logger.debug("We got menu Groups: {}", menuGroups);
        Iterator<MenuGroup> menuGroupIterator = menuGroups.iterator();
        while(menuGroupIterator.hasNext()) {
            MenuGroup menuGroup = menuGroupIterator.next();
            Iterator<MenuSubGroup> menuSubGroupIterator = menuGroup.getMenuSubGroups().iterator();
            while(menuSubGroupIterator.hasNext()) {
                MenuSubGroup menuSubGroup = menuSubGroupIterator.next();
                Iterator<Menu> menuIterator = menuSubGroup.getMenus().iterator();
                while(menuIterator.hasNext()) {
                    Menu menu = menuIterator.next();
                    if (Boolean.TRUE.equals(menu.getSystemAdminMenuFlag())) {
                        // for system admin menu, we will ignore here
                        // it is only accessible by system admin, which will have
                        // full access to everything and thus handled separately

                        menuIterator.remove();
                        continue;
                    }
                    else if (!Boolean.TRUE.equals(menu.getEnabled())) {
                        // menu is diabled
                        menuIterator.remove();
                        continue;
                    }
                    else if (!isAccessibleByCompany(menu, companyMenus)) {
                        // menu is not assigned to this company

                        menuIterator.remove();
                        continue;
                    }
                    // for admin, the user has access to all menus that is assigned to
                    // this company, regardless of the roles that assigned to the
                    // user
                    else if (!isAdmin && !isAccessible(menu, roles)) {

                        menuIterator.remove();
                        continue;
                    }
                    // the user has access to the menu, let's see if any
                    // role has non display access to the menu. The user
                    // has full access to the menu as long as one of the role
                    // has full access

                    boolean displayOnly = roles.stream().noneMatch(
                            role -> !roleMenuService.isDisplayOnly(role, menu)
                    );
                    logger.debug("roles: {}",
                            roles.stream().map(role -> role.getName()).collect(Collectors.joining(",")));
                    logger.debug("menu: {}", menu.getName());
                    logger.debug(">> display only: {}", displayOnly);
                    menu.setDisplayOnly(displayOnly);
                }
                if (menuSubGroup.getMenus().size() == 0) {
                    // there's nothing left in the sub group, let's
                    // remove it from the result
                    menuSubGroupIterator.remove();
                }
            }
            if (menuGroup.getMenuSubGroups().size() == 0) {
                // there's nothing left in the  group, let's
                // remove it from the result

                menuGroupIterator.remove();
            }
        }
        return menuGroups;

    }

/**
    private Map<Long, Long> getAccessibleMenuIdMap(Long companyId, MenuType menuType) {
        Map<Long, Long> accessibleMenuIdMap = new HashMap<>();
        // see if we have defined the restriction of the menus for the company. If we haven't
        // then it means the company have access to all menu.
        List<CompanyMenu> companyMenus = companyMenuService.findAll(companyId);
        // save the menu id into the hashmap so it will be faster to find out
        // which menu has been assigned to the company
        Map<Long, Long> companyMenuMap = new HashMap<>();
        companyMenus.forEach(
                companyMenu -> companyMenuMap.put(companyMenu.getMenu().getId(), 1l)
        );


        List<MenuGroup> menuGroups = findAll(menuType);
        // logger.debug("We got menu Groups: {}", menuGroups);
        Iterator<MenuGroup> menuGroupIterator = menuGroups.iterator();
        while(menuGroupIterator.hasNext()) {
            MenuGroup menuGroup = menuGroupIterator.next();
            Iterator<MenuSubGroup> menuSubGroupIterator = menuGroup.getMenuSubGroups().iterator();
            while(menuSubGroupIterator.hasNext()) {
                MenuSubGroup menuSubGroup = menuSubGroupIterator.next();
                Iterator<Menu> menuIterator = menuSubGroup.getMenus().iterator();
                while(menuIterator.hasNext()) {
                    Menu menu = menuIterator.next();
                    if (Boolean.TRUE.equals(menu.getSystemAdminMenuFlag())) {
                        // for system admin menu, we will ignore here
                        // it is only accessible by system admin, which will have
                        // full access to everything and thus handled separately
                        menuIterator.remove();
                    }
                    if (!accessibleMenuIdMap.containsKey(menu.getId())) {
                        // logger.debug("accessible menu id map doesn't have the menu {} / {}, will remove it",
                        //       menu.getId(), menu.getName());
                        menuIterator.remove();
                    }
                }
                if (menuSubGroup.getMenus().size() == 0) {
                    // logger.debug("menuSubGroup {} / {} is empty, will remove it",
                    //        menuSubGroup.getId(), menuSubGroup.getName());
                    menuSubGroupIterator.remove();
                }
            }
            if (menuGroup.getMenuSubGroups().size() == 0) {
                // logger.debug("menuGroup {} / {} is empty, will remove it",
                //       menuGroup.getId(), menuGroup.getName());
                menuGroupIterator.remove();
            }
        }

        return accessibleMenuIdMap;
    }
   **/
/**
    private Map<Long, Long> getAccessibleMenuIdMap(Long companyId, List<Role> roles, MenuType menuType) {
        Map<Long, Long> accessibleMenuIdMap = new HashMap<>();
        // see if we have defined the restriction of the menus for the company. If we haven't
        // then it means the company have access to all menu.
        List<CompanyMenu> companyMenus = companyMenuService.findAll(companyId);
        // save the menu id into the hashmap so it will be faster to find out
        // which menu has been assigned to the company
        Map<Long, Long> companyMenuMap = new HashMap<>();
        companyMenus.forEach(
                companyMenu -> companyMenuMap.put(companyMenu.getMenu().getId(), 1l)
        );

        roles.stream().forEach(role -> {

            role.getMenus().stream()
                    // skip the disabled menu first
                    .filter(menu -> !Boolean.FALSE.equals(menu.getEnabled()))
                    // will only return user accessible menu
                    .filter(menu -> !Boolean.TRUE.equals(menu.getSystemAdminMenuFlag()))
                    .filter(menu -> companyMenuMap.isEmpty() || companyMenuMap.containsKey(menu.getId()))
                    // filter out the menu if the type passed in and the menu's type doesn't
                    // match with the criteria
                    .filter(menu -> Objects.isNull(menuType) ? true :
                         menu.getMenuSubGroup().getMenuGroup().getType().equals(menuType))
                    .forEach(menu -> accessibleMenuIdMap.put(menu.getId(), menu.getId()));
                }
        );
        return accessibleMenuIdMap;
    }
**/
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
        return role.canAccessMenu(menu.getId());

    }

    private boolean isAccessible(Menu menu, Long companyId) {

        // see if we have defined the restriction of the menus for the company. If we haven't
        // then it means the company have access to all menu.
        List<CompanyMenu> companyMenus = companyMenuService.findAll(companyId);
        if (companyMenus.isEmpty()) {
            return true;
        }

        return companyMenus.stream().anyMatch(companyAssignedMenu -> companyAssignedMenu.equals(menu));

    }

    private boolean isAccessibleByCompany(Menu menu, List<CompanyMenu> companyMenus) {

        // see if we have defined the restriction of the menus for the company. If we haven't
        // then it means the company have access to all menu.

        if (companyMenus.isEmpty()) {
            return true;
        }

        return companyMenus.stream().anyMatch(companyAssignedMenu -> companyAssignedMenu.equals(menu));

    }


    public List<MenuGroup> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("type").
                addColumn("text").
                addColumn("name").
                addColumn("i18n").
                addColumn("group").
                addColumn("hideInBreadcrumb").
                addColumn("sequence").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MenuGroup.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MenuGroup> menuGroups = loadData(inputStream);
            menuGroups.stream().forEach(menuGroup -> saveOrUpdate(menuGroup));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    public List<MenuGroup> getCompanyAccessibleMenu(Long companyId) {
        // company admin will always have access to all menus as long as
        // 1. the menu is enabled
        // 2. the menu is assigned to the company
        return getAccessibleMenus(companyId, true, new ArrayList<>(), null);
    }
}

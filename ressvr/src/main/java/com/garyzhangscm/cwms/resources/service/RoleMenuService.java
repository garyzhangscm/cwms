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
import com.garyzhangscm.cwms.resources.model.*;
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
import java.util.stream.Collectors;

@Service
public class RoleMenuService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(RoleMenuService.class);


    @Autowired
    private MenuService menuService;
    @Autowired
    private RoleService roleService;

    @Autowired
    private FileService fileService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Value("${fileupload.test-data.role-menus:role-menus}")
    String testDataFile;

    public List<RoleMenu> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("roleName").
                addColumn("menuName").
                build().withHeader();

        return fileService.loadData(inputStream, schema, RoleMenu.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<RoleMenu> roleMenus = loadData(inputStream);
            // Save the user role result as a map
            // key: role name
            // value: a list of menu name
            Map<String, Set<String>> roleMenuMap = new HashMap<>();

            roleMenus.stream().forEach(roleMenu -> {
                Set<String> menus = roleMenuMap.getOrDefault(roleMenu.getRoleName(), new HashSet<>());
                menus.add(roleMenu.getMenuName());
                roleMenuMap.put(roleMenu.getRoleName(), menus);
            });

            roleMenuMap.entrySet().stream().forEach(roleMenuEntry -> {
                Role role = roleService.findByName(companyId, roleMenuEntry.getKey());
                List<Menu> menus = new ArrayList<>();
                Set<String> menuNames = roleMenuEntry.getValue();
                menuNames.stream().forEach(menuName -> {
                    menus.add(menuService.findByName(menuName));
                });
                role.setMenus(menus);
                roleService.saveOrUpdate(role);
            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

}

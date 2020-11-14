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
public class UserRoleService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(UserRoleService.class);


    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.user-roles:user-roles}")
    String testDataFile;

    public List<UserRole> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("companyId").
                addColumn("username").
                addColumn("roleName").
                build().withHeader();

        return fileService.loadData(inputStream, schema, UserRole.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<UserRole> userRoles = loadData(inputStream);
            // Save the user role result as a map
            // key: companyId - username
            // value: a list of role name
            Map<String, Set<String>> userRoleMap = new HashMap<>();

            userRoles.stream().forEach(userRole -> {
                String key = userRole.getCompanyId() + "-" + userRole.getUsername();
                Set<String> roles =
                        userRoleMap.getOrDefault(key, new HashSet<>());
                roles.add(userRole.getRoleName());
                userRoleMap.put(key, roles);
            });

            userRoleMap.entrySet().stream().forEach(userRoleEntry -> {
                // key: companyId - username
                String key = userRoleEntry.getKey();
                String[] tuple = key.split("-");
                Long companyId = Long.parseLong(tuple[0]);
                String username = tuple[1];

                User user = userService.findByUsername(companyId, username);

                List<Role> roles = new ArrayList<>();
                Set<String> roleNames = userRoleEntry.getValue();
                roleNames.stream().forEach(roleName -> {
                    roles.add(roleService.findByName(roleName));
                });
                user.setRoles(roles);
                userService.saveOrUpdate(user);
            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

}

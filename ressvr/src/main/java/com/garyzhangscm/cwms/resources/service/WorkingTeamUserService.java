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

@Service
public class WorkingTeamUserService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(WorkingTeamUserService.class);


    @Autowired
    private WorkingTeamService workingTeamService;
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.working-team-users:working-team-users}")
    String testDataFile;

    public List<WorkingTeamUser> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("companyId").
                addColumn("workingTeamName").
                addColumn("username").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkingTeamUser.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkingTeamUser> workingTeamUsers = loadData(inputStream);
            // Save the user role result as a map
            // key: companyId - username
            // value: a list of role name
            Map<String, Set<String>> userWorkingTeamMap = new HashMap<>();

            workingTeamUsers.stream().forEach(workingTeamUser -> {
                String key = workingTeamUser.getCompanyId() + "-" + workingTeamUser.getUsername();
                Set<String> workingTeamNames =
                        userWorkingTeamMap.getOrDefault(key, new HashSet<>());
                workingTeamNames.add(workingTeamUser.getWorkingTeamName());
                userWorkingTeamMap.put(key, workingTeamNames);
            });

            userWorkingTeamMap.entrySet().stream().forEach(userWorkingTeamEntry -> {

                // key: companyId - username
                String key = userWorkingTeamEntry.getKey();
                String[] tuple = key.split("-");
                Long companyId = Long.parseLong(tuple[0]);
                String username = tuple[1];

                User user = userService.findByUsername(companyId, username);
                List<WorkingTeam> workingTeams = new ArrayList<>();
                Set<String> workingTeamNames = userWorkingTeamEntry.getValue();
                workingTeamNames.stream().forEach(workingTeamName -> {
                    workingTeams.add(workingTeamService.findByName(workingTeamName));
                });
                user.setWorkingTeams(workingTeams);
                userService.saveOrUpdate(user);
            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


}

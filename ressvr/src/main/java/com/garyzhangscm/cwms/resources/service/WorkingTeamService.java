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
import com.garyzhangscm.cwms.resources.repository.WorkingTeamRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkingTeamService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(WorkingTeamService.class);
    @Autowired
    private WorkingTeamRepository workingTeamRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.working-teams:working-teams}")
    String testDataFile;


    public WorkingTeam findById(Long id, boolean loadDetails) {
        WorkingTeam workingTeam = workingTeamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("working team  not found by id: " + id));
        if (Objects.nonNull(workingTeam) && loadDetails) {
            loadAttribute(workingTeam);
        }
        return workingTeam;
    }

    public WorkingTeam findById(Long id) {
        return findById(id, true);
    }

    public List<WorkingTeam> findAll(Long companyId,String name, Boolean enabled) {
        return findAll(companyId, name, enabled, true);

    }
    public List<WorkingTeam> findAll(Long companyId, String name, Boolean enabled, boolean loadDetails) {

        List<WorkingTeam> workingTeams =
                workingTeamRepository.findAll(
                (Root<WorkingTeam> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

        if (workingTeams.size() > 0 && loadDetails) {
            loadAttribute(workingTeams);
        }
        return workingTeams;


    }

    public WorkingTeam findByName(String name) {
        return findByName(name, true);
    }
    public WorkingTeam findByName(String name, boolean loadDetails) {
        WorkingTeam workingTeam = workingTeamRepository.findByName(name);
        if (Objects.nonNull(workingTeam) && loadDetails) {
            loadAttribute(workingTeam);
        }
        return workingTeam;
    }

    private void loadAttribute(List<WorkingTeam> workingTeams) {

        workingTeams.forEach(workingTeam -> loadAttribute(workingTeam));

    }
    private void loadAttribute(WorkingTeam workingTeam) {

        if (workingTeam.getUsers().size() > 0) {

            userService.loadAttribute(workingTeam.getCompanyId(), workingTeam.getUsers());
        }

    }

    public WorkingTeam save(WorkingTeam workingTeam) {
        WorkingTeam newWorkingTeam = workingTeamRepository.save(workingTeam);
        loadAttribute(newWorkingTeam);
        return newWorkingTeam;
    }

    public WorkingTeam saveOrUpdate(WorkingTeam workingTeam) {
        if (Objects.isNull(workingTeam.getId()) && findByName(workingTeam.getName()) != null) {
            workingTeam.setId(findByName(workingTeam.getName()).getId());
        }
        return save(workingTeam);
    }


    public List<WorkingTeam> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("companyId").
                addColumn("name").
                addColumn("description").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkingTeam.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkingTeam> workingTeams = loadData(inputStream);
            workingTeams.stream().forEach(workingTeam -> saveOrUpdate(workingTeam));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }



    public WorkingTeam addWorkingTeam( WorkingTeam workingTeam) {

        // create the role with menus
        WorkingTeam newWorkingTeam = save(workingTeam);

        // assign the role to the user
        if (workingTeam.getUsers().size() >0) {
            workingTeam.getUsers().forEach(user -> {

                logger.debug("Assign working team: {} to the user {}", workingTeam.getName(), user);
                user = userService.findById(user.getId());
                user.assignWorkingTeam(newWorkingTeam);
                userService.saveOrUpdate(user);
            });
        }
        return newWorkingTeam;


    }

    public WorkingTeam disableWorkingTeam( Long id) {

        WorkingTeam workingTeam = findById(id);
        workingTeam.setEnabled(false);

        return saveOrUpdate(workingTeam);
    }

    public WorkingTeam enableWorkingTeam( Long id) {

        WorkingTeam workingTeam = findById(id);
        workingTeam.setEnabled(true);

        return saveOrUpdate(workingTeam);
    }
    public void processUsers(Long id, String assignedUserIds, String deassignedUserIds) {

        WorkingTeam workingTeam = findById(id);


        if (!StringUtils.isBlank(assignedUserIds)) {
            Arrays.stream(assignedUserIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(userId -> {
                        User user = userService.findById(userId, false);
                        user.assignWorkingTeam(workingTeam);
                        userService.saveOrUpdate(user);
                    });
        }
        if (!StringUtils.isBlank(deassignedUserIds)) {
            Arrays.stream(deassignedUserIds.split(","))
                    .mapToLong(Long::parseLong)
                    .forEach(userId -> {
                        User user = userService.findById(userId, false);
                        user.deassignWorkingTeam(workingTeam);
                        userService.saveOrUpdate(user);
                    });
        }

    }
}

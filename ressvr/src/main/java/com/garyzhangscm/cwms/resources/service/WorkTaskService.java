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

import com.garyzhangscm.cwms.resources.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.WorkTaskException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.WorkTaskRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WorkTaskService{

    private static final Logger logger = LoggerFactory.getLogger(WorkTaskService.class);

    @Autowired
    private WorkTaskRepository workTaskRepository;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private WorkingTeamService workingTeamService;


    public WorkTask findById(Long id) {
        return workTaskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work task not found by id: " + id));
    }

    public List<WorkTask> findAll(Long warehouseId,
                                  String number,
                                  String type,
                                  String status,
                                  String lpn,
                                  String sourceLocationName,
                                  String destinationLocationName,
                                  String assignedUserName,
                                  String assignedRoleName,
                                  String assignedWorkingTeamName,
                                  String currentUserName,
                                  String completeUserName,
                                  String workTaskIds) {
        return findAll(warehouseId, number, type, status,
                lpn, sourceLocationName, destinationLocationName, assignedUserName,
                assignedRoleName, assignedWorkingTeamName, currentUserName, completeUserName, workTaskIds, true);
    }
    public List<WorkTask> findAll(Long warehouseId,
                                  String number,
                                  String type,
                                  String status,
                                  String lpn,
                                  String sourceLocationName,
                                  String destinationLocationName,
                                  String assignedUserName,
                                  String assignedRoleName,
                                  String assignedWorkingTeamName,
                                  String currentUserName,
                                  String completeUserName,
                                  String workTaskIds,
                                  boolean loadAttribute) {

        List<WorkTask> workTasks =  workTaskRepository.findAll(
                (Root<WorkTask> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if(Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (StringUtils.isNotBlank(type)) {
                        predicates.add(criteriaBuilder.equal(root.get("type"), WorkTaskType.valueOf(type)));
                    }
                    if (StringUtils.isNotBlank(status)) {
                        predicates.add(criteriaBuilder.equal(root.get("status"), WorkTaskStatus.valueOf(status)));
                    }
                    if (StringUtils.isNotBlank(lpn)) {
                        List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLpn(
                                warehouseId, lpn
                        );
                        CriteriaBuilder.In<Long> inInventoryIds = criteriaBuilder.in(root.get("inventoryId"));
                        for(Inventory inventory : inventories) {
                            inInventoryIds.value(inventory.getId());
                        }
                        predicates.add(criteriaBuilder.and(inInventoryIds));

                    }
                    if (StringUtils.isNotBlank(sourceLocationName)) {
                        Location location = layoutServiceRestemplateClient.getLocationByName(
                                warehouseId, sourceLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), location.getId()));
                    }
                    if (StringUtils.isNotBlank(destinationLocationName)) {
                        Location location = layoutServiceRestemplateClient.getLocationByName(
                                warehouseId, destinationLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), location.getId()));
                    }
                    // In case we will need to get the user / role / working team information, which is
                    // unique inside one company, we will get the company ID first from the warehouse
                    if (StringUtils.isNotBlank(assignedUserName) ||
                            StringUtils.isNotBlank(assignedRoleName) ||
                            StringUtils.isNotBlank(assignedWorkingTeamName) ||
                            StringUtils.isNotBlank(currentUserName) ||
                            StringUtils.isNotBlank(completeUserName)) {
                        Long companyId = layoutServiceRestemplateClient.getWarehouseById(
                                warehouseId
                        ).getCompanyId();

                        if (StringUtils.isNotBlank(assignedUserName)) {
                            Join<WorkTask, User> joinAssignedUser = root.join("assignedUser", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinAssignedUser.get("username"), assignedUserName));
                        }
                        if (StringUtils.isNotBlank(assignedRoleName)) {
                            Join<WorkTask, Role> joinAssignedRole = root.join("assignedRole", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinAssignedRole.get("name"), assignedRoleName));
                        }
                        if (StringUtils.isNotBlank(assignedWorkingTeamName)) {
                            Join<WorkTask, WorkingTeam> joinAssignedWorkingTeam = root.join("assignedWorkingTeam", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinAssignedWorkingTeam.get("name"), assignedWorkingTeamName));
                        }

                        if (StringUtils.isNotBlank(currentUserName)) {
                            Join<WorkTask, User> joinCurrentUser = root.join("currentUser", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinCurrentUser.get("username"), currentUserName));
                        }
                        if (StringUtils.isNotBlank(completeUserName)) {
                            Join<WorkTask, User> joinCompleteUser = root.join("completeUser", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinCompleteUser.get("username"), completeUserName));
                        }
                    }

                    if (StringUtils.isNotBlank(workTaskIds)) {

                        CriteriaBuilder.In<Long> inWorkTaskIds = criteriaBuilder.in(root.get("id"));
                        for(String workTaskId : workTaskIds.split(",")) {
                            inWorkTaskIds.value(Long.parseLong(workTaskId));
                        }
                        predicates.add(criteriaBuilder.and(inWorkTaskIds));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "createdTime")
        );
        if (workTasks.size() >0 && loadAttribute) {
            loadAttribute(workTasks);
        }
        return workTasks;
    }

    public WorkTask findByNumber(Long warehouseId, String number){
        return workTaskRepository.findFirstByWarehouseIdAndNumber( warehouseId, number);
    }
    public List<WorkTask> findByIds(String workTaskIds){
        return findAll(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                workTaskIds);
    }

    @Transactional
    public WorkTask save(WorkTask workTask) {
        return workTaskRepository.save(workTask);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public WorkTask saveOrUpdate(WorkTask workTask) {
        if (workTask.getId() == null &&
                findByNumber( workTask.getWarehouseId(),
                        workTask.getNumber()) != null) {
            workTask.setId(findByNumber( workTask.getWarehouseId(),
                    workTask.getNumber()).getId());
        }
        return save(workTask);
    }

    public void loadAttribute(List<WorkTask> workTasks) {
        for(WorkTask workTask : workTasks) {
            loadAttribute(workTask);
        }

    }

    public void loadAttribute(WorkTask workTask) {

        if (Objects.nonNull(workTask.getWarehouseId()) && Objects.isNull(workTask.getWarehouse())) {
            workTask.setWarehouse(
                    layoutServiceRestemplateClient.getWarehouseById(
                            workTask.getWarehouseId()));

        }

        if (Objects.nonNull(workTask.getSourceLocationId()) && Objects.isNull(workTask.getSourceLocation())) {
            workTask.setSourceLocation(
                    layoutServiceRestemplateClient.getLocationById(
                            workTask.getSourceLocationId()));

        }

        if (Objects.nonNull(workTask.getDestinationLocationId()) && Objects.isNull(workTask.getDestinationLocation())) {
            workTask.setDestinationLocation(
                    layoutServiceRestemplateClient.getLocationById(
                            workTask.getDestinationLocationId()));

        }
    }
    @Transactional
    public void delete(WorkTask workTask) {
        workTaskRepository.delete(workTask);
    }
    @Transactional
    public void delete(Long id) {
        workTaskRepository.deleteById(id);
    }


    public WorkTask addWorkTask(WorkTask workTask) {
        if(Objects.isNull(workTask.getStatus())) {
            workTask.setStatus(WorkTaskStatus.PENDING);
        }
        if (Objects.isNull(workTask.getNumber())) {

            workTask.setNumber(
                    getNextWorkTaskNumber(workTask.getWarehouseId())
            );
        }
        return save(workTask);
    }

    private String getNextWorkTaskNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "work-task");
    }

    public WorkTask changeWorkTask(WorkTask workTask) {
        return saveOrUpdate(workTask);
    }

    public WorkTask removeWorkTask(Long id) {
        WorkTask workTask = findById(id);
        delete(id);

        return workTask;
    }

    @Transactional
    public List<WorkTask> assignWorkTasks(String workTaskIds, String username, String rolename, String workingTeamName) {


        List<WorkTask> workTasks = findByIds(workTaskIds);

        List<WorkTask> resultWorkTask = new ArrayList<>();
        workTasks.forEach(workTask -> resultWorkTask.add(assignWorkTask(workTask, username, rolename, workingTeamName)));
        return resultWorkTask;



    }

    @Transactional
    public WorkTask assignWorkTask(WorkTask workTask, String username, String rolename, String workingTeamName) {

        Long companyId = layoutServiceRestemplateClient.getWarehouseById(
                workTask.getWarehouseId()
        ).getCompanyId();

        // clear the previous assignment
        workTask.setAssignedUser(null);
        workTask.setAssignedRole(null);
        workTask.setAssignedWorkingTeam(null);

        if (StringUtils.isNotBlank(username)) {
            User user = userService.findByUsername(
                    companyId, username
            );
            workTask.setAssignedUser(user);
            return saveOrUpdate(workTask);
        }
        else if (StringUtils.isNotBlank(rolename)) {
            Role role = roleService.findByName(
                    companyId, rolename
            );
            workTask.setAssignedRole(role);
            return saveOrUpdate(workTask);
        }
        else if (StringUtils.isNotBlank(workingTeamName)) {
            WorkingTeam workingTeam = workingTeamService.findByName(
                    workTask.getWarehouseId(), workingTeamName
            );
            workTask.setAssignedWorkingTeam(workingTeam);
            return saveOrUpdate(workTask);
        }
        else {
            throw WorkTaskException.raiseException("At least one of user / role / working team name has to be passed in for assigning work tasks");

        }
    }

    public List<WorkTask> deassignWorkTasks(String workTaskIds) {
        List<WorkTask> workTasks = new ArrayList<>();
        findByIds(workTaskIds).forEach(workTask -> workTasks.add(deassignWorkTask(workTask)));

        return workTasks;
    }
    public WorkTask deassignWorkTask(WorkTask workTask) {

        workTask.setAssignedUser(null);
        workTask.setAssignedRole(null);
        workTask.setAssignedWorkingTeam(null);
        return saveOrUpdate(workTask);
    }

    @Transactional
    public List<WorkTask> removeAllWorkTasks(Long warehouseId, String number, String workType, String workStatus, String lpn, String sourceLocationName, String destinationLocationName, String assignedUserName, String assignedRoleName, String assignedWorkingTeamName, String currentUserName, String completeUserName, String workTaskIds) {
        List<WorkTask> workTasks = findAll(
                warehouseId, number,
                workType,   workStatus,   lpn,
                sourceLocationName,   destinationLocationName,
                assignedUserName,   assignedRoleName,   assignedWorkingTeamName,
                currentUserName,   completeUserName,   workTaskIds
        );
        workTasks.forEach(workTask -> delete(workTask));
        return workTasks;
    }
}

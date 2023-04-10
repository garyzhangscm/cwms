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

package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.WorkTaskException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.WorkTaskRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WorkTaskService{

    private static final Logger logger = LoggerFactory.getLogger(WorkTaskService.class);

    @Autowired
    private WorkTaskRepository workTaskRepository;
    @Autowired
    private SystemControlledNumberService systemControlledNumberService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;


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
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId, sourceLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), location.getId()));
                    }
                    if (StringUtils.isNotBlank(destinationLocationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
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
                        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                                warehouseId
                        ).getCompanyId();

                        if (StringUtils.isNotBlank(assignedUserName)) {
                            User user = resourceServiceRestemplateClient.getUserByUsername(
                                    companyId, assignedUserName
                            );
                            predicates.add(criteriaBuilder.equal(root.get("assignedUserId"), user.getId()));
                        }
                        if (StringUtils.isNotBlank(assignedRoleName)) {
                            Role role = resourceServiceRestemplateClient.getRoleByName(
                                    companyId, assignedRoleName
                            );
                            predicates.add(criteriaBuilder.equal(root.get("assignedRoleId"), role.getId()));
                        }
                        if (StringUtils.isNotBlank(assignedWorkingTeamName)) {
                            WorkingTeam workingTeam = resourceServiceRestemplateClient.getWorkingTeamByName(
                                    companyId, assignedWorkingTeamName
                            );
                            predicates.add(criteriaBuilder.equal(root.get("assignedWorkingTeamId"), workingTeam.getId()));
                        }

                        if (StringUtils.isNotBlank(currentUserName)) {
                            User user = resourceServiceRestemplateClient.getUserByUsername(
                                    companyId, currentUserName
                            );
                            predicates.add(criteriaBuilder.equal(root.get("currentUserId"), user.getId()));
                        }
                        if (StringUtils.isNotBlank(completeUserName)) {
                            User user = resourceServiceRestemplateClient.getUserByUsername(
                                    companyId, completeUserName
                            );
                            predicates.add(criteriaBuilder.equal(root.get("completeUserId"), user.getId()));
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
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            workTask.getWarehouseId()));

        }

        if (Objects.nonNull(workTask.getSourceLocationId()) && Objects.isNull(workTask.getSourceLocation())) {
            workTask.setSourceLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            workTask.getSourceLocationId()));

        }

        if (Objects.nonNull(workTask.getDestinationLocationId()) && Objects.isNull(workTask.getDestinationLocation())) {
            workTask.setDestinationLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            workTask.getDestinationLocationId()));

        }


        if (Objects.nonNull(workTask.getAssignedUserId()) && Objects.isNull(workTask.getAssignedUser())) {
            workTask.setAssignedUser(
                    resourceServiceRestemplateClient.getUserById(
                            workTask.getAssignedUserId()
                    ));
        }

        if (Objects.nonNull(workTask.getAssignedRoleId()) && Objects.isNull(workTask.getAssignedRole())) {
            workTask.setAssignedRole(
                    resourceServiceRestemplateClient.getRoleById(
                            workTask.getAssignedRoleId()
                    ));
        }

        if (Objects.nonNull(workTask.getAssignedWorkingTeamId()) && Objects.isNull(workTask.getAssignedWorkingTeam())) {
            workTask.setAssignedWorkingTeam(
                    resourceServiceRestemplateClient.getWorkingTeamById(
                            workTask.getAssignedWorkingTeamId()
                    ));
        }

        if (Objects.nonNull(workTask.getCurrentUserId()) && Objects.isNull(workTask.getCurrentUser())) {
            workTask.setCurrentUser(
                    resourceServiceRestemplateClient.getUserById(
                            workTask.getCurrentUserId()
                    ));
        }

        if (Objects.nonNull(workTask.getCompleteUserId()) && Objects.isNull(workTask.getCompleteUser())) {
            workTask.setCompleteUser(
                    resourceServiceRestemplateClient.getUserById(
                            workTask.getCompleteUserId()
                    ));
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
        return systemControlledNumberService.getNextNumber(warehouseId, "work-task").getNextNumber();
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

        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                workTask.getWarehouseId()
        ).getCompanyId();

        // clear the previous assignment
        workTask.setAssignedUserId(null);
        workTask.setAssignedRoleId(null);
        workTask.setAssignedWorkingTeamId(null);

        if (StringUtils.isNotBlank(username)) {
            User user = resourceServiceRestemplateClient.getUserByUsername(
                    companyId, username
            );
            workTask.setAssignedUserId(user.getId());
            return saveOrUpdate(workTask);
        }
        else if (StringUtils.isNotBlank(rolename)) {
            Role role = resourceServiceRestemplateClient.getRoleByName(
                    companyId, rolename
            );
            workTask.setAssignedRoleId(role.getId());
            return saveOrUpdate(workTask);
        }
        else if (StringUtils.isNotBlank(workingTeamName)) {
            WorkingTeam workingTeam = resourceServiceRestemplateClient.getWorkingTeamByName(
                    companyId, workingTeamName
            );
            workTask.setAssignedWorkingTeamId(workingTeam.getId());
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

        workTask.setAssignedUserId(null);
        workTask.setAssignedRoleId(null);
        workTask.setAssignedWorkingTeamId(null);
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

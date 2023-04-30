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
import com.garyzhangscm.cwms.resources.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.WorkTaskException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.WorkTaskRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkTaskService{

    private static final Logger logger = LoggerFactory.getLogger(WorkTaskService.class);

    @Autowired
    private WorkTaskRepository workTaskRepository;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WorkTaskConfigurationService workTaskConfigurationService;

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
    @Autowired
    private RFService rfService;


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
                    logger.debug("we will return the work tasks with status {}",
                            Strings.isNotBlank(status) ? status : "N/A");
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
        if (Strings.isBlank(workTask.getNumber())) {

            workTask.setNumber(
                    getNextWorkTaskNumber(workTask.getWarehouseId())
            );
        }

        WorkTask newWorkTask =  saveOrUpdate(workTask);
        // let's release the work task
        if (newWorkTask.getStatus().equals(WorkTaskStatus.PENDING)) {
            logger.debug("the new work task's status is pending, let's release it and get the operation type and priority");
            newWorkTask = releaseWorkTask(newWorkTask);
        }
        return newWorkTask;
    }

    public WorkTask releaseWorkTask(Long workTaskId) {
        return releaseWorkTask(findById(workTaskId));
    }
    public WorkTask releaseWorkTask(WorkTask workTask) {
        // let's see if we have the work task configuration setup for this work task

        WorkTaskConfiguration workTaskConfiguration =
                workTaskConfigurationService.findBestMatch(
                        workTask.getWarehouseId(),
                        workTask.getSourceLocationId(),
                        workTask.getDestinationLocationId(),
                        workTask.getType()
                );
        if (Objects.nonNull(workTaskConfiguration)) {
            logger.debug("Found the best matched work task configuration: \n{}", workTaskConfiguration);
            logger.debug("start to release work task {} / {}", workTask.getId(), workTask.getNumber());
            Integer priority = Objects.isNull(workTaskConfiguration.getPriority()) ?
                    workTaskConfiguration.getOperationType().getDefaultPriority() :
                    workTaskConfiguration.getPriority();
            if (Objects.isNull(priority)) {
                logger.debug(">> fail to get priority");
                return workTask;
            }
            if (!workTask.getStatus().equals(WorkTaskStatus.COMPLETE)) {

                workTask.setStatus(WorkTaskStatus.RELEASED);
            }
            workTask.setPriority(priority);
            workTask.setOperationType(workTaskConfiguration.getOperationType());
            return saveOrUpdate(workTask);
        }
        logger.debug("Fail to get the best matched work task configuration for work task {} / {}",
                workTask.getId(), workTask.getNumber());

        return workTask;
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

    /**
     * Check if the user can perform the work task
     * @param workTaskId
     * @param userId
     * @return
     */
    public boolean validateWorkTaskAgainstUser(Long workTaskId, Long userId) {
        User user = userService.findById(userId);
        WorkTask workTask = findById(workTaskId);

        return validateWorkTaskAgainstUser(workTask, user);
    }

    public boolean validateWorkTaskAgainstUser(WorkTask workTask, User user) {
        if (Boolean.TRUE.equals(user.getAdmin()) ||
                Boolean.TRUE.equals(user.getSystemAdmin())) {
            return true;
        }

        return user.getRoles().stream().anyMatch(
                role -> validateWorkTaskAgainstRole(workTask, role)
        );
    }
    /**
     * Check if the role can perform the work task
     * @param workTaskId
     * @param roleId
     * @return
     */
    public boolean validateWorkTaskAgainstRole(Long workTaskId, Long roleId) {
        Role role = roleService.findById(roleId);
        WorkTask workTask = findById(workTaskId);

        return validateWorkTaskAgainstRole(workTask, role);
    }

    public boolean validateWorkTaskAgainstRole(WorkTask workTask , Role role) {
        return role.getOperationTypes().stream().anyMatch(
                operationType -> operationType.equals(workTask.getOperationType())
        );
    }

    /**
     * Assign the user to the work task
     * @param workTaskId
     * @param warehouseId
     * @param userId
     * @return
     */
    public WorkTask assignUser(Long workTaskId, Long warehouseId, Long userId) {
        WorkTask workTask = findById(workTaskId);

        if (WorkTaskStatus.WORKING.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "someone is working on the work task " + workTask.getNumber());
        }
        else if (WorkTaskStatus.COMPLETE.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "work task " + workTask.getNumber() + " is already completed!");
        }
        User user = userService.findById(userId);
        // first of all, make sure the user has the right to perform the work task
        if (!validateWorkTaskAgainstUser(workTask, user)) {
            logger.debug("The user {} doesn't have right to do the work task {} / {}",
                    user.getUsername(),
                    workTask.getNumber(),
                    workTask.getOperationType().getName());

            throw RequestValidationFailException.raiseException(
                    "The user " + user.getUsername() +  " doesn't have right to do the work task " +
                            workTask.getNumber() + " / " +  workTask.getOperationType().getName());
        }

        workTask.setAssignedUser(user);
        workTask.setAssignedRole(null);
        workTask.setAssignedWorkingTeam(null);

        return saveOrUpdate(workTask);
    }

    /**
     * Assign the user to the work task
     * @param workTaskId
     * @param warehouseId
     * @return
     */
    public WorkTask unassignUser(Long workTaskId, Long warehouseId) {
        WorkTask workTask = findById(workTaskId);

        if (WorkTaskStatus.WORKING.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "someone is working on the work task " + workTask.getNumber());
        }

        workTask.setAssignedUser(null);

        return saveOrUpdate(workTask);
    }

    /**
     * Assign the role to the work task
     * @param workTaskId
     * @param warehouseId
     * @param roleId
     * @return
     */
    public WorkTask assignRole(Long workTaskId, Long warehouseId, Long roleId) {
        WorkTask workTask = findById(workTaskId);

        if (WorkTaskStatus.WORKING.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "someone is working on the work task " + workTask.getNumber());
        }
        else if (WorkTaskStatus.COMPLETE.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "work task " + workTask.getNumber() + " is already completed!");
        }

        Role role = roleService.findById(roleId);

        // first of all, make sure the role has the right to perform the work task
        if (!validateWorkTaskAgainstRole(workTask, role)) {
            logger.debug("The role {} doesn't have right to do the work task {} / {}",
                    role.getName(),
                    workTask.getNumber(),
                    workTask.getOperationType().getName());

            throw RequestValidationFailException.raiseException(
                    "The role " + role.getName() +  " doesn't have right to do the work task " +
                            workTask.getNumber() + " / " +  workTask.getOperationType().getName());
        }

        workTask.setAssignedUser(null);
        workTask.setAssignedRole(role);
        workTask.setAssignedWorkingTeam(null);

        return saveOrUpdate(workTask);
    }


    /**
     * Assign the role to the work task
     * @param workTaskId
     * @param warehouseId
     * @return
     */
    public WorkTask unassignRole(Long workTaskId, Long warehouseId) {
        WorkTask workTask = findById(workTaskId);


        if (WorkTaskStatus.WORKING.equals(workTask.getStatus())) {
            throw RequestValidationFailException.raiseException(
                    "someone is working on the work task " + workTask.getNumber());
        }

        workTask.setAssignedRole(null);

        return saveOrUpdate(workTask);

    }

    /**
     * Get the open and assignable work task that is
     * 1. released
     * 2. no one is working on the task
     * @param warehouseId
     * @return
     */
    public List<WorkTask> getOpenWorkTaskForAcknowledge(Long warehouseId) {
        List<WorkTask> workTasks = findAll(warehouseId,
                null,
                null,
                WorkTaskStatus.RELEASED.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        return workTasks.stream().filter(
                workTask -> Objects.isNull(workTask.getCurrentUser()) &&
                        Objects.isNull(workTask.getCompleteUser()) &&
                        Objects.isNull(workTask.getStartTime()) &&
                        Objects.isNull(workTask.getCompleteTime())
        ).sorted(Comparator.comparing(WorkTask::getPriority)).collect(Collectors.toList());
    }


    /**
     * Get the open and assignable work task that is
     * 1. released
     * 2. no one is working on the task
     * 3. can be processed by the user
     * @param warehouseId
     * @return
     */
    public List<WorkTask> getOpenWorkTaskForAcknowledge(Long warehouseId, User user) {
        logger.debug("we will get the works that already acknowledged by the user");
        List<WorkTask> workTasks = findAll(warehouseId,
                null,
                null,
                WorkTaskStatus.WORKING.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                user.getUsername(),
                null,
                null);
        if (!workTasks.isEmpty()) {
            logger.debug("get {} works that is already acknowledged by the user {}",
                    workTasks.size(),
                    user.getUsername());

            return workTasks.stream().sorted(Comparator.comparing(WorkTask::getPriority)).collect(Collectors.toList());
        }

        logger.debug("The user hasn't acknowledged anything yet, let's get all the released work");
        workTasks = findAll(warehouseId,
                null,
                null,
                WorkTaskStatus.RELEASED.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        logger.debug("get {} works that is in released status, let's see if they can be assigned to the user {}",
                workTasks.size(),
                user.getUsername());
        return workTasks.stream().filter(
                workTask -> {
                    boolean userCanAcknowledge = canAcknowledge(workTask, user);
                    logger.debug("user {} can acknowledge task {}? {}",
                            user.getUsername(),
                            workTask.getNumber(),
                            userCanAcknowledge);
                    return  userCanAcknowledge;
                }
        ).sorted(Comparator.comparing(WorkTask::getPriority)).collect(Collectors.toList());
    }

    /**
     * Get the next work task for the current user, with current location
     * @param warehouseId
     * @param currentLocationId
     * @return
     */
    public synchronized WorkTask getNextWorkTask(Long warehouseId, Long currentLocationId,
                                    String rfCode,
                                    Boolean acknowledge, String username) {
        Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseById(warehouseId);

        Location location = null;

        if (Objects.nonNull(currentLocationId)) {
            location = layoutServiceRestemplateClient.getLocationById(currentLocationId);
        }
        else if (Strings.isNotBlank(rfCode)) {
            // if the current location id is not passed in, but we have the RF code, then check where
            // is the current location
            RF rf = rfService.findByRFCode(warehouseId, rfCode);
            if (Objects.nonNull(rf) && Objects.nonNull(rf.getCurrentLocationId())) {
                location = layoutServiceRestemplateClient.getLocationById(
                        rf.getCurrentLocationId()
                );

            }
        }
        User user = null;
        if (Strings.isNotBlank(username)) {
            user = userService.findByUsername(warehouse.getCompanyId(), username, false);
        }
        else {
            user = userService.getCurrentUser(warehouse.getCompanyId());

        }
        logger.debug("start to get the best next work for user {}, from the current location {}",
                user.getUsername(),
                Objects.isNull(location) ? "N/A" : location.getName());
        WorkTask workTask = getNextWorkTask(warehouseId, user, location);

        if (Objects.nonNull(workTask) && Boolean.TRUE.equals(acknowledge)) {
            logger.debug("we will acknowledge the work task {} when return it to the user {}",
                    workTask.getNumber(),
                    user.getUsername());
            acknowledgeWorkTask(warehouse, workTask, rfCode, user);
        }
        return workTask;

    }

    /**
     * Check if teh user can acknowledge the work task
     * @param workTask
     * @param user
     * @return
     */
    public boolean canAcknowledge(WorkTask workTask, User user) {
        logger.debug("start to check if the user {} can acknowledge the work task {}",
                user.getUsername(),
                workTask.getNumber());
        if (WorkTaskStatus.WORKING.equals(workTask.getStatus())) {
            // if current work task is already acknowledged, then make sure
            // it is acknowledged by the current user
            logger.debug("current work task  {} is in working status and akkonwledged by {}" +
                    ", let's check if it is already  assign to the current user {}",
                    workTask.getNumber(),
                    workTask.getCurrentUser(),
                    user.getUsername());
            return user.equals(workTask.getCurrentUser());
        }
        return WorkTaskStatus.RELEASED.equals(workTask.getStatus()) &&
                Objects.isNull(workTask.getCurrentUser()) &&
                Objects.isNull(workTask.getCompleteUser()) &&
                Objects.isNull(workTask.getStartTime()) &&
                Objects.isNull(workTask.getCompleteTime()) &&
                // work task is not assigned yet, or assigned to the current user
                (Objects.isNull(workTask.getAssignedUser()) || workTask.getAssignedUser().equals(user)) &&
                // work task is not assigned yet, or assigned to the current role
                (Objects.isNull(workTask.getAssignedRole()) ||
                        user.getRoles().stream().anyMatch(role -> workTask.getAssignedRole().equals(role)));
    }
    public WorkTask getNextWorkTask(Long warehouseId, User user, Location currentLocation) {
        // first of all, get all the valid work tasks that can be assigned to the user
        List<WorkTask> workTasks = getOpenWorkTaskForAcknowledge(warehouseId, user);

        logger.debug("we get {} work tasks for the user {}",
                workTasks.size(),
                user.getUsername());


        // sort the work task by priority,
        // if both task has the same priority, then sort by distance between
        // the current location and the work's source location
        Collections.sort(
                workTasks, (a, b) -> {

                    // if the work task is assigned to the current user, then we will always return
                    // the work task first
                    if (Objects.equals(a.getAssignedUser(), user)) {
                        return -1;
                    }
                    else if (Objects.equals(b.getAssignedUser(),user)) {
                        return 1;
                    }


                    // if the work task already skipped, then it will be added
                    // to the back of the available works
                    if (!a.getSkipCount().equals(b.getSkipCount())) {
                        return a.getSkipCount().compareTo(b.getSkipCount());
                    }

                    // if priority is not the same, sort by priority
                    if (!a.getPriority().equals(b.getPriority())) {
                        return a.getPriority().compareTo(b.getPriority());
                    }
                    Long currentLocationSequenceA = getSequence(a.getType(), currentLocation);
                    Long currentLocationSequenceB = getSequence(b.getType(), currentLocation);
                    Long workALocationSequence = getSequence(a.getType(), a.getSourceLocation());
                    Long workBLocationSequence = getSequence(b.getType(), b.getSourceLocation());
                    if (Math.abs(currentLocationSequenceA - workALocationSequence) <
                                                Math.abs(currentLocationSequenceB - workBLocationSequence)) {
                        return -1;
                    }
                    else {
                        return 1;
                    }
        });

        if (Objects.isNull(workTasks) || workTasks.isEmpty()) {
            return null;
        }
        logger.debug("let's return first work task from the list, after sort by priority and proximity");
        return workTasks.get(0);

    }

    private Long getSequence(WorkTaskType workTaskType, Location location) {
        Long sequence = 0l;
        if (Objects.nonNull(location)) {
            switch (workTaskType) {
                case PICK:
                case LIST_PICK:
                case BULK_PICK:
                    sequence = location.getPickSequence();
                    break;
                case INVENTORY_MOVEMENT:
                case INVENTORY_MOVEMENT_CROSS_WAREHOUSE_OUT:
                case INVENTORY_MOVEMENT_CROSS_WAREHOUSE_IN:
                case PUT_AWAY:
                    sequence = location.getPutawaySequence();
                    break;
                default:
                    sequence = 0l;
            }
        }
        return sequence;

    }

    /**
     * Acknowledge the work task
     * @param warehouseId
     * @param id
     * @param rfCode
     * @return
     */
    public synchronized WorkTask acknowledgeWorkTask(Long warehouseId, Long id,
                                                     String rfCode, String username){
        WorkTask workTask = findById(id);
        Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseById(warehouseId);

        User user = null;
        if (Strings.isNotBlank(username)) {
            user = userService.findByUsername(warehouse.getCompanyId(),
                    username, false);
        }
        else {
            user = userService.getCurrentUser(warehouse.getCompanyId());
        }

        return acknowledgeWorkTask(warehouse, workTask, rfCode, user);
    }
    public synchronized WorkTask acknowledgeWorkTask(Warehouse warehouse, WorkTask workTask,
                                                     String rfCode, User user){

        if (!canAcknowledge(workTask, user)) {
            throw RequestValidationFailException.raiseException(
                    "current user " + user.getUsername() + " can't acknowledge the work task " +
                    workTask.getNumber());
        }

        // reset the RF's current location according to the work task's source location
        // as the user start to process the work task by the RF
        rfService.resetCurrentLocation(warehouse.getId(), rfCode, workTask.getSourceLocationId());

        workTask.setStatus(WorkTaskStatus.WORKING);
        workTask.setCurrentUser(user);
        workTask.setStartTime(ZonedDateTime.now());
        return saveOrUpdate(workTask);


    }


    public WorkTask resetWorkTaskStatus(Long warehouseId, Long id) {
        WorkTask workTask = findById(id);
        // if the work task's status is WORKING, then reset
        // it back to released and release from the current user
        if (WorkTaskStatus.WORKING.equals(workTask)) {
            workTask.setStatus(WorkTaskStatus.RELEASED);
            workTask.setCurrentUser(null);
            workTask.setStartTime(null);
        }
        return saveOrUpdate(workTask);
    }

    /**
     * Complete the work task
     * @param id
     * @param warehouseId
     * @return
     */
    public WorkTask completeWorkTask(Long id, Long warehouseId,
                                     String username) {
        if (Strings.isBlank(username)) {
            username = userService.getCurrentUserName();
        }
        User user = null;
        Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseById(warehouseId);

        if (Strings.isNotBlank(username)) {
            user = userService.findByUsername(warehouse.getCompanyId(), username, false);
        }
        WorkTask workTask = findById(id);
        workTask.setStatus(WorkTaskStatus.COMPLETE);
        workTask.setCompleteTime(ZonedDateTime.now());
        if (Objects.nonNull(user)) {
            workTask.setCompleteUser(user);
        }

        return saveOrUpdate(workTask);
    }

    public WorkTask unacknowledgeWorkTask(Long id, Boolean skip) {
        WorkTask workTask = findById(id);

        if (!workTask.getStatus().equals(WorkTaskStatus.COMPLETE)) {

            workTask.setStatus(WorkTaskStatus.RELEASED);
        }
        workTask.setCurrentUser(null);
        workTask.setStartTime(null);

        logger.debug("we will reset the work task's status to RELEASE");
        logger.debug("Skip the current work task {}? {}",
                workTask.getNumber(), skip);
        // if the user explicitly state that the user needs to skip the
        // current work task, then we will increase the skip count so that
        // the work task will be put back to the back end of the list
        if (Boolean.TRUE.equals(skip)) {

            logger.debug("Skip current work task and add the skip count by 1");
            workTask.setSkipCount(
                    Objects.isNull(workTask.getSkipCount()) ? 0 : workTask.getSkipCount()
                            + 1);
            logger.debug("Work task's new skip count is {}",
                    workTask.getSkipCount());
        }
        return saveOrUpdate(workTask);
    }
}

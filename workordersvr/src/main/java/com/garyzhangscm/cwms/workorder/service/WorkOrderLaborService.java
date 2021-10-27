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

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ProductionLineException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.MouldRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLaborRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderLaborService  {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLaborService.class);

    @Autowired
    private WorkOrderLaborRepository workOrderLaborRepository;
    @Autowired
    private WorkOrderLaborActivityHistoryService workOrderLaborActivityHistoryService;

    @Autowired
    private ProductionLineService productionLineService;

    public WorkOrderLabor findById(Long id) {
        return workOrderLaborRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order labor record not found by id: " + id));
    }



    public List<WorkOrderLabor> findAll(Long warehouseId,
                                        Long productionLineId,
                                        String workOrderLaborStatus,
                                        String username) {
        return workOrderLaborRepository.findAll(
                (Root<WorkOrderLabor> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(productionLineId)) {
                        Join<WorkOrderLabor, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }
                    if (Strings.isNotBlank(workOrderLaborStatus)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderLaborStatus"), WorkOrderLaborStatus.valueOf(workOrderLaborStatus)));

                    }
                    if (Strings.isNotBlank(username)) {
                        predicates.add(criteriaBuilder.equal(root.get("username"), username));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "productionLine")
        );

    }

    public WorkOrderLabor findByUsernameAndProductionLine(Long warehouseId, Long productionLineId, String username) {
        List<WorkOrderLabor> workOrderLabors = findAll(warehouseId, productionLineId, null, username);
        // for the combination of username and production line, we should only have at max one record
        // the user should either check in or check out of the production line
        if (workOrderLabors.size() > 0) {
            return workOrderLabors.get(0);
        }
        return  null;
    }

    public WorkOrderLabor save(WorkOrderLabor workOrderLabor) {
        return workOrderLaborRepository.save(workOrderLabor);
    }

    public WorkOrderLabor saveOrUpdate(WorkOrderLabor workOrderLabor) {
        if (workOrderLabor.getId() == null &&
                findByUsernameAndProductionLine(workOrderLabor.getWarehouseId(), workOrderLabor.getProductionLine().getId(), workOrderLabor.getUsername()) != null) {
            workOrderLabor.setId(
                    findByUsernameAndProductionLine(workOrderLabor.getWarehouseId(), workOrderLabor.getProductionLine().getId(), workOrderLabor.getUsername()).getId());
        }
        return save(workOrderLabor);
    }


    public void delete(WorkOrderLabor workOrderLabor) {
        workOrderLaborRepository.delete(workOrderLabor);
    }

    public void delete(Long id) {
        workOrderLaborRepository.deleteById(id);
    }


    /**
     * Check in the user into the production line
     * @param warehouseId warehouse id
     * @param productionLineId production line id
     * @param username the user who was checked in
     * @param currentUsername the user who make the check in activity. This may be a supervisor or the user self
     * @return
     */
    public WorkOrderLabor checkInUser(Long warehouseId, Long productionLineId, String username, String currentUsername) {
        // get the status of the user in the production line,
        // if there's no record, then create a record for check in
        // if the user is already checked in , then throw error message
        // if the user is already checked out, then update the check in time
        // and save a history

        WorkOrderLabor workOrderLabor = findByUsernameAndProductionLine(
                warehouseId, productionLineId, username
        );
        if (Objects.nonNull(workOrderLabor)) {
            if (workOrderLabor.getWorkOrderLaborStatus().equals(WorkOrderLaborStatus.CHECK_IN)) {
                ProductionLine productionLine = productionLineService.findById(productionLineId);
                throw ProductionLineException.raiseException("The user " + username + " is already check in this production line " + productionLine.getName());
            }
            else {
                String originalValue = Objects.isNull(workOrderLabor.getLastCheckInTime())?
                    "" : workOrderLabor.getLastCheckInTime().toString();
                workOrderLabor.setWorkOrderLaborStatus(WorkOrderLaborStatus.CHECK_IN);
                workOrderLabor.setLastCheckInTime(LocalDateTime.now());

                workOrderLabor = saveOrUpdate(workOrderLabor);

                workOrderLaborActivityHistoryService.checkInUser(
                        warehouseId, workOrderLabor, originalValue, currentUsername
                );
                return workOrderLabor;
            }
        }
        else {
            ProductionLine productionLine = productionLineService.findById(productionLineId);
            workOrderLabor = new WorkOrderLabor(warehouseId, username, productionLine,
                    LocalDateTime.now(), null, WorkOrderLaborStatus.CHECK_IN);

            workOrderLabor = saveOrUpdate(workOrderLabor);

            workOrderLaborActivityHistoryService.checkInUser(
                    warehouseId, workOrderLabor, "", currentUsername
            );
            return workOrderLabor;
        }
    }


    /**
     * Check out the user from the production line
     * @param warehouseId warehouse id
     * @param productionLineId production line id
     * @param username the user who was checked out
     * @param currentUsername the user who make the check out activity. This may be a supervisor or the user self
     * @return
     */
    public WorkOrderLabor checkOutUser(Long warehouseId, Long productionLineId, String username, String currentUsername) {
        // get the status of the user in the production line,
        // if there's no record, then raise error says the user doesn't check in yet
        // if the user is already checked in , then update the check out time
        // if the user is already checked out, then raise error says the user is already check out

        WorkOrderLabor workOrderLabor = findByUsernameAndProductionLine(
                warehouseId, productionLineId, username
        );
        if (Objects.nonNull(workOrderLabor)) {
            if (workOrderLabor.getWorkOrderLaborStatus().equals(WorkOrderLaborStatus.CHECK_IN)) {
                String originalValue =
                        workOrderLabor.getLastCheckOutTime() == null ? "" : workOrderLabor.getLastCheckOutTime().toString();

                workOrderLabor.setWorkOrderLaborStatus(WorkOrderLaborStatus.CHECK_OUT);
                workOrderLabor.setLastCheckOutTime(LocalDateTime.now());

                workOrderLabor = saveOrUpdate(workOrderLabor);

                workOrderLaborActivityHistoryService.checkOutUser(
                        warehouseId, workOrderLabor, originalValue, currentUsername
                );
                return workOrderLabor;
            }
            else {
                ProductionLine productionLine = productionLineService.findById(productionLineId);
                throw ProductionLineException.raiseException("The user " + username + " already check out this production line " + productionLine.getName());
            }
        }
        else {
            ProductionLine productionLine = productionLineService.findById(productionLineId);
            throw ProductionLineException.raiseException("The user " + username + " has not check in this production line " + productionLine.getName() + " yet");
        }
    }

    public WorkOrderLabor addLabor(WorkOrderLabor workOrderLabor, String currentUsername) {

        String originalValue = workOrderLabor.getLastCheckInTime().toString() + "|" + workOrderLabor.getLastCheckOutTime().toString();

        workOrderLabor = saveOrUpdate(workOrderLabor);

        workOrderLaborActivityHistoryService.addLabor(
                workOrderLabor.getWarehouseId(), workOrderLabor, originalValue, currentUsername
        );
        return workOrderLabor;

    }

    public WorkOrderLabor changeLabor(Long id, WorkOrderLabor workOrderLabor, String currentUsername) {
        String originalValue = workOrderLabor.getLastCheckInTime().toString() + "|" + workOrderLabor.getLastCheckOutTime().toString();
        workOrderLabor = saveOrUpdate(workOrderLabor);

        workOrderLaborActivityHistoryService.changeLabor(
                workOrderLabor.getWarehouseId(), workOrderLabor, originalValue, currentUsername
        );
        return workOrderLabor;
    }
}

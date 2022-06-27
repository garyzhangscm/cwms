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

import com.garyzhangscm.cwms.workorder.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.WorkOrderInstruction;
import com.garyzhangscm.cwms.workorder.model.WorkOrderLine;
import com.garyzhangscm.cwms.workorder.model.WorkOrderLineSparePart;
import com.garyzhangscm.cwms.workorder.model.WorkOrderLineSparePartDetail;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineSparePartDetailRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineSparePartRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderLineSparePartDetailService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLineSparePartDetailService.class);

    @Autowired
    private WorkOrderLineSparePartDetailRepository workOrderLineSparePartDetailRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;



    public WorkOrderLineSparePartDetail findById(Long id) {
        return workOrderLineSparePartDetailRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order line spare part detail not found by id: " + id));
    }


    public List<WorkOrderLineSparePartDetail> findAll(Long workOrderLineId, String name,
                                                Long itemId, Long clientId,
                                                boolean loadDetails) {

        if (Objects.isNull(workOrderLineId)) {
            throw WorkOrderException.raiseException("work order line ID is required in order to get the spare part");
        }

        List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails =  workOrderLineSparePartDetailRepository.findAll(
                (Root<WorkOrderLineSparePartDetail> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    Join<WorkOrderLineSparePartDetail, WorkOrderLineSparePart> joinWorkOrderLineSparePart
                            = root.join("workOrderLineSparePart", JoinType.INNER);
                    Join<WorkOrderLineSparePart, WorkOrderLine> joinWorkOrderLine =
                            joinWorkOrderLineSparePart.join("workOrderLine", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWorkOrderLine.get("id"), workOrderLineId));

                    if (Strings.isNotBlank(name)) {
                        if (name.contains("%")) {
                            predicates.add(criteriaBuilder.like(joinWorkOrderLineSparePart.get("name"), name));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinWorkOrderLineSparePart.get("name"), name));
                        }
                    }

                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));

                    }
                    if (Objects.nonNull(itemId)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));

                    }



                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (workOrderLineSparePartDetails.size() > 0 && loadDetails) {
            loadAttribute(workOrderLineSparePartDetails);
        }
        return workOrderLineSparePartDetails;

    }

    public void loadAttribute(List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails) {
        workOrderLineSparePartDetails.forEach(this::loadAttribute);
    }

    public void loadAttribute(WorkOrderLineSparePartDetail workOrderLineSparePartDetail) {
        if (Objects.nonNull(workOrderLineSparePartDetail.getClientId()) &&
                Objects.nonNull(workOrderLineSparePartDetail.getClient())) {

            workOrderLineSparePartDetail.setClient(
                    commonServiceRestemplateClient.getClientById(
                            workOrderLineSparePartDetail.getClientId()
                    )
            );
        }

        if (Objects.nonNull(workOrderLineSparePartDetail.getItemId()) &&
                Objects.nonNull(workOrderLineSparePartDetail.getItem())) {

            workOrderLineSparePartDetail.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            workOrderLineSparePartDetail.getItemId()
                    )
            );
        }

        if (Objects.nonNull(workOrderLineSparePartDetail.getInventoryStatusId()) &&
                Objects.nonNull(workOrderLineSparePartDetail.getInventoryStatus())) {

            workOrderLineSparePartDetail.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            workOrderLineSparePartDetail.getInventoryStatusId()
                    )
            );
        }
    }

    public WorkOrderLineSparePartDetail save(WorkOrderLineSparePartDetail workOrderLineSparePartDetail) {
        return workOrderLineSparePartDetailRepository.save(workOrderLineSparePartDetail);

    }

    public WorkOrderLineSparePartDetail findByItem(Long workOrderLineId, String name, Long itemId, Long clientId) {
        if (Objects.isNull(workOrderLineId)) {
            throw new IllegalArgumentException("work order line id is required");
        }
        if (Strings.isBlank(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (Objects.isNull(itemId)) {
            throw new IllegalArgumentException("item id is required");
        }
        // there should be at max 1 record if we have the above 3 parameters passed in
        List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails = findAll(
                workOrderLineId, name, itemId, clientId, false
        );

        if (workOrderLineSparePartDetails.isEmpty()) {
            return null;
        }
        return workOrderLineSparePartDetails.get(0);
    }

    public WorkOrderLineSparePartDetail saveOrUpdate(WorkOrderLineSparePartDetail workOrderLineSparePartDetail) {


        if (workOrderLineSparePartDetail.getId() == null &&
                Objects.nonNull(workOrderLineSparePartDetail.getWorkOrderLineSparePart())
                && findByItem(workOrderLineSparePartDetail.getWorkOrderLineSparePart().getWorkOrderLine().getId(),
                                workOrderLineSparePartDetail.getWorkOrderLineSparePart().getName(),
                                workOrderLineSparePartDetail.getItemId(),
                                workOrderLineSparePartDetail.getClientId()) != null) {
            workOrderLineSparePartDetail.setId(
                    findByItem(workOrderLineSparePartDetail.getWorkOrderLineSparePart().getWorkOrderLine().getId(),
                            workOrderLineSparePartDetail.getWorkOrderLineSparePart().getName(),
                            workOrderLineSparePartDetail.getItemId(),
                            workOrderLineSparePartDetail.getClientId()).getId());
        }
        return save(workOrderLineSparePartDetail);
    }


    public void delete(WorkOrderLineSparePartDetail workOrderLineSparePartDetail) {
        workOrderLineSparePartDetailRepository.delete(workOrderLineSparePartDetail);
    }

    public void delete(Long id) {
        workOrderLineSparePartDetailRepository.deleteById(id);
    }



}

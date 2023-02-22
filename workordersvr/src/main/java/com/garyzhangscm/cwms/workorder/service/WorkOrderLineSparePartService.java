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
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderInstructionRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineSparePartRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderLineSparePartService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLineSparePartService.class);

    @Autowired
    private WorkOrderLineSparePartRepository workOrderLineSparePartRepository;

    @Autowired
    private WorkOrderLineSparePartDetailService workOrderLineSparePartDetailService;

    @Autowired
    private WorkOrderLineService workOrderLineService;



    public WorkOrderLineSparePart findById(Long id) {
        return workOrderLineSparePartRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order line spare part not found by id: " + id));
    }


    public List<WorkOrderLineSparePart> findAll(Long workOrderLineId, String name) {
        return findAll(workOrderLineId, name, true);
    }
    public List<WorkOrderLineSparePart> findAll(Long workOrderLineId, String name,
                                                boolean loadDetails) {

        if (Objects.isNull(workOrderLineId)) {
            throw WorkOrderException.raiseException("work order line ID is required in order to get the spare part");
        }

        List<WorkOrderLineSparePart> workOrderLineSpareParts =  workOrderLineSparePartRepository.findAll(
                (Root<WorkOrderLineSparePart> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    Join<WorkOrderLineSparePart, WorkOrderLine> joinWorkOrderLine = root.join("workOrderLine", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWorkOrderLine.get("id"), workOrderLineId));

                    if (Strings.isNotBlank(name)) {
                        if (name.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "name")
        );

        if (workOrderLineSpareParts.size() > 0 && loadDetails) {
            loadAttribute(workOrderLineSpareParts);
        }
        return workOrderLineSpareParts;

    }

    public void loadAttribute(List<WorkOrderLineSparePart> workOrderLineSpareParts) {
        workOrderLineSpareParts.forEach(this::loadAttribute);
    }

    public void loadAttribute(WorkOrderLineSparePart workOrderLineSparePart) {
        workOrderLineSparePartDetailService.loadAttribute(workOrderLineSparePart.getWorkOrderLineSparePartDetails());
    }

    private WorkOrderLineSparePart findByName(Long workOrderLineId, String name) {


        return findByName(workOrderLineId, name, true);
    }

    private WorkOrderLineSparePart findByName(Long workOrderLineId, String name, boolean loadDetails) {

        if (Objects.isNull(workOrderLineId)) {
            throw new IllegalArgumentException("work order line id is required");
        }
        if (Strings.isBlank(name)) {
            throw new IllegalArgumentException("name is required");
        }


        // there should be at max 1 record if we have the above 2 parameters passed in
        List<WorkOrderLineSparePart> workOrderLineSpareParts = findAll(
                workOrderLineId, name, loadDetails
        );

        if (workOrderLineSpareParts.isEmpty()) {
            return null;
        }
        return workOrderLineSpareParts.get(0);

    }

    public WorkOrderLineSparePart save(WorkOrderLineSparePart workOrderLineSparePart) {
        return workOrderLineSparePartRepository.save(workOrderLineSparePart);

    }

    public WorkOrderLineSparePart saveOrUpdate(WorkOrderLineSparePart workOrderLineSparePart) {

        if (workOrderLineSparePart.getId() == null
                && findByName(workOrderLineSparePart.getWorkOrderLine().getId(),
                              workOrderLineSparePart.getName()) != null) {
            workOrderLineSparePart.setId(
                    findByName(workOrderLineSparePart.getWorkOrderLine().getId(),
                            workOrderLineSparePart.getName()) .getId());
        }
        return save(workOrderLineSparePart);

    }


    public void delete(WorkOrderLineSparePart workOrderLineSparePart) {
        workOrderLineSparePartRepository.delete(workOrderLineSparePart);
    }

    public void delete(Long id) {
        logger.debug("workOrderLineSparePartRepository.deleteById(id): {}",
                id);
        workOrderLineSparePartRepository.deleteById(id);
    }


    // refresh the inprocess quantity based on the inprocess quantity
    // from the details and the ratio
    public void refreshInprocessQuantity(WorkOrderLineSparePart workOrderLineSparePart) {
        logger.debug("refresh the in process quantity for work order line {} - {}'s spare part ",
                workOrderLineSparePart.getWorkOrderLine().getWorkOrder().getNumber(),
                workOrderLineSparePart.getWorkOrderLine().getNumber());
        long oldInprocessQuantity = workOrderLineSparePart.getInprocessQuantity();
        long newInprocessQuantity = Long.MAX_VALUE;

        for (WorkOrderLineSparePartDetail workOrderLineSparePartDetail : workOrderLineSparePart.getWorkOrderLineSparePartDetails()) {
            double ratio = workOrderLineSparePart.getQuantity() * 1.0 / workOrderLineSparePartDetail.getQuantity();
            logger.debug("based on spare part detail {}, ratio is {}",
                    workOrderLineSparePartDetail.getItemId(), ratio);
            // we will get the mininum in process quantity from the details, based on the ratio
            newInprocessQuantity = (long)Math.min(workOrderLineSparePartDetail.getInprocessQuantity() * ratio, newInprocessQuantity);
            logger.debug("> new in process quantity should be {}", newInprocessQuantity);
        }
        workOrderLineSparePart.setInprocessQuantity(newInprocessQuantity);


        logger.debug("====> refresh in process quantity to {}", newInprocessQuantity);
        saveOrUpdate(workOrderLineSparePart);

        // we will refresh work order line's quantity as well
        logger.debug("====> refresh WORK ORDER LINE's spare part quantity by adding quantity {}",
                (newInprocessQuantity - oldInprocessQuantity));
        workOrderLineService.refreshSparePartQuantity(workOrderLineSparePart.getWorkOrderLine(), newInprocessQuantity - oldInprocessQuantity);



    }
}

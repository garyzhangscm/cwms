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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.model.Order;
import com.garyzhangscm.cwms.workorder.repository.MasterProductionScheduleLineDateRepository;
import com.garyzhangscm.cwms.workorder.repository.MasterProductionScheduleRepository;
import com.garyzhangscm.cwms.workorder.repository.MouldRepository;
import org.apache.commons.lang.StringUtils;
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
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class MasterProductionScheduleService   {
    private static final Logger logger = LoggerFactory.getLogger(MasterProductionScheduleService.class);

    @Autowired
    private MasterProductionScheduleRepository masterProductionScheduleRepository;
    @Autowired
    private MasterProductionScheduleLineDateRepository masterProductionScheduleLineDateRepository;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public MasterProductionSchedule findById(Long id) {
        return findById(id, true);
    }

    public MasterProductionSchedule findById(Long id, boolean loadDetails) {
        MasterProductionSchedule masterProductionSchedule =
                masterProductionScheduleRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("MPS not found by id: " + id));
        if (Objects.nonNull(masterProductionSchedule) && loadDetails) {
            loadAttributes(masterProductionSchedule);
        }

        return masterProductionSchedule;
    }

    private void loadAttributes(List<MasterProductionSchedule> masterProductionSchedules) {
        masterProductionSchedules.forEach(
                masterProductionSchedule -> loadAttributes(masterProductionSchedule)
        );
    }
    private void loadAttributes(MasterProductionSchedule masterProductionSchedule) {
        if (Objects.nonNull(masterProductionSchedule.getItemId())) {

            masterProductionSchedule.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            masterProductionSchedule.getItemId()
                    )
            );
        }
    }


    public List<MasterProductionSchedule> findAll(Long warehouseId, String number, String description) {
        return findAll(warehouseId, number, description, true);
    }
    public List<MasterProductionSchedule> findAll(Long warehouseId, String number, String description, boolean loadDetails) {
        List<MasterProductionSchedule> masterProductionSchedules =
                masterProductionScheduleRepository.findAll(
                (Root<MasterProductionSchedule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));

                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }

                    }

                    if (StringUtils.isNotBlank(description)) {
                        predicates.add(criteriaBuilder.like(root.get("description"), description));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "number")
        );

        if (masterProductionSchedules.size() > 0 && loadDetails) {
            loadAttributes(masterProductionSchedules);
        }

        return masterProductionSchedules;

    }


    public List<MasterProductionScheduleLineDate> findAllMasterProductionScheduleLineDate(
            Long warehouseId, LocalDateTime beginDateTime, LocalDateTime endDateTime, Long productionLineId) {

        return masterProductionScheduleLineDateRepository.findAll(
                (Root<MasterProductionScheduleLineDate> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("plannedDate"), beginDateTime));
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("plannedDate"), endDateTime));

                    if (Objects.nonNull(productionLineId)) {

                        Join<MasterProductionScheduleLineDate, MasterProductionScheduleLine> joinMasterProductionScheduleLine
                                = root.join("masterProductionScheduleLine", JoinType.INNER);
                        Join<MasterProductionScheduleLine, ProductionLine> joinProductionLine
                                = joinMasterProductionScheduleLine.join("productionLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "plannedDate")
        );

    }

    public MasterProductionSchedule findByNumber(Long warehouseId, String number) {

        return findByNumber(warehouseId, number, true);
    }

    public MasterProductionSchedule findByNumber(Long warehouseId, String number, boolean loadDetails) {

        MasterProductionSchedule masterProductionSchedule =
                masterProductionScheduleRepository.findByWarehouseIdAndNumber(warehouseId, number);

        if (Objects.nonNull(masterProductionSchedule) && loadDetails) {
            loadAttributes(masterProductionSchedule);
        }
        return masterProductionSchedule;
    }

    public MasterProductionSchedule save(MasterProductionSchedule masterProductionSchedule) {
        return masterProductionScheduleRepository.save(masterProductionSchedule);
    }

    public MasterProductionSchedule saveOrUpdate(MasterProductionSchedule masterProductionSchedule) {
        if (masterProductionSchedule.getId() == null &&
                findByNumber(masterProductionSchedule.getWarehouseId(), masterProductionSchedule.getNumber()) != null) {
            masterProductionSchedule.setId(
                    findByNumber(masterProductionSchedule.getWarehouseId(), masterProductionSchedule.getNumber()).getId());
        }
        return save(masterProductionSchedule);
    }


    public void delete(MasterProductionSchedule masterProductionSchedule) {
        masterProductionScheduleRepository.delete(masterProductionSchedule);
    }

    public void delete(Long id) {
        masterProductionScheduleRepository.deleteById(id);
    }


    public MasterProductionSchedule addMasterProductionSchedule(MasterProductionSchedule masterProductionSchedule) {
        masterProductionSchedule.getMasterProductionScheduleLines().forEach(
                masterProductionScheduleLine -> {
                    masterProductionScheduleLine.setMasterProductionSchedule(masterProductionSchedule);
                    masterProductionScheduleLine.getMasterProductionScheduleLineDates().forEach(
                            masterProductionScheduleLineDate ->
                                    masterProductionScheduleLineDate.setMasterProductionScheduleLine(
                                            masterProductionScheduleLine
                                    )
                    );
                }
        );
        return saveOrUpdate(masterProductionSchedule);
    }

    public MasterProductionSchedule changeMasterProductionSchedule(Long id, MasterProductionSchedule masterProductionSchedule) {
        masterProductionSchedule.getMasterProductionScheduleLines().forEach(
                masterProductionScheduleLine -> {
                    masterProductionScheduleLine.setMasterProductionSchedule(masterProductionSchedule);
                    masterProductionScheduleLine.getMasterProductionScheduleLineDates().forEach(
                            masterProductionScheduleLineDate ->
                                    masterProductionScheduleLineDate.setMasterProductionScheduleLine(
                                            masterProductionScheduleLine
                                    )
                    );
                }
        );
        return saveOrUpdate(masterProductionSchedule);
    }

    public Set<LocalDateTime> getAvailableDate(Long warehouseId, Long productionLineId, String beginDateTime, String endDateTime) {


        List<MasterProductionScheduleLineDate> masterProductionScheduleLineDates =
                findAllMasterProductionScheduleLineDate(
                        warehouseId,
                        LocalDateTime.parse(beginDateTime),
                        LocalDateTime.parse(endDateTime),
                        productionLineId);
        return masterProductionScheduleLineDates.stream().map(MasterProductionScheduleLineDate::getPlannedDate).collect(Collectors.toSet());
    }
}

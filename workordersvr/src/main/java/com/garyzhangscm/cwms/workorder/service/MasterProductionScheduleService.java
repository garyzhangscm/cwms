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

import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.MasterProductionScheduleLineDateRepository;
import com.garyzhangscm.cwms.workorder.repository.MasterProductionScheduleRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;


@Service
public class MasterProductionScheduleService   {
    private static final Logger logger = LoggerFactory.getLogger(MasterProductionScheduleService.class);

    @Autowired
    private MasterProductionScheduleRepository masterProductionScheduleRepository;
    @Autowired
    private MasterProductionScheduleLineDateRepository masterProductionScheduleLineDateRepository;


    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        masterProductionScheduleRepository.processItemOverride(
                warehouseId, oldItemId, newItemId
        );
    }

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

            try {
                masterProductionSchedule.setItem(
                        inventoryServiceRestemplateClient.getItemById(
                                masterProductionSchedule.getItemId()
                        )
                );
            }
            catch (Exception ex) {}
        }
    }






    public List<MasterProductionSchedule> findAll(Long warehouseId, String number, String description,
                                                  String beginDateTime, String endDateTime,
                                                  Long productionLineId, String productionLineIds,
                                                  String itemName) {
        return findAll(warehouseId, number, description, beginDateTime, endDateTime, productionLineId, productionLineIds,
                itemName, true);
    }
    public List<MasterProductionSchedule> findAll(Long warehouseId, String number, String description,
                                                  String beginDateTime, String endDateTime,
                                                  Long productionLineId, String productionLineIds,
                                                  String itemName,
                                                  boolean loadDetails) {
        List<MasterProductionSchedule> masterProductionSchedules =
                masterProductionScheduleRepository.findAll(
                (Root<MasterProductionSchedule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    criteriaQuery.distinct(true);

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));

                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }

                    }
                    if(Objects.nonNull(productionLineId)) {

                        Join<MasterProductionSchedule, MasterProductionScheduleLine> joinMasterProductionScheduleLine
                                = root.join("masterProductionScheduleLines", JoinType.INNER);

                        Join<MasterProductionScheduleLine, ProductionLine> joinProductionLine
                                = joinMasterProductionScheduleLine.join("productionLine", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }
                    if (Strings.isNotBlank(productionLineIds)) {

                        Join<MasterProductionSchedule, MasterProductionScheduleLine> joinMasterProductionScheduleLine
                                = root.join("masterProductionScheduleLines", JoinType.INNER);

                        Join<MasterProductionScheduleLine, ProductionLine> joinProductionLine
                                = joinMasterProductionScheduleLine.join("productionLine", JoinType.INNER);

                        CriteriaBuilder.In<Long> inProductionLineIds
                                = criteriaBuilder.in(joinProductionLine.get("id"));
                        for(String id : productionLineIds.split(",")) {
                            inProductionLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inProductionLineIds));
                    }
                    if (Strings.isNotBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);

                        predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                    }

                    if (Strings.isNotBlank(beginDateTime) || Strings.isNotBlank(endDateTime)) {

                        Join<MasterProductionSchedule, MasterProductionScheduleLine> joinMasterProductionScheduleLine
                                = root.join("masterProductionScheduleLines", JoinType.INNER);
                        Join<MasterProductionScheduleLine, MasterProductionScheduleLineDate> joinMasterProductionScheduleLineDate
                                = joinMasterProductionScheduleLine.join("masterProductionScheduleLineDates", JoinType.INNER);


                        if (Strings.isNotBlank(beginDateTime)) {

                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                    joinMasterProductionScheduleLineDate.get("plannedDate"),
                                    LocalDateTime.parse(beginDateTime)));
                        }
                        if (Strings.isNotBlank(endDateTime)) {

                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                    joinMasterProductionScheduleLineDate.get("plannedDate"),
                                    LocalDateTime.parse(endDateTime)));
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

        logger.debug("find {} result", masterProductionSchedules.size());
        return masterProductionSchedules;

    }


    public List<MasterProductionScheduleLineDate> findAllMasterProductionScheduleLineDate(
            Long warehouseId, LocalDateTime beginDateTime, LocalDateTime endDateTime, Long productionLineId) {

        return masterProductionScheduleLineDateRepository.findAll(
                (Root<MasterProductionScheduleLineDate> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    Join<MasterProductionScheduleLineDate, MasterProductionScheduleLine> joinMasterProductionScheduleLine
                            = root.join("masterProductionScheduleLine", JoinType.INNER);
                    Join<MasterProductionScheduleLine, MasterProductionSchedule> joinMasterProductionSchedule
                            = joinMasterProductionScheduleLine.join("masterProductionSchedule", JoinType.INNER);

                    predicates.add(criteriaBuilder.equal(joinMasterProductionSchedule.get("warehouseId"), warehouseId));

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("plannedDate"), beginDateTime));
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("plannedDate"), endDateTime));

                    if (Objects.nonNull(productionLineId)) {

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


    public void delete(MasterProductionSchedule masterProductionSchedule, Boolean moveSuccessor) {
        // see if we will need to move the successor MPS back wards after we remove the current MPS

        logger.debug("do we need to move successor after the change? {}", moveSuccessor);

        if (Boolean.TRUE.equals(moveSuccessor)) {
            masterProductionSchedule.getMasterProductionScheduleLines().forEach(
                    masterProductionScheduleLine -> {
                        long productionLineId = masterProductionScheduleLine.getProductionLine().getId();
                        long movedDays =
                                getLastDateDifference(masterProductionSchedule, null, productionLineId) - 1;
                        logger.debug("> we may need move {} days for the successor MPS after we chaged the currnet MPS {} on production line {}",
                                movedDays, masterProductionSchedule.getNumber(),
                                masterProductionScheduleLine.getProductionLine().getName());
                        if (movedDays != 0) {
                            moveSuccessingMPS(masterProductionSchedule, productionLineId, movedDays);
                        }
                    }
            );
        }
        masterProductionScheduleRepository.delete(masterProductionSchedule);
    }

    public void delete(Long id, Boolean moveSuccessor) {

        delete(findById(id), moveSuccessor);

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

    @Transactional
    public MasterProductionSchedule changeMasterProductionSchedule(Long id,
                                                                   MasterProductionSchedule masterProductionSchedule,
                                                                   Boolean moveSuccessor) {
        MasterProductionSchedule originalMasterProductionSchedule = findById(id);

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
        // see if the last date of the MPS is changed, if so, we may need to move the successor
        // forward or backward accordingly
        logger.debug("do we need to move successor after the change? {}", moveSuccessor);
        if (Boolean.TRUE.equals(moveSuccessor)) {
            originalMasterProductionSchedule.getMasterProductionScheduleLines().forEach(
                    masterProductionScheduleLine -> {
                        long productionLineId = masterProductionScheduleLine.getProductionLine().getId();
                        long movedDays =
                                getLastDateDifference(originalMasterProductionSchedule, masterProductionSchedule, productionLineId);
                        logger.debug("> we may need move {} days for the successor MPS after we chaged the currnet MPS {} on production line {}",
                                movedDays, masterProductionSchedule.getNumber(),
                                masterProductionScheduleLine.getProductionLine().getName());
                        if (movedDays != 0) {
                            moveSuccessingMPS(originalMasterProductionSchedule, productionLineId, movedDays);
                        }
                    }
            );
        }
        return saveOrUpdate(masterProductionSchedule);
    }

    @Transactional
    private void moveSuccessingMPS(MasterProductionSchedule originalMasterProductionSchedule,
                                   long productionLineId, long movedDays) {
        // let's find all the successing MPS that on the same production and have
        // date that later than the current MPS's last date

        LocalDateTime originalMPSLastDay = getLastPlannedDate(originalMasterProductionSchedule, productionLineId);
        List<MasterProductionSchedule> allMPSNeedsMove = findAll(
                originalMasterProductionSchedule.getWarehouseId(),
                null,
                null,
                originalMPSLastDay.toLocalDate().plusDays(1).atStartOfDay().toString(),  // only return MPS that is after the last day of the original MPS
                null,
                productionLineId,
                null, null, false
        );
        allMPSNeedsMove.forEach(
                masterProductionSchedule ->
                        moveMPSByDays(masterProductionSchedule, productionLineId, originalMPSLastDay.plusDays(1), movedDays)
        );
    }

    @Transactional
    private void moveMPSByDays(MasterProductionSchedule masterProductionSchedule,
                               long productionLineId,
                               LocalDateTime startDate,
                               long movedDays) {
        masterProductionSchedule.getMasterProductionScheduleLines().stream().filter(
                masterProductionScheduleLine -> masterProductionScheduleLine.getProductionLine().getId().equals(productionLineId)
        ).forEach(
                masterProductionScheduleLine -> {
                    logger.debug("Start to move MPS {}, on production line {}, by {} days, for any date on or after {}",
                            masterProductionSchedule.getNumber(),
                            masterProductionScheduleLine.getProductionLine().getName(),
                            movedDays, startDate);
                    masterProductionScheduleLine.getMasterProductionScheduleLineDates().forEach(
                            masterProductionScheduleLineDate -> {
                                if (!masterProductionScheduleLineDate.getPlannedDate().toLocalDate().isBefore(
                                        startDate.toLocalDate()
                                )) {
                                    // the planned date is not before the start date, let's move the date
                                    logger.debug("start to move date from {}", masterProductionScheduleLineDate.getPlannedDate().toLocalDate());
                                    masterProductionScheduleLineDate.setPlannedDate(
                                            masterProductionScheduleLineDate.getPlannedDate().toLocalDate().plusDays(movedDays).atStartOfDay()
                                    );
                                    logger.debug("==> to {}", masterProductionScheduleLineDate.getPlannedDate().toLocalDate());
                                }
                            }
                    );
                }
        );
        saveOrUpdate(masterProductionSchedule);
    }

    private long getLastDateDifference(MasterProductionSchedule originalMasterProductionSchedule,
                                      MasterProductionSchedule masterProductionSchedule,
                                       Long productionLineId) {

        LocalDateTime originalMPSLastDay = getLastPlannedDate(originalMasterProductionSchedule, productionLineId);
        LocalDateTime newMPSLastDay = getLastPlannedDate(masterProductionSchedule, productionLineId);

        logger.debug("0. originalMPSLastDay: {}", Objects.isNull(originalMPSLastDay) ? "N/A" : originalMPSLastDay);
        logger.debug("0. newMPSLastDay: {}", Objects.isNull(newMPSLastDay) ? "N/A" : newMPSLastDay);
        if (Objects.isNull(originalMPSLastDay) && Objects.isNull(newMPSLastDay)) {
            // the last day in both MPS are empty, which should not happen
            return 0;
        }
        else if (Objects.isNull(originalMPSLastDay) && Objects.nonNull(newMPSLastDay)) {
            // the original MPS is empty but we have new MPS, then we are adding MPS and
            // we will use the new MPS' total days as the difference in days so that
            // we can move the successor
            LocalDateTime newMPSFirstDay = getFirstPlannedDate(masterProductionSchedule, productionLineId);

            logger.debug("1. newMPSFirstDay: {}", Objects.isNull(newMPSFirstDay) ? "N/A" : newMPSFirstDay);
            logger.debug("1. DAYS.between(newMPSFirstDay.toLocalDate(), newMPSLastDay.toLocalDate()): {}",
                    DAYS.between(newMPSFirstDay.toLocalDate(), newMPSLastDay.toLocalDate()));

            return DAYS.between(newMPSFirstDay.toLocalDate(), newMPSLastDay.toLocalDate());

        }
        else if (Objects.nonNull(originalMPSLastDay) && Objects.isNull(newMPSLastDay)) {
            // the new MPS is empty but we have original MPS, then we are removing MPS and
            // we will use the original MPS' total days as the difference in days so that
            // we can move the successor BACKWARDS
            LocalDateTime originalMPSFirstDay = getFirstPlannedDate(originalMasterProductionSchedule, productionLineId);
            logger.debug("2. originalMPSFirstDay: {}", Objects.isNull(originalMPSFirstDay) ? "N/A" : originalMPSFirstDay);
            logger.debug("2. DAYS.between(originalMPSLastDay.toLocalDate(), originalMPSFirstDay.toLocalDate()): {}",
                    DAYS.between(originalMPSLastDay.toLocalDate(), originalMPSFirstDay.toLocalDate()));
            return DAYS.between(originalMPSLastDay.toLocalDate(), originalMPSFirstDay.toLocalDate());

        }
        else {
            // we have both the original MPS and new MPS. so we are changing existing MPS
            // see if we need to move the successor
            logger.debug("3. DAYS.between(originalMPSLastDay.toLocalDate(), newMPSLastDay.toLocalDate(): {}",
                    DAYS.between(originalMPSLastDay.toLocalDate(), newMPSLastDay.toLocalDate()));
            return DAYS.between(originalMPSLastDay.toLocalDate(), newMPSLastDay.toLocalDate());
        }
    }

    private LocalDateTime getLastPlannedDate(MasterProductionSchedule masterProductionSchedule,
                                             Long productionLineId) {

        if (Objects.isNull(masterProductionSchedule)) {
            return null;
        }
        LocalDateTime mpsLastDay = null;

        for (MasterProductionScheduleLine masterProductionScheduleLine : masterProductionSchedule.getMasterProductionScheduleLines()) {
            if (masterProductionScheduleLine.getProductionLine().getId().equals(productionLineId)) {

                for (MasterProductionScheduleLineDate masterProductionScheduleLineDate : masterProductionScheduleLine.getMasterProductionScheduleLineDates()) {
                    if (Objects.isNull(mpsLastDay) ||
                            mpsLastDay.toLocalDate().isBefore(masterProductionScheduleLineDate.getPlannedDate().toLocalDate())) {
                        mpsLastDay = masterProductionScheduleLineDate.getPlannedDate();
                    }
                }
            }
        }
        return  mpsLastDay;
    }

    private LocalDateTime getFirstPlannedDate(MasterProductionSchedule masterProductionSchedule,
                                              Long productionLineId) {

        LocalDateTime mpsFirstDay = null;

        for (MasterProductionScheduleLine masterProductionScheduleLine : masterProductionSchedule.getMasterProductionScheduleLines()) {
            if (masterProductionScheduleLine.getProductionLine().getId().equals(productionLineId)) {

                for (MasterProductionScheduleLineDate masterProductionScheduleLineDate : masterProductionScheduleLine.getMasterProductionScheduleLineDates()) {
                    if (Objects.isNull(mpsFirstDay) ||
                            mpsFirstDay.toLocalDate().isAfter(masterProductionScheduleLineDate.getPlannedDate().toLocalDate())) {
                        mpsFirstDay = masterProductionScheduleLineDate.getPlannedDate();
                    }
                }
            }
        }
        return  mpsFirstDay;
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

    public Collection<MasterProductionSchedule> getExistingMPSs(Long warehouseId, Long productionLineId,
                                                                String beginDateTime, String endDateTime) {
        List<MasterProductionScheduleLineDate> masterProductionScheduleLineDates =
                findAllMasterProductionScheduleLineDate(
                        warehouseId,
                        LocalDateTime.parse(beginDateTime),
                        LocalDateTime.parse(endDateTime),
                        productionLineId);
        return masterProductionScheduleLineDates.stream().map(MasterProductionScheduleLineDate::getMasterProductionScheduleLine)
                .map(MasterProductionScheduleLine::getMasterProductionSchedule).collect(Collectors.toSet());
    }


}

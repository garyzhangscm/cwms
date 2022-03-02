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
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.TrailerException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.TractorRepository;
import com.garyzhangscm.cwms.common.repository.TractorScheduleRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class TractorScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(TractorScheduleService.class);

    @Autowired
    private TractorScheduleRepository tractorScheduleRepository;



    public TractorSchedule findById(Long id) {
        return tractorScheduleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer not found by id: " + id));
    }


    public List<TractorSchedule> findAll(Long warehouseId,
                                         Long tractorId,
                                         String tractorNumber,
                                         LocalDateTime startCheckInTime,
                                         LocalDateTime endCheckInTime,
                                         LocalDateTime startDispatchTime,
                                         LocalDateTime endDispatchTime) {
        return tractorScheduleRepository.findAll(
                (Root<TractorSchedule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(tractorId) || Strings.isNotBlank(tractorNumber)) {

                        Join<TractorSchedule, Tractor> joinTractor = root.join("tractor", JoinType.INNER);
                        if (Objects.nonNull(tractorId)) {
                            predicates.add(criteriaBuilder.equal(
                                    joinTractor.get("id"), tractorId));
                        }
                        if (Strings.isNotBlank(tractorNumber)) {
                            predicates.add(criteriaBuilder.equal(
                                    joinTractor.get("number"), tractorNumber));
                        }
                    }
                    if (Objects.nonNull(startCheckInTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("checkInTime"), startCheckInTime));

                    }
                    if (Objects.nonNull(endCheckInTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("checkInTime"), endCheckInTime));

                    }
                    if (Objects.nonNull(startDispatchTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("dispatchTime"), startDispatchTime));

                    }
                    if (Objects.nonNull(endDispatchTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("dispatchTime"), endDispatchTime));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "tractor", "type", "checkInTime", "dispatchTime")
        );

    }


    public TractorSchedule save(TractorSchedule tractorSchedule) {
        return tractorScheduleRepository.save(tractorSchedule);
    }



    public void delete(TractorSchedule tractorSchedule) {
        tractorScheduleRepository.delete(tractorSchedule);
    }

    public void delete(Long id) {
        tractorScheduleRepository.deleteById(id);
    }


    public TractorSchedule addTractorSchedule(Long warehouseId, TractorSchedule tractorSchedule) {
        return save(tractorSchedule);
    }
}

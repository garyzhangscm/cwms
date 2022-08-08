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
import com.garyzhangscm.cwms.common.clients.KafkaSender;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.TrailerException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.TrailerAppointmentRepository;
import com.garyzhangscm.cwms.common.repository.TrailerRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class TrailerAppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerAppointmentService.class);

    @Autowired
    private TrailerAppointmentRepository trailerAppointmentRepository;

    @Autowired
    private KafkaSender kafkaSender;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public TrailerAppointment findById(Long id) {
        return trailerAppointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer appointment not found by id: " + id));
    }


    public List<TrailerAppointment> findAll(Long warehouseId,
                                            String number,
                                            String type,
                                            String status,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime,
                                            LocalDate date) {
        List<TrailerAppointment> trailerAppointments = trailerAppointmentRepository.findAll(
                (Root<TrailerAppointment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                    if (Strings.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), TrailerAppointmentType.valueOf(type)));
                    }
                    if (Strings.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"), TrailerAppointmentStatus.valueOf(status)));
                    }
                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("completedTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("completedTime"), endTime));

                    }
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("completedTime"), dateStartTime, dateEndTime));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));

                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "number")
        );

        return trailerAppointments;
    }


    public TrailerAppointment save(TrailerAppointment trailerAppointment) {
        return trailerAppointmentRepository.save(trailerAppointment);
    }
    public TrailerAppointment saveOrUpdate(TrailerAppointment trailerAppointment) {
        if (Objects.isNull(trailerAppointment.getId()) &&
            Objects.nonNull(findByNumber(
                    trailerAppointment.getWarehouseId(), trailerAppointment.getNumber()
            ))) {
            trailerAppointment.setId(
                    findByNumber(
                            trailerAppointment.getWarehouseId(),
                            trailerAppointment.getNumber()
                    ).getId()
            );
        }
        return save(trailerAppointment);
    }

    public TrailerAppointment findByNumber(Long warehouseId, String number) {

        return trailerAppointmentRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }




    public void delete(TrailerAppointment trailerAppointment) {
        trailerAppointmentRepository.delete(trailerAppointment);
    }

    public void delete(Long id) {
        trailerAppointmentRepository.deleteById(id);
    }

    public void processIntegration(TrailerAppointment trailerAppointment,
                                   long integrationId) {
        trailerAppointment.setStatus(TrailerAppointmentStatus.PLANNED);
        TrailerAppointment newTrailerAppointment = save(trailerAppointment);

        // we will only process the trailer appointment portion
        // here. The stop / shipment / order assignment will be processed
        // by outbound service
        trailerAppointment.setId(newTrailerAppointment.getId());



        kafkaSender.send(IntegrationType.INTEGRATION_STOP,
                trailerAppointment.getCompanyId() + "-" +
                        (Objects.isNull(trailerAppointment.getWarehouseId()) ? "" : trailerAppointment.getWarehouseId())
                        + "-" + integrationId, trailerAppointment);

    }


    public TrailerAppointment completeTrailerAppointment(Long id) {
        TrailerAppointment trailerAppointment = findById(id);
        if (!trailerAppointment.getStatus().equals(TrailerAppointmentStatus.PLANNED)&&
            !trailerAppointment.getStatus().equals(TrailerAppointmentStatus.INPROCESS)) {
            throw TrailerException.raiseException("Trailer is not ready for complete");
        }
        trailerAppointment.setStatus(TrailerAppointmentStatus.COMPLETED);
        trailerAppointment.setCompletedTime(LocalDateTime.now());
        return save(trailerAppointment);
    }
}

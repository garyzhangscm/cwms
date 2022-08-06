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


    public List<TrailerAppointment> findAll(  Long warehouseId,
                                 String number,
                                 String status) {
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
                    if (Strings.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"), TrailerAppointmentStatus.valueOf(status)));
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



    public void delete(TrailerAppointment trailerAppointment) {
        trailerAppointmentRepository.delete(trailerAppointment);
    }

    public void delete(Long id) {
        trailerAppointmentRepository.deleteById(id);
    }

    public void processIntegration(TrailerAppointment trailerAppointment,
                                   long integrationId) {
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



}

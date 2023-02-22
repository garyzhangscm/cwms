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
import org.springframework.web.bind.annotation.PathVariable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;


@Service
public class TrailerService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerService.class);

    @Autowired
    private TrailerRepository trailerRepository;
    @Autowired
    private TrailerAppointmentRepository trailerAppointmentRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Trailer findById(Long id) {
        return trailerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer not found by id: " + id));
    }


    public List<Trailer> findAll(Long companyId, Long warehouseId,
                                 String number,
                                 String trailerIds) {
        List<Trailer> trailers = trailerRepository.findAll(
                (Root<Trailer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }
                    if (Strings.isNotBlank(trailerIds)) {

                        CriteriaBuilder.In<Long> inTrailerIds = criteriaBuilder.in(root.get("id"));
                        for(String trailerId : trailerIds.split(",")) {
                            inTrailerIds.value(Long.parseLong(trailerId));
                        }
                        predicates.add(criteriaBuilder.and(inTrailerIds));
                    }
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                        criteriaBuilder.isNull(root.get("warehouseId"))));
                    }
                    else  {
                        return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                    }
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "number")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedContainerRecords(trailers);
        }
        return trailers;
    }

    /**
     * Remove the duplicated container record. If we have 2 record with the same container number
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param trailers
     */
    private void removeDuplicatedContainerRecords(List<Trailer> trailers) {
        Iterator<Trailer> containerIterator = trailers.listIterator();
        Set<String> containerProcessed = new HashSet<>();
        while(containerIterator.hasNext()) {
            Trailer container = containerIterator.next();

            if (containerProcessed.contains(container.getNumber()) &&
                    Objects.isNull(container.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                containerIterator.remove();
            }
            containerProcessed.add(container.getNumber());
        }
    }

    public Trailer save(Trailer trailers) {
        return trailerRepository.save(trailers);
    }



    public void delete(Trailer trailers) {
        trailerRepository.delete(trailers);
    }

    public void delete(Long id) {
        trailerRepository.deleteById(id);
    }


    public TrailerAppointment getTrailerCurrentAppointment(Long trailerId) {
        return findById(trailerId).getCurrentAppointment();
    }

    public TrailerAppointment getTrailerAppointmentById(Long id) {
        return trailerAppointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer appointment not found by id: " + id));
    }

    public TrailerAppointment addTrailerAppointment(Long id, TrailerAppointment trailerAppointment) {
        Trailer trailer = findById(id);
        trailerAppointment.setTrailer(trailer);
        trailerAppointment.setStatus(TrailerAppointmentStatus.PLANNED);
        trailerAppointment = trailerAppointmentRepository.save(trailerAppointment);
        trailer.setCurrentAppointment(trailerAppointment);
        save(trailer);

        return trailerAppointment;
    }

    public TrailerAppointment cancelTrailerAppointment(Long trailerId,
                                                       Long trailerAppointmentId) {
        TrailerAppointment trailerAppointment = trailerAppointmentRepository.findById(trailerAppointmentId)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer appointment not found by id: " + trailerAppointmentId));
        trailerAppointment.setStatus(TrailerAppointmentStatus.CANCELLED);
        trailerAppointment = trailerAppointmentRepository.save(trailerAppointment);

        // remove the current appointment from the trailer
        Trailer trailer = findById(trailerId);
        trailer.setCurrentAppointment(null);
        save(trailer);
        return trailerAppointment;
    }

    public TrailerAppointment completeTrailerAppointment(Long trailerId,
                                                         Long trailerAppointmentId) {
        TrailerAppointment trailerAppointment = trailerAppointmentRepository.findById(trailerAppointmentId)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer appointment not found by id: " + trailerAppointmentId));
        trailerAppointment.setStatus(TrailerAppointmentStatus.COMPLETED);
        trailerAppointment = trailerAppointmentRepository.save(trailerAppointment);

        // remove the current appointment from the trailer
        Trailer trailer = findById(trailerId);
        trailer.setCurrentAppointment(null);
        save(trailer);
        return trailerAppointment;
    }

    /**
     * Find open trailer that has not been assigned to any tractor yet
     * @param companyId
     * @param warehouseId
     * @return
     */
    public List<Trailer> findTrailersOpenForTractor(Long companyId, Long warehouseId) {
        return trailerRepository.findTrailersOpenForTractor(companyId, warehouseId);
    }

    public Trailer createAttachedTrailer(Long companyId, Long warehouseId,
                                         Tractor tractor) {
        Trailer trailer = new Trailer();
        trailer.setCompanyId(companyId);
        trailer.setWarehouseId(warehouseId);
        trailer.setNumber(tractor.getNumber());
        trailer.setDescription("AUTO created for tractor: " + tractor.getNumber());
        trailer.setSize(0.0);

        return save(trailer);
    }
}

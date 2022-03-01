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
import com.garyzhangscm.cwms.common.model.Location;
import com.garyzhangscm.cwms.common.model.Tractor;
import com.garyzhangscm.cwms.common.model.Trailer;
import com.garyzhangscm.cwms.common.model.TractorStatus;
import com.garyzhangscm.cwms.common.repository.TractorRepository;
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
public class TractorService {
    private static final Logger logger = LoggerFactory.getLogger(TractorService.class);

    @Autowired
    private TractorRepository tractorRepository;
    @Autowired
    private TrailerService trailerService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Tractor findById(Long id) {
        return tractorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer not found by id: " + id));
    }


    public List<Tractor> findAll(Long companyId, Long warehouseId,
                                 String number) {
        List<Tractor> tractors = tractorRepository.findAll(
                (Root<Tractor> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
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
            removeDuplicatedTractorRecords(tractors);
        }
        return tractors;
    }
    /**
     * Remove the duplicated tractors record. If we have 2 records with the same tractor number
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param tractors
     */
    private void removeDuplicatedTractorRecords(List<Tractor> tractors) {
        Iterator<Tractor> tractorIterator = tractors.listIterator();
        Set<String> tractorProcessed = new HashSet<>();
        while(tractorIterator.hasNext()) {
            Tractor tractor = tractorIterator.next();

            if (tractorProcessed.contains(tractor.getNumber()) &&
                    Objects.isNull(tractor.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                tractorIterator.remove();
            }
            tractorProcessed.add(tractor.getNumber());
        }
    }

    public List<Tractor> findByNumber(Long warehouseId, String number) {
        return tractorRepository.findByWarehouseIdAndNumber(warehouseId, number);

    }


    public Tractor save(Tractor tractor) {
        return tractorRepository.save(tractor);
    }



    public void delete(Tractor tractor) {
        tractorRepository.delete(tractor);
    }

    public void delete(Long id) {
        tractorRepository.deleteById(id);
    }


    // OK, we check in tractor without any suggested dock location. There normally
    // happens when the trailer is a fake tractor and we don't care about
    // the actual dock door we check in
    public Tractor checkInTractor(Tractor tractor) {
        List<Location> dockLocations = warehouseLayoutServiceRestemplateClient.findEmptyDockLocations(tractor.getWarehouseId());
        if (dockLocations.size() > 0) {
            return checkInTractor(tractor, dockLocations.get(0));
        }
        throw TrailerException.raiseException(  "Can't find empty dock location to check in");
    }

    public Tractor checkInTractor(Long tractorId, Location dockLocation) {
        return checkInTractor(findById(tractorId), dockLocation);
    }
    // Check in the tractor, when the tractor actually arrives at the warehouse
    // We will create a temporary location for the tractor so when we
    // actually load the inventory onto the trailer, we will systematically move
    // the inventory onto the tractor
    public Tractor checkInTractor(Tractor tractor, Location dockLocation) {

        warehouseLayoutServiceRestemplateClient.checkInTractorAtDockLocations(dockLocation.getId(), tractor.getId());
        tractor.setLocationId(dockLocation.getId());
        return save(tractor);
    }

    public Tractor dispatchTractor(Long tractorId) {
        return dispatchTractor(findById(tractorId));
    }

    public Tractor dispatchTractor(Tractor tractor) {

        warehouseLayoutServiceRestemplateClient.dispatchTractorFromDockLocations(tractor.getLocationId());
        tractor.setLocationId(null);
        tractor.setStatus(TractorStatus.DISPATCHED);

        // complete all the stops in the tractor
        tractor = save(tractor);
        logger.debug("Set the tractor {}'s status to dispatched", tractor.getNumber());
        return tractor;
    }


    public Tractor addTractor(Tractor tractor,
                              Long companyId, Long warehouseId,
                              Boolean hasAttachedTrailer,
                              Boolean autoCreatedTrailer,
                              String attachedTrailerIds) {
        // see if there's trailer attached to this tractor. Attached
        // means the trailer is physically connect to this tractor and
        // won't break from this tractor so that when we assign appointment
        // to this tractor, we already know the trailer we will use
        if (Boolean.TRUE.equals(hasAttachedTrailer)) {
            List<Trailer> trailers = new ArrayList<>();
            if (autoCreatedTrailer) {
                // we will only create one trailer for the tractor
                Trailer trailer = trailerService.createAttachedTrailer(companyId, warehouseId, tractor);
                trailers.add(trailer);
            }
            else if (Strings.isNotBlank(attachedTrailerIds)) {
                // if the user specify the trailers to be attached
                trailers = trailerService.findAll(companyId, warehouseId, null, attachedTrailerIds);
            }
            tractor.setAttachedTrailers(trailers);
        }

        return save(tractor);

    }
}

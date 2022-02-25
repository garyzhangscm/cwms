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

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.StopRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class StopService {
    private static final Logger logger = LoggerFactory.getLogger(StopService.class);

    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private ShipmentService shipmentService;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Stop findById(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("stop not found by id: " + id));
    }


    public List<Stop> findAll(Long warehouseId,
                              String number,
                              Long trailerAppointmentId,
                              Long sequence,
                              Boolean onlyOpenStops) {
        return stopRepository.findAll(
                (Root<Stop> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

                    if (Objects.nonNull(trailerAppointmentId)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("trailerAppointmentId"), trailerAppointmentId));

                    }
                    if (Objects.nonNull(sequence)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("sequence"), sequence));

                    }
                    if (Boolean.TRUE.equals(onlyOpenStops)) {
                        // we will only return the stop that is not assigned
                        // to any trailer appointment yet

                        predicates.add(criteriaBuilder.isNull( root.get("trailerAppointmentId") ));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "number")
        );
    }


    public Stop save(Stop stop) {
        return stopRepository.save(stop);
    }
    public Stop saveAndFlush(Stop stop) {
        return stopRepository.saveAndFlush(stop);
    }


    public void delete(Stop stop) {
        stopRepository.delete(stop);
    }

    public void delete(Long id) {
        stopRepository.deleteById(id);
    }

    public void delete(String stopIds) {
        if (!stopIds.isEmpty()) {
            long[] stopIdArray = Arrays.asList(stopIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : stopIdArray) {
                delete(id);
            }
        }
    }

    public Stop createStop(Shipment shipment) {
        Stop stop = new Stop();
        stop.setNumber(commonServiceRestemplateClient.getNextNumber(shipment.getWarehouseId(), "stop"));

        List<Shipment> shipments = new ArrayList<>();
        shipments.add(shipment);
        stop.setShipments(shipments);
        stop.setWarehouseId(shipment.getWarehouseId());
        return saveAndFlush(stop);
    }

    public boolean isAllShipmentLoaded(Stop stop) {
        Long shipmentUnloaded = stop.getShipments()
                .stream()
                .filter(shipment ->
                        !shipment.getStatus().equals(ShipmentStatus.DISPATCHED) &&
                        !shipment.getStatus().equals(ShipmentStatus.LOADED))
                .count();
        return shipmentUnloaded == 0;
    }

    public void completeStop(Stop stop) {
        stop.getShipments().stream().forEach(shipment ->
                shipmentService.dispatchShipment(shipment));
    }

    public List<Stop> getOpenStops(Long warehouseId) {
        return findAll(warehouseId, null, null, null, true);
    }

    public void assignTrailerAppointment(long stopId, TrailerAppointment trailerAppointment) {
        assignTrailerAppointment(findById(stopId), trailerAppointment);
    }

    public Long getNextStopSequenceInTrailerAppointment(Long warehouseId, TrailerAppointment trailerAppointment) {
        if (Objects.isNull(trailerAppointment.getId())) {
            return 1l;
        }
        List<Stop> stops = findAll(warehouseId, null,
                trailerAppointment.getId(), null, null);
        return stops.stream().mapToLong(stop -> Objects.isNull(stop.getSequence()) ? 0 : stop.getSequence())
                .max().orElse(0l) + 1l;

    }

    public void assignTrailerAppointment(Stop stop, TrailerAppointment trailerAppointment) {
        if (Objects.nonNull(stop.getTrailerAppointmentId())) {
            // if the stop is already assigned to some trailer appointment,

            if (Objects.equals(trailerAppointment.getId(), stop.getTrailerAppointmentId())) {
                // the stop is already assigned to the current trailer appointment, we don't need to
                // do anything
                return;
            }
            else {

                throw ShippingException.raiseException("The stop is already assigned to some other trailer appointment, " +
                        " please remove the stop from the existing appointment first");
            }
        }
        stop.setTrailerAppointmentId(trailerAppointment.getId());
        stop.setSequence(getNextStopSequenceInTrailerAppointment(stop.getWarehouseId(), trailerAppointment));
        save(stop);
    }

    public Stop findMatchedStop(TrailerAppointment trailerAppointment, Shipment shipment) {
        if (Objects.isNull(trailerAppointment.getId())) {
            return null;
        }
        List<Stop> stops = findAll(shipment.getWarehouseId(),
                null,  trailerAppointment.getId(), null, null);
        return stops.stream().filter(stop -> stop.validateNewShipmentsForStop(shipment)).findFirst().orElse(null);
    }

}

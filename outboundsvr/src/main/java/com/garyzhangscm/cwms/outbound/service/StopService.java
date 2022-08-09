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
    private OrderLineService orderLineService;


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
                              String status,
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
                    if (Strings.isNotBlank(status)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("status"), StopStatus.valueOf(status)));

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

    public Stop findByNumber(Long warehouseId, String number) {
        return stopRepository.findByWarehouseIdAndNumber(warehouseId,
                number);
    }

    public Stop save(Stop stop) {
        return stopRepository.save(stop);
    }
    public Stop saveAndFlush(Stop stop) {
        return stopRepository.saveAndFlush(stop);
    }

    public Stop saveOrUpdate(Stop stop) {
        if (Objects.isNull(stop.getId()) &&
                Objects.nonNull(findByNumber(stop.getWarehouseId(), stop.getNumber()))) {
            stop.setId(
                    findByNumber(stop.getWarehouseId(), stop.getNumber()).getId()
            );
        }
        return save(stop);
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

    public Stop completeStop(Long id) {
        return completeStop(findById(id));
    }
    public Stop completeStop(Stop stop) {
        if (stop.getStatus() == StopStatus.CANCELLED) {
            logger.debug("Can't complete the stop {} as its status is {}",
                    stop.getNumber(), stop.getStatus());
            throw ShippingException.raiseException("Fail to allocate the stop " +
                    stop.getNumber() + " due to its current status " + stop.getStatus());
        }
        if (stop.getStatus() == StopStatus.COMPLETED) {
            logger.debug("The stop {} is already completed, do nothing",
                    stop.getNumber());
            return stop;
        }

        if (Objects.isNull(stop.getTrailerAppointment())) {
            stop.setTrailerAppointment(
                    commonServiceRestemplateClient.getTrailerAppointmentById(
                            stop.getTrailerAppointmentId()
                    )
            );
        }

        stop.setStatus(StopStatus.COMPLETED);
        saveOrUpdate(stop);
        stop.getShipments().stream().forEach(shipment ->
                shipmentService.completeShipment(shipment));
        return stop;
    }

    public List<Stop> getOpenStops(Long warehouseId) {
        return findAll(warehouseId, null, null, null, null,true);
    }

    public void assignTrailerAppointment(long stopId, TrailerAppointment trailerAppointment) {
        assignTrailerAppointment(findById(stopId), trailerAppointment);
    }

    public Long getNextStopSequenceInTrailerAppointment(Long warehouseId, TrailerAppointment trailerAppointment) {
        if (Objects.isNull(trailerAppointment.getId())) {
            return 1l;
        }
        List<Stop> stops = findAll(warehouseId, null,
                trailerAppointment.getId(), null, null,null);
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
                null,  trailerAppointment.getId(), null, null,null);
        return stops.stream().filter(stop -> stop.validateNewShipmentsForStop(shipment)).findFirst().orElse(null);
    }

    /**
     * Process integration for stop. We will create the stop and add the shipment into the stops
     * @param stop
     * @param trailerAppointmentId
     */
    public Stop processIntegration(Wave wave, Stop stop, Long trailerAppointmentId) {
        // we will plan the orders into the shipment
        // and then add the shipment into the stop
        // after that we will save the stop
        stop.setStatus(StopStatus.PLANNED);
        List<Shipment> plannedShipments = new ArrayList<>();
        for (Shipment shipment : stop.getShipments()) {
            // we will plan every order line in the same order into the same
            // shipment
            List<OrderLine> orderLines = new ArrayList<>();

            shipment.getShipmentLines().stream().filter(
                    shipmentLine -> Objects.nonNull(shipmentLine.getOrderLineId())
            ).forEach(
                    shipmentLine -> {
                        orderLines.add(
                                orderLineService.findById(
                                        shipmentLine.getOrderLineId(), false
                                )
                        );

                    }
            );
            logger.debug("start to plan shipment for order {}, with {} order lines, " +
                    "shipment number: {}",
                    shipment.getOrderId(),
                    orderLines.size(),
                    shipment.getNumber());
            Shipment plannedShipment = shipmentService.planShipments(
                    wave, shipment.getNumber(), orderLines
            );
            if (Objects.isNull(plannedShipment)) {
                throw ShippingException.raiseException("Not able to plan shipment for stop " +
                                stop.getNumber() + ", shipment " + shipment.getNumber()+ ", " +
                                " order id " + shipment.getOrderId() + ", there's no available order lines");
            }
            logger.debug("shipment {} planned for order lines ",
                    plannedShipment.getNumber());
            orderLines.forEach(
                    orderLine ->
                            logger.debug("> order line {} / {}",
                                    orderLine.getOrderNumber(), orderLine.getNumber())
            );
            plannedShipments.add(plannedShipment);

        }
        logger.debug("add totally {} shipments into the stop {}",
                plannedShipments.size(), stop.getNumber());
        stop.setTrailerAppointmentId(trailerAppointmentId);
        // clear the stop's shipment for now. We will assign the shipment to the stop
        // later on
        stop.setShipments(Collections.emptyList());
        Stop newStop = saveOrUpdate(stop);

        shipmentService.assignShipmentToStop(newStop, plannedShipments);
        return newStop;
    }

    public Stop allocateStop(Long id) {
        return allocateStop(findById(id));
    }
    public Stop allocateStop(Stop stop) {
        if (stop.getStatus() == StopStatus.CANCELLED || stop.getStatus() == StopStatus.COMPLETED) {
            logger.debug("Can't allocate the stop {} as its status is {}",
                    stop.getNumber(), stop.getStatus());
            throw ShippingException.raiseException("Fail to allocate the stop " +
                    stop.getNumber() + " due to its current status " + stop.getStatus());
        }
        stop.setStatus(StopStatus.INPROCESS);
        saveOrUpdate(stop);

        stop.getShipments().forEach(
                shipment -> shipmentService.allocateShipment(shipment.getId())
        );
        return stop;

    }
}

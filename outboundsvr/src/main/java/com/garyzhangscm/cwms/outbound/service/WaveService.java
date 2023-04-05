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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.WaveRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;


@Service
public class WaveService {
    private static final Logger logger = LoggerFactory.getLogger(WaveService.class);

    @Autowired
    private WaveRepository waveRepository;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderLineService orderLineService;

    @Autowired
    private ShipmentLineService shipmentLineService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public Wave findById(Long id) {
        return findById(id, true);
    }

    public Wave findById(Long id, boolean loadAttribute) {

        Wave wave =  waveRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("wave not found by id: " + id));
        if (loadAttribute) {
            loadAttribute(wave);
        }
        return wave;
    }

    public List<Wave> findAll(Long warehouseId,
                              String number,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date) {
        return findAll(warehouseId, number, startTime, endTime, date, true);
    }

    public List<Wave> findAll(Long warehouseId,
                              String number,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date,
                              boolean loadAttribute) {
        List<Wave> waves
            = waveRepository.findAll(
                (Root<Wave> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(number)) {
                        if (number.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }

                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "warehouseId", "createdTime", "number")
            );


        if (waves.size() > 0 && loadAttribute) {
            loadAttribute(waves);
        }
        return waves;
    }

    public Wave findByNumber(String number) {
        return findByNumber(number, true);
    }

    public Wave findByNumber(String number, boolean loadAttribute) {
        logger.debug("start to find wave by number {}", number);
        Wave wave = waveRepository.findByNumber(number);
        if (Objects.nonNull(wave) && loadAttribute) {
            loadAttribute(wave);
        }
        return wave;
    }

    private void loadAttribute(List<Wave> waves) {
        waves.forEach(wave -> loadAttribute(wave));
    }
    private void loadAttribute(Wave wave) {
        wave.getShipmentLines().forEach(shipmentLine -> {
            loadOrderLineAttribute(shipmentLine.getOrderLine());
            if (shipmentLine.getShortAllocations().size() > 0) {
                loadShortAllocationAttribute(shipmentLine.getShortAllocations());
            }
            loadPickAttribute(shipmentLine.getPicks());
        });
    }
    private void loadOrderLineAttribute(OrderLine orderLine) {
        if (orderLine.getInventoryStatusId() != null && orderLine.getInventoryStatus() == null) {
            orderLine.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(orderLine.getInventoryStatusId()));
        }
        if (orderLine.getItemId() != null && orderLine.getItem() == null) {
            orderLine.setItem(inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()));
        }
    }

    private void loadShortAllocationAttribute(List<ShortAllocation> shortAllocations) {

        shortAllocations.forEach(this::loadShortAllocationAttribute);
    }
    private void loadShortAllocationAttribute(ShortAllocation shortAllocation) {

        if (shortAllocation.getItemId() != null && shortAllocation.getItem() == null) {
            shortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(shortAllocation.getItemId()));
        }
    }

    private void loadPickAttribute(List<Pick> picks) {
        picks.stream().filter(Objects::nonNull).forEach(this::loadPickAttribute);
    }
    private void loadPickAttribute(Pick pick) {

        if (pick.getItemId() != null && pick.getItem() == null) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }
        if (pick.getSourceLocationId() != null && pick.getSourceLocation() == null) {
            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
        }
        if (pick.getDestinationLocationId() != null && pick.getDestinationLocation() == null) {
            pick.setDestinationLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getDestinationLocationId()));
        }
    }
    public Wave save(Wave wave) {
        return waveRepository.save(wave);
    }


    public void delete(Wave wave) {
        waveRepository.delete(wave);
    }

    public void delete(Long id) {
        waveRepository.deleteById(id);
    }

    public void delete(String waveIds) {
        if (!waveIds.isEmpty()) {
            long[] waveIdArray = Arrays.asList(waveIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : waveIdArray) {
                delete(id);
            }
        }
    }

    public void cancelWave(Wave wave) {

    }
    public void cancelWave(Long id) {
        cancelWave(findById(id));
    }

    // Plan a list of order lines into a wave
    @Transactional
    public Wave planWave(Long warehouseId, String waveNumber, List<Long> orderLineIds) {

        if (StringUtils.isBlank(waveNumber)) {
            waveNumber = getNextWaveNumber(warehouseId);
            logger.debug(">> wave number is not passed in during plan wave, auto generated number: {}", waveNumber);
        }

        logger.debug(">> Start to plan {} order lines into wave # {}", orderLineIds.size(), waveNumber);
        Wave wave = findByNumber(waveNumber);
        if (Objects.isNull(wave)) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            wave.setStatus(WaveStatus.PLANED);
            wave.setWarehouseId(warehouseId);
            wave = save(wave);
        }
        List<Shipment> shipments = planShipments(warehouseId, wave, orderLineIds);
        Collections.sort(shipments, Comparator.comparing(Shipment::getId));
        for(Shipment shipment : shipments) {
            for(ShipmentLine shipmentLine : shipment.getShipmentLines()) {
                wave.getShipmentLines().add(shipmentLine);
            }
        }

        logger.debug(">> we get {} shipment lines",
                shipments.stream().map(shipment -> shipment.getShipmentLines()).flatMap(shipmentLines -> shipmentLines.stream()).count()
        );
        logger.debug(">> The wave has {} shipment lines", wave.getShipmentLines().size());

        return wave;

    }

    /**
     * create shipment for each order and plan the shipment into the wave
     * @param wave Wave
     * @param orderLineIds order line IDs
     */
    private List<Shipment> planShipments(Long warehouseId, Wave wave, List<Long> orderLineIds) {
        List<Shipment> shipments = new ArrayList<>();
        logger.debug(">> start to plan {} lines into wave {}",
                orderLineIds.size(), wave.getNumber());
        Map<String, List<OrderLine>> orders = new HashMap<>();
        orderLineIds.forEach(orderLineId -> {
            OrderLine orderLine = orderLineService.findById(orderLineId);

            List<OrderLine> orderLines = orders.getOrDefault(
                    orderLine.getOrderNumber(), new ArrayList<>()
            );
            orderLines.add(orderLine);
            orders.put(orderLine.getOrderNumber(), orderLines);
        });

        logger.debug(">> we find {} orders out of those order lines",
                orders.size());

        // Let's plan a shipment for each order
        orders.entrySet().forEach(entry -> {
            String shipmentNumber = shipmentService.getNextShipmentNumber(warehouseId);
            logger.debug("Start to plan shipment for order: {}, line # {}, into shipment {}",
                    entry.getKey(), entry.getValue().size(), shipmentNumber);

            shipments.add(shipmentService.planShipments(wave, shipmentNumber, entry.getValue()));

        });

            return shipments;
    }

    public Wave createWave(Long warehouseId, String waveNumber) {

        Wave wave = findByNumber(waveNumber);
        if (wave == null) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            wave.setStatus(WaveStatus.PLANED);
            wave.setWarehouseId(warehouseId);
            return save(wave);
        }
        else {
            return wave;
        }
    }

    public List<Order> findWaveCandidate(Long warehouseId, String orderNumber,
                                         Long clientId,
                                         String customerName, Long customerId,
                                         ZonedDateTime startCreatedTime,
                                         ZonedDateTime endCreatedTime,
                                         LocalDate specificCreatedDate,
                                         ClientRestriction clientRestriction
    ) {

        return orderService.findWavableOrders(warehouseId, orderNumber, clientId,
                customerName, customerId, startCreatedTime, endCreatedTime,
                specificCreatedDate, clientRestriction);
    }

    public Wave allocateWave(Long id) {
        return allocateWave(findById(id));
    }
    public Wave allocateWave(Wave wave) {

        // Allocate each open shipment line
        wave.getShipmentLines().forEach(shipmentLine -> {
            shipmentLineService.allocateShipmentLine(shipmentLine);
        });
        wave.setStatus(WaveStatus.ALLOCATED);

        // return the latest information
        return save(wave);

    }

    private String getNextWaveNumber(Long warehouseId){
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "wave-number");
    }
}

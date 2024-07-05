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
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.WaveRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WaveService {
    private static final Logger logger = LoggerFactory.getLogger(WaveService.class);

    // maximun orders per wave
    private static final int MAX_ORDER_PER_WAVE = 1000;

    @Autowired
    private WaveRepository waveRepository;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private PickListService pickListService;

    @Autowired
    private BulkPickService bulkPickService;
    @Autowired
    private PickService pickService;
    @Autowired
    private ShipmentLineService shipmentLineService;

    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Autowired
    private BulkPickConfigurationService bulkPickConfigurationService;
    @Autowired
    private PickReleaseService pickReleaseService;
    @Autowired
    private ShortAllocationService shortAllocationService;

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
                              String waveStatus,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date,
                              Boolean includeCompletedWave,
                              Boolean includeCancelledWave) {
        return findAll(warehouseId, number, waveStatus,
                startTime, endTime, date, includeCompletedWave,
                includeCancelledWave,
                true);
    }

    public List<Wave> findAll(Long warehouseId,
                              String number,
                              String waveStatus,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date,
                              Boolean includeCompletedWave,
                              Boolean includeCancelledWave,
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
                    if (Strings.isNotBlank(waveStatus)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"),
                                WaveStatus.valueOf(waveStatus)));
                    }

                    // if not include complete wave
                    if (!Boolean.TRUE.equals(includeCompletedWave)) {

                        predicates.add(criteriaBuilder.notEqual(root.get("status"),
                                WaveStatus.COMPLETED));
                    }

                    // if not include complete wave
                    if (!Boolean.TRUE.equals(includeCancelledWave)) {

                        predicates.add(criteriaBuilder.notEqual(root.get("status"),
                                WaveStatus.CANCELLED));
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

    public Wave findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }

    public Wave findByNumber(Long warehouseId, String number, boolean loadAttribute) {
        logger.debug("start to find wave by number {}", number);
        Wave wave = waveRepository.findByWarehouseIdAndNumber(warehouseId, number);
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

    public Wave saveOrUpdate(Wave wave) {
        if (Objects.isNull(wave.getId()) &&
                Objects.nonNull( findByNumber(wave.getWarehouseId(), wave.getNumber(), false)) ) {
            wave.setId(
                    findByNumber(wave.getWarehouseId(), wave.getNumber(), false).getId()
            );
        }
        return save(wave);
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

    @Transactional
    public void cancelWave(Wave wave) {
        // first cancel all the picks and short allocation
        logger.debug("start to cancel the wave {}", wave.getNumber());
        valdiateWaveReadyForCancel(wave);

        Set<Long> shipmentIds = new HashSet<>();
        logger.debug("we will start from cancelling all the shipment first");
        wave.getShipmentLines().forEach(
                shipmentLine -> {
                    List<Long> pickIds = shipmentLine.getPicks().stream().map(pick -> pick.getId()).collect(Collectors.toList());
                    pickIds.forEach(
                            pickId -> pickService.cancelPick(pickId, false, false)
                    );
                    logger.debug("picks for the shipment line {} / {} has been cancelled",
                            shipmentLine.getShipment().getNumber(),
                            shipmentLine.getNumber());

                    List<Long> shortAllocationIds = shipmentLine.getShortAllocations().stream().map(
                            shortAllocation -> shortAllocation.getId()).collect(Collectors.toList());
                    shortAllocationIds.forEach(
                            shortAllocationId -> shortAllocationService.cancelShortAllocation(shortAllocationId)
                    );
                    logger.debug("short allocation for the shipment line {} / {} has been cancelled",
                            shipmentLine.getShipment().getNumber(),
                            shipmentLine.getNumber());

                    shipmentLineService.cancelShipmentLine(shipmentLine);
                    logger.debug("shipment line {} / {} has been cancelled",
                            shipmentLine.getShipment().getNumber(),
                            shipmentLine.getNumber());

                    shipmentIds.add(shipmentLine.getShipment().getId());
                }
        );
        // see if we will need to cancel the shipment as well
        logger.debug("start to check if we will need to cancel the shipments as well");
        shipmentIds.forEach(
                shipmentId -> {
                    Shipment shipment = shipmentService.findById(shipmentId);
                    // if all the lines has been cancelled, then cancel the shipment
                    boolean allLineCancelled = !shipment.getShipmentLines().stream().anyMatch(
                            shipmentLine -> !shipmentLine.getStatus().equals(ShipmentLineStatus.CANCELLED)
                    );
                    logger.debug("Do we need to cancel the shipment {} as well? {}",
                            shipment.getNumber(), allLineCancelled);

                    if (allLineCancelled) {
                        shipmentService.cancelShipment(shipment);
                    }
                }
        );
        wave.setStatus(WaveStatus.CANCELLED);
        save(wave);

    }

    /**
     * Check if wave is ready for cancel. The wave is ready for cancel only if
     * there's no inventory being picked yet
     * @param wave
     */
    private void valdiateWaveReadyForCancel(Wave wave) {
        boolean inventoryPicked = wave.getShipmentLines().stream().anyMatch(
                shipmentLine -> shipmentLine.getPicks().stream().anyMatch(
                        pick -> pick.getPickedQuantity() > 0
                )
        );
        if (inventoryPicked) {
            throw OrderOperationException.raiseException("Can't cancel wave " + wave.getNumber() +
                    " as there's already inventory picked. please unpick the inventory first");
        }
    }

    @Transactional
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
        Wave wave = findByNumber(warehouseId, waveNumber);
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

        Wave wave = findByNumber(warehouseId, waveNumber);
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

    public List<Order> findWaveableOrdersCandidate(Long warehouseId, String orderNumber,
                                         Long clientId,
                                         String customerName, Long customerId,
                                         ZonedDateTime startCreatedTime,
                                         ZonedDateTime endCreatedTime,
                                         LocalDate specificCreatedDate,
                                         Boolean singleOrderLineOnly,
                                         Boolean singleOrderQuantityOnly,
                                         Boolean singleOrderCaseQuantityOnly,
                                         ClientRestriction clientRestriction
    ) {

        return orderService.findWaveableOrdersCandidate(warehouseId, orderNumber, clientId,
                customerName, customerId, startCreatedTime, endCreatedTime,
                specificCreatedDate,
                singleOrderLineOnly, singleOrderQuantityOnly, singleOrderCaseQuantityOnly,
                clientRestriction, MAX_ORDER_PER_WAVE);
    }

    public List<Shipment> findWaveableShipmentsCandidate(Long warehouseId, String orderNumber,
                                                  Long clientId,
                                                  String customerName, Long customerId,
                                                  ZonedDateTime startCreatedTime,
                                                  ZonedDateTime endCreatedTime,
                                                  LocalDate specificCreatedDate,
                                                  Boolean singleOrderLineOnly,
                                                  Boolean singleOrderQuantityOnly,
                                                  Boolean singleOrderCaseQuantityOnly,
                                                  ClientRestriction clientRestriction
    ) {

        return shipmentService.findWaveableShipmentsCandidate(warehouseId, orderNumber, clientId,
                customerName, customerId, startCreatedTime, endCreatedTime,
                specificCreatedDate,
                singleOrderLineOnly, singleOrderQuantityOnly, singleOrderCaseQuantityOnly,
                clientRestriction, MAX_ORDER_PER_WAVE);
    }

    public Wave allocateWave(Long id, Boolean asynchronous) {
        return allocateWave(findById(id), asynchronous);
    }
    public Wave allocateWave(Wave wave, Boolean asynchronous) {


        logger.debug(">>>    Start to allocate wave  {} ,asynchronous? : {}  <<<",
                wave.getNumber(), asynchronous);

        if (wave.getStatus().equals(WaveStatus.ALLOCATING)) {

            throw OrderOperationException.raiseException("Wave " + wave.getNumber() + " is allocating, please wait for it to complete");
        }
        if (wave.getStatus().equals(WaveStatus.CANCELLED)) {

            throw OrderOperationException.raiseException("Wave " + wave.getNumber() + " is already cancelled, can't allocate it");
        }
        if (wave.getStatus().equals(WaveStatus.COMPLETED)) {

            throw OrderOperationException.raiseException("Wave " + wave.getNumber() + " is already completed, can't allocate it");
        }

        List<ShipmentLine> allocatableShipmentLines =
                wave.getShipmentLines().stream().filter(
                        shipmentLine -> shipmentLineService.isAllocatable(shipmentLine) && shipmentLine.getOpenQuantity() > 0 )
                        .collect(Collectors.toList());

        wave.setStatus(WaveStatus.ALLOCATING);
        saveOrUpdate(wave);

        // check if we will need to allocate asynchronously
        // 1. if the client explicitly want asynchronous
        // 2. if the warehouse is configured to allocate asynchronously
        if (Objects.isNull(asynchronous)) {
            // TO-DO: Will need to use pallet quantity instead of quantity
            long totalPalletQuantity = allocatableShipmentLines.stream().map(
                    shipmentLine -> orderLineService.getPalletQuantityEstimation(
                            shipmentLine.getOrderLine(), shipmentLine.getOpenQuantity()
                    )
            ).mapToLong(Long::longValue).sum();

            asynchronous  = outboundConfigurationService.isSynchronousAllocationRequired(
                    wave.getWarehouseId(), totalPalletQuantity);
        }


        logger.debug("allocate wave {} Asynchronously or Synchronously? {}",
                wave.getNumber(),
                Boolean.TRUE.equals(asynchronous) ? "Asynchronously" : "Synchronously");

        if (Boolean.TRUE.equals(asynchronous)) {
            new Thread(() -> {

                logger.debug("start to allocate the wave asynchronously");
                Wave finalWave = findById(wave.getId());
                allocateWave(finalWave, allocatableShipmentLines);

                logger.debug("Asynchronously wave allocation for {} is done",
                        finalWave.getNumber());


            }).start();
        }
        else {
            // Allocate each open shipment line
            allocateWave(wave, allocatableShipmentLines);
            logger.debug("Synchronously wave allocation for {} is done",
                    wave.getNumber());

        }
        return saveOrUpdate(wave);

    }

    private Wave allocateWave(Wave wave, List<ShipmentLine> allocatableShipmentLines) {

        List<AllocationResult> allocationResults = new ArrayList<>();
        allocatableShipmentLines.forEach(shipmentLine -> {
            allocationResults.add(shipmentLineService.allocateShipmentLine(shipmentLine));
        });

        wave.setStatus(WaveStatus.ALLOCATED);

        // return the latest information
        wave = saveOrUpdate(wave);

        // post allocation process
        // 1. bulk pick
        // 2. list pick
        // 3. release picks into work task
        logger.debug("wave is allocated and picks are generated, let's see if we need to group them into" +
                " bulk or list and release");
        postAllocationProcess(wave.getWarehouseId(),
                wave.getNumber(),  allocationResults);

        logger.debug("wave is allocated!");
        return saveOrUpdate(wave);
    }

    /**
     * Post allocation process
     * 1. bulk pick
     * 2. list pick
     * @param allocationResults
     */
    private void postAllocationProcess(Long warehouseId,
                                       String waveNumber,
                                       List<AllocationResult> allocationResults) {
        // we will always try bulk pick first
        logger.debug("Let's see if we will need to group the picks into bulk");
        requestBulkPick(warehouseId, waveNumber, allocationResults);

        // for anything that not fall in the bulk pick, see if we can group them into
        // a list pick
        logger.debug("Let's see if we will need to group the picks into list");
        processListPick(warehouseId, waveNumber, allocationResults);

        releaseSinglePicks(warehouseId, waveNumber, allocationResults);

    }


    /**
     * Find picks from the same wave and group them together into the same list
     * @param warehouseId
     * @param waveNumber
     */
    private void processListPick(Long warehouseId, String waveNumber, List<AllocationResult> allocationResults) {
        logger.debug("start to process pick list for wave {}", waveNumber);
        // save the pick list that generated in this session.
        // in case the pick list configuration is setup to be NOT allow new pick
        // being group into existing, then we will only group the pick into
        // the list that generated in the same session
        List<PickList> pickLists = new ArrayList<>();

        // let's get any pick that is
        // 1. not in any group
        // 2. in PENDING status
        // and see if we can group into a existing pick
        allocationResults.stream().map(
                allocationResult ->  allocationResult.getPicks()
        ).flatMap(List::stream)
                .filter(pick ->  pick.getStatus().equals(PickStatus.PENDING) &&
                            Objects.isNull(pick.getBulkPick()) &&
                            Objects.isNull(pick.getCartonization()) &&
                            Objects.isNull(pick.getWorkTaskId()) &&
                            // !Boolean.TRUE.equals(pick.getWholeLPNPick()) &&
                            pick.getPickedQuantity() == 0
                 )
                .forEach(
                        pick -> {
                            pickListService.processPickList(pick, pickLists);
                        }
                );


    }
    /**
     * Release the picks of the wave, which are not in any group of
     * 1. list pick
     * 2. bulk pick
     * 3. carton pick
     * @param warehouseId
     * @param waveNumber
     * @param allocationResults
     */
    private void releaseSinglePicks(Long warehouseId,
                                    String waveNumber,
                                    List<AllocationResult> allocationResults) {
        // let's get any pick that is
        // 1. not in any group
        // 2. in PENDING status
        // and then release
        allocationResults.stream().map(
                allocationResult ->  allocationResult.getPicks()
        ).flatMap(List::stream)
                .filter(pick -> {

                    logger.debug("check if we will need to release the pick {}",
                            pick.getNumber());
                    logger.debug("pick.getStatus().equals(PickStatus.PENDING): {}",
                            pick.getStatus().equals(PickStatus.PENDING));
                    logger.debug("Objects.isNull(pick.getBulkPick()): {}",
                            Objects.isNull(pick.getBulkPick()));
                    logger.debug("Objects.isNull(pick.getCartonization()): {}", Objects.isNull(pick.getCartonization()) );
                    logger.debug("Objects.isNull(pick.getPickList()): {}", Objects.isNull(pick.getPickList()));
                    logger.debug("Objects.isNull(pick.getWorkTaskId()): {}", Objects.isNull(pick.getWorkTaskId()));
                    logger.debug("pick.getPickedQuantity() == 0: {}", pick.getPickedQuantity() == 0);
                    return pick.getStatus().equals(PickStatus.PENDING) &&
                        Objects.isNull(pick.getBulkPick()) &&
                        Objects.isNull(pick.getCartonization()) &&
                        Objects.isNull(pick.getPickList()) &&
                        Objects.isNull(pick.getWorkTaskId()) &&
                        pick.getPickedQuantity() == 0;
                })
                .forEach(
                        pick -> {
                            pick = pickReleaseService.releasePick(pick);
                            logger.debug("pick {} is released? {}, work task id: {}",
                                    pick.getNumber(),
                                    PickStatus.RELEASED.equals(pick.getStatus()),
                                    pick.getWorkTaskId());
                            pickService.saveOrUpdate(pick, false);
                        }
                );

    }

    /**
     * Group all the picks into bulk pick, if possible
     * @param allocationResults
     */
    private void requestBulkPick(Long warehouseId,
                                 String waveNumber,
                                 List<AllocationResult> allocationResults) {

        logger.debug("start to seek bulk pick possibility for wave {}",
                waveNumber);
        // make sure the bulk pick is enabled
        BulkPickConfiguration bulkPickConfiguration =
                bulkPickConfigurationService.findByWarehouse(warehouseId);
        if (Objects.isNull(bulkPickConfiguration)) {
            logger.debug("Skip the bulk pick process as there's no configuration setup for bulk picking");
            return;
        }
        if (!Boolean.TRUE.equals(bulkPickConfiguration.getEnabledForOutbound())){
            // bulk pick is not enabled at the warehouse

            logger.debug("Skip the bulk pick process as it is disabled for the outbound process");
            return;
        }

        bulkPickService.groupPicksIntoBulk(
                waveNumber, allocationResults, bulkPickConfiguration.getPickSortDirection());


        logger.debug("complete bulk pick processing for wave {}",
                waveNumber);
    }

    private String getNextWaveNumber(Long warehouseId){
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "wave-number");
    }

    public Wave resetWaveStatus(Wave wave) {
        logger.debug("Start to check if we can complete the wave {}",
                wave.getNumber());
        boolean waveComplete = false;
        if (wave.getShipmentLines().isEmpty()) {
            waveComplete = true;
        }
        else {
            waveComplete = wave.getShipmentLines().stream().noneMatch(
                    shipmentLine -> !shipmentLine.getStatus().equals(ShipmentLineStatus.CANCELLED) &&
                            !shipmentLine.getStatus().equals(ShipmentLineStatus.DISPATCHED)
            );
        }
        if (waveComplete) {
            logger.debug("we are ready to complete wave {}",
                    wave.getNumber());
            wave.setStatus(WaveStatus.COMPLETED);
            return save(wave);
        }
        return wave;
    }

    public ReportHistory generateWavePickReport(Long waveId, String locale) {
        return generateWavePickReport(findById(waveId), locale);
}
    public ReportHistory generateWavePickReport(Wave wave, String locale){
        Long warehouseId = wave.getWarehouseId();


        Report reportData = new Report();
        setupWavePickReportParameters(
                reportData, wave
        );
        setupWavePickReportData(
                reportData, wave
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.WAVE_PICK_SHEET_BY_LOCATION, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }


    private void setupWavePickReportParameters(
            Report report, Wave wave) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("waveNumber", wave.getNumber());


    }

    private void setupWavePickReportData(Report report, Wave wave) {

        // set data to be all picks
        List<Pick> picks = pickService.findByWave(wave);

        // sort the pick so that we can group picks with same attribute from same location
        // together for manual bulk pick
        Collections.sort(picks, (a, b) -> {

            String inventoryAttributeA = new StringBuilder()
                    .append(a.getSourceLocation().getPickSequence()).append("-")
                    .append(a.getItemId()).append("-")
                    .append(Strings.isBlank(a.getStyle()) ? "____" : a.getStyle()).append("-")
                    .append(Strings.isBlank(a.getColor()) ? "____" : a.getColor()).append("-")
                    .append(Strings.isBlank(a.getProductSize()) ? "____" : a.getProductSize()).append("-")
                    .append(Strings.isBlank(a.getInventoryAttribute1()) ? "____" : a.getInventoryAttribute1()).append("-")
                    .append(Strings.isBlank(a.getInventoryAttribute2()) ? "____" : a.getInventoryAttribute2()).append("-")
                    .append(Strings.isBlank(a.getInventoryAttribute3()) ? "____" : a.getInventoryAttribute3()).append("-")
                    .append(Strings.isBlank(a.getInventoryAttribute4()) ? "____" : a.getInventoryAttribute4()).append("-")
                    .append(Strings.isBlank(a.getInventoryAttribute5()) ? "____" : a.getInventoryAttribute5()).append("-")
                    .toString();

            String inventoryAttributeB = new StringBuilder()
                    .append(b.getSourceLocation().getPickSequence()).append("-")
                    .append(b.getItemId()).append("-")
                    .append(Strings.isBlank(b.getStyle()) ? "____" : b.getStyle()).append("-")
                    .append(Strings.isBlank(b.getColor()) ? "____" : b.getColor()).append("-")
                    .append(Strings.isBlank(b.getProductSize()) ? "____" : b.getProductSize()).append("-")
                    .append(Strings.isBlank(b.getInventoryAttribute1()) ? "____" : b.getInventoryAttribute1()).append("-")
                    .append(Strings.isBlank(b.getInventoryAttribute2()) ? "____" : b.getInventoryAttribute2()).append("-")
                    .append(Strings.isBlank(b.getInventoryAttribute3()) ? "____" : b.getInventoryAttribute3()).append("-")
                    .append(Strings.isBlank(b.getInventoryAttribute4()) ? "____" : b.getInventoryAttribute4()).append("-")
                    .append(Strings.isBlank(b.getInventoryAttribute5()) ? "____" : b.getInventoryAttribute5()).append("-")
                    .toString();
            return inventoryAttributeA.compareTo(inventoryAttributeB);
        });

        // whether we add an empty line between pick groups
        // pick groups are group of picks that can be bulk picked
        boolean addSummeryLineBetweenPickGroup = true;

        // map to save the case quantity of the inventory form the
        // location
        // key: locationID-ItemId-style-color-productSize-inventoryAttribute1 - 5
        // value: case quantity of the inventory in the location. If the location is mixed
        //       of inventory with the item, then show MIXED
        Map<String, String> quantityPerCaseMap = new HashMap<>();
        Map<String, String> quantityPerPackMap = new HashMap<>();
        Map<String, Long> totalQuantityMap = new HashMap<>();

        double totalCaseQuantity = 0.0;
        long totalUnitQuantity = 0l;

        // decimal format to format the case quantity in case of picking partial cases
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        List<Pick> results = new ArrayList<>();
        String lastKey = "";
        // Setup display field
        for (Pick pick : picks) {
            totalUnitQuantity += pick.getQuantity();
            // set the inventory attribute in one string
            StringBuilder inventoryAttribute = new StringBuilder()
                    .append(pick.getSourceLocationId()).append("-")
                    .append(pick.getItemId()).append("-")
                    .append(Strings.isBlank(pick.getStyle()) ? "____" : pick.getStyle()).append("-")
                    .append(Strings.isBlank(pick.getColor()) ? "____" : pick.getColor()).append("-")
                    .append(Strings.isBlank(pick.getProductSize()) ? "____" : pick.getProductSize()).append("-")
                    .append(Strings.isBlank(pick.getInventoryAttribute1()) ? "____" : pick.getInventoryAttribute1()).append("-")
                    .append(Strings.isBlank(pick.getInventoryAttribute2()) ? "____" : pick.getInventoryAttribute2()).append("-")
                    .append(Strings.isBlank(pick.getInventoryAttribute3()) ? "____" : pick.getInventoryAttribute3()).append("-")
                    .append(Strings.isBlank(pick.getInventoryAttribute4()) ? "____" : pick.getInventoryAttribute4()).append("-")
                    .append(Strings.isBlank(pick.getInventoryAttribute5()) ? "____" : pick.getInventoryAttribute5()).append("-");

            // get the value of quantity per case by the pickable inventory from the source location
            // and save it temporary
            String key = inventoryAttribute.toString();
            // used to check if we just start with a new pick group. If so, we may want to
            // add a empty line between different pick groups so that to make the picker clear
            if (Strings.isBlank(lastKey)) {
                lastKey = key;
            }
            else if (!lastKey.equals(key)) {
                logger.debug("lastkey: {}, key: {}" ,
                        lastKey, key);
                // we just started a new group
                Long totalQuantity = totalQuantityMap.getOrDefault(lastKey, 0l);

                if (addSummeryLineBetweenPickGroup) {
                    // everytime we started a new pick group, add a empty line
                    Pick summeryPick = getSummeryPickDataForWavePickSheet(totalQuantity,
                            quantityPerCaseMap.getOrDefault(lastKey, ""),
                            quantityPerPackMap.getOrDefault(lastKey, ""),
                            decimalFormat
                            );

                    results.add(summeryPick);
                    // emptyPick.setQuantity(0l);

                    lastKey = key;
                }
            }
            Long quantity = totalQuantityMap.getOrDefault(key, 0l);
            totalQuantityMap.put(key, quantity + pick.getQuantity());


            String quantityPerCase = quantityPerCaseMap.getOrDefault(key, "");
            String quantityPerPack = quantityPerPackMap.getOrDefault(key, "");

            if (Strings.isBlank(quantityPerCase) || Strings.isBlank(quantityPerPack)) {
                // get the case and pack quantity from the inventory

                Pair<String, String> casePackQuantity
                        = getQuantityPerCaseAndQuantityPerPackForWavePickSheet(pick.getItemId(), pick.getInventoryStatusId(), pick.getSourceLocationId(),
                        pick.getColor(), pick.getProductSize(),
                        pick.getStyle(),
                        pick.getInventoryAttribute1(),
                        pick.getInventoryAttribute2(),
                        pick.getInventoryAttribute3(),
                        pick.getInventoryAttribute4(),
                        pick.getInventoryAttribute5());

                quantityPerCase = casePackQuantity.getFirst();
                quantityPerPack = casePackQuantity.getSecond();

            }
            quantityPerCaseMap.put(key, quantityPerCase);
            quantityPerPackMap.put(key, quantityPerPack);

            pick.setQuantityPerCase(quantityPerCase);
            pick.setQuantityPerPack(quantityPerPack);
            logger.debug("key {}'s quantity per case: {}, quantity per pack: {}",
                    key, quantityPerCase, quantityPerPack);
            int quantityPerCaseNumber = 0;
            int quantityPerPackNumber = 0;
            try {

                quantityPerCaseNumber = Integer.parseInt(quantityPerCase);
                if (pick.getQuantity() % quantityPerCaseNumber == 0) {

                    pick.setCaseQuantity(String.valueOf(pick.getQuantity() / quantityPerCaseNumber));
                } else {

                    pick.setCaseQuantity(decimalFormat.format(pick.getQuantity() * 1.0 / Integer.parseInt(quantityPerCase)));
                }
                totalCaseQuantity += (pick.getQuantity() * 1.0 / quantityPerCaseNumber);
            } catch (NumberFormatException ex) {
                // if we can't pass the quantityPerCase to number, it normally means the location is
                // mixed of different case quantity , so we can't calculate how many cases we will need
                // for the pick
                pick.setCaseQuantity("");
            }
            try {

                quantityPerPackNumber = Integer.parseInt(quantityPerPack);
                if (pick.getQuantity() % quantityPerPackNumber == 0) {

                    pick.setPackQuantity(String.valueOf(pick.getQuantity() / quantityPerPackNumber));
                } else {

                    pick.setPackQuantity(decimalFormat.format(pick.getQuantity() * 1.0 / Integer.parseInt(quantityPerPack)));
                }
                logger.debug("pick {}'s pack quantity is setup to {}",
                        pick.getNumber(), pick.getPackQuantity());

            } catch (NumberFormatException ex) {
                // if we can't pass the quantityPerPack to number, it normally means the location is
                // mixed of different Pack quantity , so we can't calculate how many packs we will need
                // for the pick
                pick.setPackQuantity("");
            }

            if (quantityPerCaseNumber > 0 && quantityPerPackNumber > 0) {

                if (quantityPerCaseNumber % quantityPerPackNumber == 0) {

                    pick.setPackPerCase(String.valueOf(quantityPerCaseNumber / quantityPerPackNumber));
                }
                else {
                    pick.setPackPerCase(decimalFormat.format(quantityPerCaseNumber * 1.0 / quantityPerPackNumber));
                }
            }
            results.add(pick);


        }
        // we may need to add a line for the last group
        if (Strings.isNotBlank(lastKey) && addSummeryLineBetweenPickGroup) {

            Long totalQuantity = totalQuantityMap.getOrDefault(lastKey, 0l);

            // everytime we started a new pick group, add a empty line
            Pick summeryPick = getSummeryPickDataForWavePickSheet(totalQuantity,
                    quantityPerCaseMap.getOrDefault(lastKey, ""),
                    quantityPerPackMap.getOrDefault(lastKey, ""),
                    decimalFormat
            );

            results.add(summeryPick);
        }


        report.setData(results);
        report.addParameter("totalCaseQuantity", decimalFormat.format(totalCaseQuantity));
        report.addParameter("totalUnitQuantity", totalUnitQuantity);
    }

    /**
     * Get the summeray of the picks from a list pick and fill in the report
     * @param totalQuantity
     * @param quantityPerCase
     * @param quantityPerPack
     * @return
     */
    private Pick getSummeryPickDataForWavePickSheet(Long totalQuantity, String quantityPerCase, String quantityPerPack,
                                                    DecimalFormat decimalFormat) {

        Pick summeryPick = new Pick();
        summeryPick.setSourceLocation(new Location());
        summeryPick.setQuantity(totalQuantity);
        try {

            int quantityPerCaseNumber = Integer.parseInt(quantityPerCase);
            if (totalQuantity % quantityPerCaseNumber == 0) {

                summeryPick.setCaseQuantity(String.valueOf(totalQuantity / quantityPerCaseNumber));
            } else {

                summeryPick.setCaseQuantity(decimalFormat.format(totalQuantity * 1.0 / quantityPerCaseNumber));
            }
        } catch (NumberFormatException ex) {
            // if we can't pass the quantityPerCase to number, it normally means the location is
            // mixed of different case quantity , so we can't calculate how many cases we will need
            // for the pick
            summeryPick.setCaseQuantity("");
        }
        try {

            int quantityPerPackNumber = Integer.parseInt(quantityPerPack);
            if (totalQuantity % quantityPerPackNumber == 0) {

                summeryPick.setPackQuantity(String.valueOf(totalQuantity / quantityPerPackNumber));
            } else {

                summeryPick.setPackQuantity(decimalFormat.format(totalQuantity * 1.0 / quantityPerPackNumber));
            }
        } catch (NumberFormatException ex) {
            // if we can't pass the quantityPerCase to number, it normally means the location is
            // mixed of different case quantity , so we can't calculate how many cases we will need
            // for the pick
            summeryPick.setPackQuantity("");
        }
        return summeryPick;
    }

    private Pair<String, String> getQuantityPerCaseAndQuantityPerPackForWavePickSheet(
            Long itemId, Long inventoryStatusId, Long sourceLocationId, String color, String productSize, String style,
            String inventoryAttribute1, String inventoryAttribute2, String inventoryAttribute3, String inventoryAttribute4,
            String inventoryAttribute5) {

        List<Inventory> inventoryList = inventoryServiceRestemplateClient.getPickableInventory(
                itemId, inventoryStatusId, sourceLocationId, color, productSize, style,
                inventoryAttribute1, inventoryAttribute2, inventoryAttribute3, inventoryAttribute4,
                inventoryAttribute5,
                "");
        logger.debug("Get a list of pickable inventory for printing wave pick sheet with item id {} and location id {}",
                itemId, sourceLocationId);

        if (inventoryList.isEmpty()) {
            return Pair.of("", "");
        }
        Long caseQuantity = 0l;
        Long packQuantity = 0l;

        String caseQuantityString = "";
        String packQuantityString = "";

        for (Inventory inventory : inventoryList) {
            logger.debug(inventory.toString());

            if (Objects.nonNull(inventory.getItemPackageType())){
                if (Objects.nonNull(inventory.getItemPackageType().getCaseItemUnitOfMeasure())) {
                    if (caseQuantity.equals(0l)) {
                        caseQuantity = inventory.getItemPackageType().getCaseItemUnitOfMeasure().getQuantity();
                        caseQuantityString = String.valueOf(caseQuantity);
                    } else if (!caseQuantity.equals(inventory.getItemPackageType().getCaseItemUnitOfMeasure().getQuantity())) {
                        caseQuantityString = "MIXED";
                    }
                }

                if (Objects.nonNull(inventory.getItemPackageType().getPackItemUnitOfMeasure())) {
                    logger.debug("item {} / {}, item package type {} / {}'s pack unit of measure is {}",
                            inventory.getItem().getId(),
                            inventory.getItem().getName(),
                            inventory.getItemPackageType().getId(),
                            inventory.getItemPackageType().getName(),
                            inventory.getItemPackageType().getPackItemUnitOfMeasure().getId());
                    if (packQuantity.equals(0l)) {
                        packQuantity = inventory.getItemPackageType().getPackItemUnitOfMeasure().getQuantity();
                        packQuantityString = String.valueOf(packQuantity);
                    } else if (!packQuantity.equals(inventory.getItemPackageType().getPackItemUnitOfMeasure().getQuantity())) {
                        packQuantityString = "MIXED";
                    }
                }
            }
        }
        return Pair.of(caseQuantityString, packQuantityString);
    }


    public ReportHistory generateWavePackingSlip(Long waveId, String locale) {
        return generateWavePackingSlip(findById(waveId), locale);
    }
    public ReportHistory generateWavePackingSlip(Wave wave, String locale){
        Long warehouseId = wave.getWarehouseId();


        Report reportData = new Report();
        setupWavePackingSlipParameters(
                reportData, wave
        );
        setupWavePackingSlipData(
                reportData, wave
        );

        logger.debug("will call resource service to print the Packing Slip report with locale: {}",
                locale);
        logger.debug("####  Packing Slip Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.WAVE_PACKING_SLIP, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }


    private void setupWavePackingSlipParameters(
            Report report, Wave wave) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("waveNumber", wave.getNumber());


    }

    public List<Inventory> getStagedInventory(Long id) {
        return getStagedInventory(findById(id));
    }
    private List<Inventory> getStagedInventory(Wave wave) {

        // set data to be all picks
        List<Pick> picks = pickService.findByWave(wave).stream().filter(
                pick -> pick.getPickedQuantity() > 0
        ).collect(Collectors.toList());

        if (picks.size() == 0) {
            return new ArrayList<>();
        }
        List<Inventory> pickedInventories
                = inventoryServiceRestemplateClient.getPickedInventory(wave.getWarehouseId(), picks);

        // only return the picked inventory that is already in stage
        return pickedInventories.stream()
                .filter(inventory -> inventory.getLocation().getLocationGroup().getLocationGroupType().getShippingStage())
                .collect(Collectors.toList());


    }
    private void setupWavePackingSlipData(Report report, Wave wave) {
        List<Inventory> stagedInventories = getStagedInventory(wave);
        if (stagedInventories.isEmpty()) {
            throw OrderOperationException.raiseException("There's nothing staged yet for wave " +
                    wave.getNumber() + ", please pick and stage first");
        }

        List<Pick> picks = pickService.findByWave(wave);
        // save the mapping between pick and order number so that we can show
        // order number in the packing slip report
        // KEY: pick id
        // value: order number
        Map<Long, String> pickOrderNumberMap = new HashMap<>();
        picks.forEach(
                pick -> pickOrderNumberMap.put(pick.getId(), pick.getOrderNumber())
        );

        // setup the order number so we can group and sum the quantity by order and attribute
        for (Inventory inventory : stagedInventories) {
            // setup the order number for display
            if (Objects.nonNull(inventory.getPickId())) {
                inventory.setOrderNumber(pickOrderNumberMap.getOrDefault(inventory.getPickId(), ""));
            }
        }

        // group the inventory quantity by order and inventory attribute
        Map<String, Inventory> groupedInventoryMap = new HashMap<>();
        for (Inventory inventory : stagedInventories) {

            String key = new StringBuilder()
                    .append(Strings.isBlank(inventory.getOrderNumber()) ? "____" : inventory.getOrderNumber()).append("-")
                    .append(getInventoryAttributeKey(inventory))
                    .toString();
            Inventory groupedInventory = groupedInventoryMap.getOrDefault(key, new Inventory());
            groupedInventory.copyAttribute(inventory);
            groupedInventory.setQuantity(groupedInventory.getQuantity() + inventory.getQuantity());
            groupedInventory.setCaseQuantity(groupedInventory.getCaseQuantity() + inventory.getCaseQuantity());
            groupedInventory.setPackQuantity(groupedInventory.getPackQuantity() + inventory.getPackQuantity());
            groupedInventory.setOrderNumber(inventory.getOrderNumber());

            groupedInventoryMap.put(key, groupedInventory);
        }
        List<Inventory> groupedInventoryList = new ArrayList<>(groupedInventoryMap.values());


        // sort the pick so that we can group inventory
        // picked   with same attribute
        Collections.sort(groupedInventoryList, (a, b) -> {

            String inventoryAttributeA = new StringBuilder()
                    .append(Strings.isBlank(a.getOrderNumber()) ? "____" : a.getOrderNumber()).append("-")
                    .append(getInventoryAttributeKey(a))
                    .toString();

            String inventoryAttributeB = new StringBuilder()
                    .append(Strings.isBlank(b.getOrderNumber()) ? "____" : b.getOrderNumber()).append("-")
                    .append(getInventoryAttributeKey(b))
                    .toString();
            return inventoryAttributeA.compareTo(inventoryAttributeB);
        });
        logger.debug("===== after sort, we have groupped inventory   =======");
        for (Inventory inventory : groupedInventoryList) {
            logger.debug("=> order = {}, po = {}, style = {}, color = {}, case quantity = {}",
                    inventory.getOrderNumber(),
                    inventory.getAttribute1(),
                    inventory.getStyle(),
                    inventory.getColor(),
                    inventory.getCaseQuantity());


        }


        // whether we add an empty line between pick groups
        // pick groups are group of picks that can be bulk picked
        boolean addSummeryLineBetweenPickGroup = true;

        // map to save the case quantity of the inventory form the
        // location
        // key: locationID-ItemId-style-color-productSize-inventoryAttribute1 - 5
        // value: case quantity of the inventory in the location. If the location is mixed
        //       of inventory with the item, then show MIXED
        Map<String, Long> totalQuantityMap = new HashMap<>();
        Map<String, Double> totalCaseQuantityMap = new HashMap<>();
        Map<String, Double> totalPackQuantityMap = new HashMap<>();

        double totalCaseQuantity = 0.0;
        double totalPackQuantity = 0.0;
        long totalUnitQuantity = 0l;

        // decimal format to format the case quantity in case of picking partial cases
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        List<Inventory> results = new ArrayList<>();
        String lastKey = "";
        // Setup display field
        for (Inventory inventory : groupedInventoryList) {
            totalUnitQuantity += inventory.getQuantity();
            totalCaseQuantity += inventory.getCaseQuantity();
            // setup the order number for display
            if (Objects.nonNull(inventory.getPickId())) {
                inventory.setOrderNumber(pickOrderNumberMap.getOrDefault(inventory.getPickId(), ""));
            }
            // set the inventory attribute in one string
            StringBuilder inventoryAttribute = getInventoryAttributeKey(inventory);

            // get the value of quantity per case by the inventory from
            String key = inventoryAttribute.toString();
            // used to check if we just start with a new pick group. If so, we may want to
            // add a empty line between different pick groups so that to make the picker clear
            if (Strings.isBlank(lastKey)) {
                lastKey = key;
            }
            else if (!lastKey.equals(key)) {
                logger.debug("lastkey: {}, key: {}" ,
                        lastKey, key);
                // we just started a new group
                Long quantity = totalQuantityMap.getOrDefault(lastKey, 0l);
                Double caseQuantity = totalCaseQuantityMap.getOrDefault(lastKey, 0.0);
                Double packQuantity = totalPackQuantityMap.getOrDefault(lastKey, 0.0);

                if (addSummeryLineBetweenPickGroup) {
                    // everytime we started a new pick group, add a empty line
                    Inventory summeryInventory = new Inventory();
                    summeryInventory.setQuantity(quantity);
                    summeryInventory.setCaseQuantity(caseQuantity);
                    summeryInventory.setPackQuantity(packQuantity);
                    summeryInventory.setItemPackageType(new ItemPackageType());

                    results.add(summeryInventory);

                    lastKey = key;
                }
            }
            Long quantity = totalQuantityMap.getOrDefault(key, 0l);
            Double caseQuantity = totalCaseQuantityMap.getOrDefault(key, 0.0);
            Double packQuantity = totalPackQuantityMap.getOrDefault(key, 0.0);

            totalQuantityMap.put(key, quantity + inventory.getQuantity());
            totalCaseQuantityMap.put(key, caseQuantity + inventory.getCaseQuantity());
            totalPackQuantityMap.put(key, packQuantity + inventory.getPackQuantity());

            results.add(inventory);


        }
        // we may need to add a line for the last group
        if (Strings.isNotBlank(lastKey) && addSummeryLineBetweenPickGroup) {

            Long quantity = totalQuantityMap.getOrDefault(lastKey, 0l);
            Double caseQuantity = totalCaseQuantityMap.getOrDefault(lastKey, 0.0);
            Double packQuantity = totalPackQuantityMap.getOrDefault(lastKey, 0.0);

            Inventory summeryInventory = new Inventory();
            summeryInventory.setQuantity(quantity);
            summeryInventory.setCaseQuantity(caseQuantity);
            summeryInventory.setPackQuantity(packQuantity);
            summeryInventory.setItemPackageType(new ItemPackageType());

            results.add(summeryInventory);
        }


        report.setData(results);

        // add statistics data as well
        report.addParameter("totalCaseQuantity", decimalFormat.format(totalCaseQuantity));
        report.addParameter("totalUnitQuantity", totalUnitQuantity);
    }


    private StringBuilder getInventoryAttributeKey(Inventory inventory) {

        return new StringBuilder()
                .append(Strings.isBlank(inventory.getStyle()) ? "____" : inventory.getStyle()).append("-")
                .append(Strings.isBlank(inventory.getColor()) ? "____" : inventory.getColor()).append("-")
                .append(Strings.isBlank(inventory.getProductSize()) ? "____" : inventory.getProductSize()).append("-")
                .append(Strings.isBlank(inventory.getAttribute1()) ? "____" : inventory.getAttribute1()).append("-")
                .append(Strings.isBlank(inventory.getAttribute2()) ? "____" : inventory.getAttribute2()).append("-")
                .append(Strings.isBlank(inventory.getAttribute3()) ? "____" : inventory.getAttribute3()).append("-")
                .append(Strings.isBlank(inventory.getAttribute4()) ? "____" : inventory.getAttribute4()).append("-")
                .append(Strings.isBlank(inventory.getAttribute5()) ? "____" : inventory.getAttribute5()).append("-");
    }

    public Wave completeWave(Long id) {
        return completeWave(findById(id));
    }
    public Wave completeWave(Wave wave) {
        validateWaveReadyForComplete(wave);
        // check if we will need to automatically complete the shipment
        // and orders that attached to this wave.
        // in some simple scenario when there's no outbund truck, we allow
        // the user to complete all orders and shipments in the wave
        OutboundConfiguration outboundConfiguration =
                outboundConfigurationService.findByWarehouse(wave.getWarehouseId());
        if (Objects.nonNull(outboundConfiguration) &&
            Boolean.TRUE.equals(outboundConfiguration.getCompleteOrderAndShipmentWhenCompleteWave())) {
            completeOrderAndShipmentWhenCompleteWave(wave);
        }
        // after we complete the orders. let's complete the wave
        wave.setStatus(WaveStatus.COMPLETED);
        return save(wave);
    }

    private void completeOrderAndShipmentWhenCompleteWave(Wave wave) {

        logger.debug("start to complete orders and shipment when we complete the wave {}",
                wave.getNumber());
        Set<Long> orderIds = wave.getShipmentLines().stream().map(
                shipmentLine -> shipmentLine.getOrderLine().getOrder().getId()
        ).collect(Collectors.toSet());

        // see if the order is ready for complete only if all the order
        // lines are in the wave

        Set<Long> validOrders = orderIds.stream().filter(
                orderId -> orderLineAllInWave(orderId, wave)
        ).collect(Collectors.toSet());


        validOrders.forEach(
                orderId -> {
                    Order orderToBeComplete = orderService.findById(orderId);
                    logger.debug("start to complete order {} when we complete the wave {}",
                            orderToBeComplete.getNumber(), wave.getNumber());
                    // complete order should complete the shipment that attached to it
                    // TO-DO: when we have multiple shipment
                    orderService.completeOrder(orderId, orderToBeComplete);
                }
        );
    }

    /**
     * Check if we can complete the wave
     * @param wave
     */
    private void validateWaveReadyForComplete(Wave wave) {
        List<Pick> picks = pickService.findByWave(wave);
        // make sure all the picks are done
        if (picks.stream().anyMatch(pick -> pick.getQuantity() > pick.getPickedQuantity())) {
            throw OrderOperationException.raiseException("can't complete wave " + wave.getNumber() +
                    " as there's still open pick");
        }
        List<ShortAllocation> shortAllocations = shortAllocationService.findByWave(wave);
        // make sure all the picks are done
        if (!shortAllocations.isEmpty()) {
            throw OrderOperationException.raiseException("can't complete wave " + wave.getNumber() +
                    " as there's still short allocation");
        }
    }

    private boolean orderLineAllInWave(Long orderId, Wave wave) {
        Order order = orderService.findById(orderId);
        for (OrderLine orderLine : order.getOrderLines()) {
            boolean orderLineInWave = false;
            for (ShipmentLine shipmentLine : wave.getShipmentLines()) {
                if (orderLine.getId().equals(shipmentLine.getOrderLineId())) {
                    orderLineInWave = true;
                }
            }
            if (!orderLineInWave) {
                logger.debug("Order {}, order line {} is not wave {}",
                        order.getNumber(), orderLine.getNumber(), wave.getNumber());
                return false;
            }
        }
        return true;
    }

    public Wave deassignShipmentLine(Long id, Long shipmentLineId) {
        return deassignShipmentLine(findById(id), shipmentLineId);
    }

    /**
     * Deassign a shipment from the wave
     * @param wave
     * @param shipmentLineId
     * @return
     */
    public Wave deassignShipmentLine(Wave wave, Long shipmentLineId) {

        shipmentLineService.deassignShipmentLineFromWave(wave, shipmentLineId);

        return findById(wave.getId());

    }
}

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

    public List<Order> findWaveCandidate(Long warehouseId, String orderNumber,
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

        return orderService.findWavableOrders(warehouseId, orderNumber, clientId,
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

}

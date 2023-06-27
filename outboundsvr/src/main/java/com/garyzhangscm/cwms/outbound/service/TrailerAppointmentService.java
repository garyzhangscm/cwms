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
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
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
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class TrailerAppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerAppointmentService.class);


    @Autowired
    private StopService stopService;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private WaveService waveService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private BulkPickConfigurationService bulkPickConfigurationService;
    @Autowired
    private BulkPickService bulkPickService;
    @Autowired
    private PickListService pickListService;
    @Autowired
    private PickReleaseService pickReleaseService;
    @Autowired
    private PickService pickService;

    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> shippingTrailerAppointmentFileUploadProgressMap = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> shippingTrailerAppointmentFileUploadResultMap = new ConcurrentHashMap<>();

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public TrailerAppointment assignStopShipmentOrdersToTrailerAppointment(Long trailerAppointmentId,
                                                                           String stopIdList,
                                                                           String shipmentIdList,
                                                                           String orderIdList) {
        TrailerAppointment trailerAppointment =
                commonServiceRestemplateClient.getTrailerAppointmentById(trailerAppointmentId);
        if (Objects.isNull(trailerAppointment)) {
           throw ResourceNotFoundException.raiseException("trailer appointment not found by id: " + trailerAppointmentId);
        }
        logger.debug("We get trailer appointment {}, by id {}",
                trailerAppointment.getNumber(), trailerAppointment.getId());
        if (Strings.isNotBlank(stopIdList)) {
            assignStopsToTrailerAppointment(trailerAppointment, stopIdList);
        }
        if (Strings.isNotBlank(shipmentIdList)) {
            assignShipmentsToTrailerAppointment(trailerAppointment, shipmentIdList);
        }
        if (Strings.isNotBlank(orderIdList)) {
            assignOrdersToTrailerAppointment(trailerAppointment, orderIdList);
        }

        return trailerAppointment;
    }


    private void assignOrdersToTrailerAppointment(TrailerAppointment trailerAppointment, String orderIdList) {
        logger.debug("Start to assign trailer appointment {} to orders with id {}",
                trailerAppointment.getNumber(), orderIdList);
        Arrays.stream(orderIdList.split(",")).forEach(
                orderId -> {
                    orderService.assignTrailerAppointment(Long.parseLong(orderId), trailerAppointment);
                }
        );
    }

    private void assignShipmentsToTrailerAppointment(TrailerAppointment trailerAppointment, String shipmentIdList) {
        logger.debug("Start to assign trailer appointment {} to shipment with id {}",
                trailerAppointment.getNumber(), shipmentIdList);

        Arrays.stream(shipmentIdList.split(",")).forEach(
                shipmentId -> {
                    shipmentService.assignTrailerAppointment(Long.parseLong(shipmentId), trailerAppointment);
                }
        );
    }

    private void assignStopsToTrailerAppointment(TrailerAppointment trailerAppointment, String stopIdList) {
        logger.debug("Start to assign trailer appointment {} to stop with id {}",
                trailerAppointment.getNumber(), stopIdList);
        Arrays.stream(stopIdList.split(",")).forEach(
                stopId -> {
                    stopService.assignTrailerAppointment(Long.parseLong(stopId), trailerAppointment);
                }
        );
    }

    /**
     * Process trailer appointment integration
     * @param trailerAppointment
     */
    public void processIntegration(TrailerAppointment trailerAppointment) {

        // first let's create a wave with the trailer appointment's number
        // we will only have one wave for the entire trailer appointment
        initTrailerAppointment(trailerAppointment);

    }

    public void initTrailerAppointment(TrailerAppointment trailerAppointment) {

        // first let's create a wave with the trailer appointment's number
        // we will only have one wave for the entire trailer appointment
        logger.debug("Start to initial trailer appointment, we should already have the trailer appointment" +
                " created via common service. we will create stop and shipment in this transaction");

        Wave wave = waveService.createWave(
                trailerAppointment.getWarehouseId(),
                trailerAppointment.getNumber());

        // second, let's plan a shipment for each order in the stops

        for (Stop stop : trailerAppointment.getStops()) {
            stopService.processIntegration(wave, stop, trailerAppointment.getId());

        }

    }


    /**
     * Allocate the shipments in the trailer appointment
     * @param id
     */
    public TrailerAppointment allocateTrailerAppointment(Long warehouseId, Long id) {
        // get the stops from the trailer appointment by sequence and
        // allocate stop by stop
        List<Stop> stops = stopService.findAll(warehouseId, null, id, null, null, null )
                .stream().filter(stop -> stop.getStatus() != StopStatus.CANCELLED && stop.getStatus() != StopStatus.COMPLETED)
                .collect(Collectors.toList());

        if (stops.isEmpty()) {
            // all stops are completed / cancelled, let's complete this trailer appointment
            logger.debug("all stops for this trailer appointment is either completed or cancelled, " +
                    "let's just mark this trailer appointment as in process");
            return commonServiceRestemplateClient.changeTrailerAppointmentStatus(id, TrailerAppointmentStatus.INPROCESS);
        }

        Collections.sort(stops, Comparator.comparing(Stop::getSequence));

        List<AllocationResult> allocationResults = new ArrayList<>();
        stops.forEach(stop ->  {
            try {
                allocationResults.addAll(stopService.allocateStop(stop));
            }
            catch(Exception ex) {
                ex.printStackTrace();
                // ignore any exception
            }
        });

        // post allocation process
        // 1. bulk pick
        // 2. list pick
        // 3. release picks into work task
        TrailerAppointment trailerAppointment = commonServiceRestemplateClient.getTrailerAppointmentById(id);
        postAllocationProcess(warehouseId,
                trailerAppointment.getNumber(),  allocationResults);


        // set the trailer appointment's status to in process
        return commonServiceRestemplateClient.changeTrailerAppointmentStatus(id, TrailerAppointmentStatus.INPROCESS);
    }


    /**
     * Post allocation process
     * 1. bulk pick
     * 2. list pick
     * @param allocationResults
     */
    private void postAllocationProcess(Long warehouseId,
                                       String trailerAppointmentNumber,
                                       List<AllocationResult> allocationResults) {
        // we will always try bulk pick first
        requestBulkPick(warehouseId, trailerAppointmentNumber, allocationResults);

        // for anything that not fall in the bulk pick, see if we can group them into
        // a list pick
        processListPick(warehouseId, trailerAppointmentNumber, allocationResults);

        releaseSinglePicks(allocationResults);

    }
    /**
     * Group all the picks into bulk pick, if possible
     * @param allocationResults
     */
    private void requestBulkPick(Long warehouseId,
                                 String trailerAppointmentNumber,
                                 List<AllocationResult> allocationResults) {

        logger.debug("start to seek bulk pick possibility for trailer appointment {}",
                trailerAppointmentNumber);
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
                trailerAppointmentNumber, allocationResults, bulkPickConfiguration.getPickSortDirection());


        logger.debug("complete bulk pick processing for trailer appointment {}",
                trailerAppointmentNumber);
    }

    /**
     * Find picks from the same wave and group them together into the same list
     * @param warehouseId
     * @param trailerAppointmentNumber
     */
    private void processListPick(Long warehouseId, String trailerAppointmentNumber, List<AllocationResult> allocationResults) {
        logger.debug("start to process pick list for trailer appointment {}", trailerAppointmentNumber);
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
     * @param allocationResults
     */
    private void releaseSinglePicks(List<AllocationResult> allocationResults) {
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

    public TrailerAppointment completeTrailerAppointment(Long warehouseId, Long id) {

        List<Stop> stops = stopService.findAll(warehouseId, null, id, null, null,null )
                .stream().filter(stop -> stop.getStatus() != StopStatus.CANCELLED && stop.getStatus() != StopStatus.COMPLETED)
                .collect(Collectors.toList());
        if (stops.isEmpty()) {
            // all stops are completed / cancelled, let's complete this trailer appointment
            logger.debug("all stops for this trailer appointment is either completed or cancelled, " +
                    "let's complete this trailer appointment");
            return commonServiceRestemplateClient.changeTrailerAppointmentStatus(id, TrailerAppointmentStatus.COMPLETED);
        }
        // the trailer appointment is ready for complete when
        // all the shipment in the trailer is ready for complete

        validateTrailerAppointmentReadyForComplete(stops);

        TrailerAppointment trailerAppointment =
                commonServiceRestemplateClient.getTrailerAppointmentById(id);

        // let's complete all the stops
        for (Stop stop : stops) {
            stop.setTrailerAppointment(trailerAppointment);
            stopService.completeStop(stop);
        }


        // set the trailer appointment's status to in process
        return commonServiceRestemplateClient.changeTrailerAppointmentStatus(id, TrailerAppointmentStatus.COMPLETED);


    }

    private void validateTrailerAppointmentReadyForComplete(List<Stop> stops) {
        for (Stop stop : stops) {
            for (Shipment shipment : stop.getShipments()) {
                if (!shipmentService.validateShipmentReadyForComplete(shipment)) {
                    throw ShippingException.raiseException("Shipment " + shipment.getNumber() +
                            " is not ready for complete yet" +
                            " please check if there's open pick and short allocation");
                }
            }
        }
    }
    private void clearFileUploadMap() {

        if (shippingTrailerAppointmentFileUploadProgressMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = shippingTrailerAppointmentFileUploadProgressMap.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (shippingTrailerAppointmentFileUploadResultMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = shippingTrailerAppointmentFileUploadResultMap.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }


    public double getShippingTrailerAppoitnmentFileUploadProgress(String key) {
        return shippingTrailerAppointmentFileUploadProgressMap.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getShippingTrailerAppointmentFileUploadResult(Long warehouseId, String key) {
        return shippingTrailerAppointmentFileUploadResultMap.getOrDefault(key, new ArrayList<>());
    }


    private List<OrderLineCSVWrapper> loadDataWithLine(File file) throws IOException {


        // return fileService.loadData(file, getCsvSchemaWithLine(), OrderLineCSVWrapper.class);
        return fileService.loadData(file, OrderLineCSVWrapper.class);
    }
    public String saveShippingTrailerAppointmentData(Long warehouseId,
                                File localFile) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();

        shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 0.0);
        shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());

        List<OrderLineCSVWrapper> orderLineCSVWrappers = loadDataWithLine(localFile);

        logger.debug("start to save {} order lines ", orderLineCSVWrappers.size());

        shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {
            int totalCount = orderLineCSVWrappers.size();
            int index = 0;
            // start to build the trailer appointment structure
            // 1. create the order line
            // 2. build the trailer appointment.
            // 3. setup the trailer appointment / stop / shipment

            // a map of trailer app
            // key: trailer appointment number
            // value: trailer appointment
            Map<String, TrailerAppointment> trailerAppointmentMap = new HashMap<>();

            // Step 1: create the order line and build into the trailer appointment MAP
            for (OrderLineCSVWrapper orderLineCSVWrapper : orderLineCSVWrappers) {

                try {
                    // see if we already have the trailer appointment
                    TrailerAppointment trailerAppointment = null;
                    if (trailerAppointmentMap.containsKey(orderLineCSVWrapper.getLoad())) {
                        trailerAppointment = trailerAppointmentMap.get(orderLineCSVWrapper.getLoad());
                    }
                    else {
                        trailerAppointment =
                                commonServiceRestemplateClient.getTrailerAppointmentByNumber(warehouseId, orderLineCSVWrapper.getLoad());
                        if (Objects.isNull(trailerAppointment)) {
                            trailerAppointment = createTrailerAppointment(warehouseId, orderLineCSVWrapper.getLoad());
                        }
                    }

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));
                    Client client = Strings.isNotBlank(orderLineCSVWrapper.getClient()) ?
                            commonServiceRestemplateClient.getClientByName(warehouseId, orderLineCSVWrapper.getClient())
                            : null;
                    Long clientId = Objects.isNull(client) ? null : client.getId();

                    Order order = orderService.findByNumber(warehouseId, clientId, orderLineCSVWrapper.getOrder());
                    if (Objects.isNull(order)) {
                        logger.debug("order {} is not created yet, let's create the order on the fly ", orderLineCSVWrapper.getOrder());
                        // the order is not created yet, let's

                        // we have to do it manually since the user name is only available in the main http session
                        // but we will create the receipt / receipt line in a separate transaction
                        order = orderService.convertFromWrapper(warehouseId, orderLineCSVWrapper);
                        order.setCreatedBy(username);
                        order = orderService.saveOrUpdate(order);
                    }
                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 90.0 +  (70.0 / totalCount) * (index + 0.5));
                    logger.debug("start to create order line {} for item {}, quantity {}, for order {}",
                            orderLineCSVWrapper.getLine(),
                            orderLineCSVWrapper.getItem(),
                            orderLineCSVWrapper.getExpectedQuantity(),
                            order.getNumber());
                    OrderLine newOrderLine = orderLineService.saveOrderLineData(warehouseId, clientId, order, orderLineCSVWrapper);

                    attachOrderLineToTrailerAppointment(warehouseId, trailerAppointment,
                            orderLineCSVWrapper.getStopSequence(), newOrderLine);

                    trailerAppointmentMap.put(orderLineCSVWrapper.getLoad(), trailerAppointment);

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 90.0 +  (70.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = shippingTrailerAppointmentFileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            orderLineCSVWrapper.toString(),
                            "success", ""
                    ));
                    shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receiving order upload file record: {}, \n error message: {}",
                            orderLineCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = shippingTrailerAppointmentFileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            orderLineCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
            }

        }).start();

        return fileUploadProgressKey;

    }

    private TrailerAppointment createTrailerAppointment(Long warehouseId, String load) {
        return commonServiceRestemplateClient.createTrailerAppointment(warehouseId, load);
    }

    private void attachOrderLineToTrailerAppointment(Long warehouseId,
                                                     TrailerAppointment trailerAppointment,
                                                     Integer stopSequence,
                                                     String orderNumber,
                                                     String orderLineNumber) {

        OrderLine orderLine = orderLineService.findByNumber(warehouseId,
                orderNumber, orderLineNumber);

        attachOrderLineToTrailerAppointment(warehouseId, trailerAppointment,
                stopSequence, orderLine);
    }
    private void attachOrderLineToTrailerAppointment(Long warehouseId,
                                                         TrailerAppointment trailerAppointment,
                                                         Integer stopSequence,
                                                     OrderLine orderLine) {

        // make sure the line is not build into an active shipment line yet


        if (orderLine.getOrder().getStatus().equals(OrderStatus.COMPLETE)) {

            throw OrderOperationException.raiseException("order " + orderLine.getOrder().getNumber() + " is already completed, " +
                    "can't further process this order");
        }
        // get the open shipment for the order line. If there're multiple open shipments, then
        // raise an error as we don't want to have multiple shipments group into the
        // same trailer

        List<ShipmentLine> shipmentLines = orderLine.getShipmentLines().stream()
                .filter(shipmentLine ->
                        !shipmentLine.getStatus().equals(ShipmentLineStatus.CANCELLED) &&
                                !shipmentLine.getStatus().equals(ShipmentLineStatus.DISPATCHED))
                .collect(Collectors.toList());
        if (shipmentLines.size() > 1) {

            throw OrderOperationException.raiseException("order " + orderLine.getOrder().getNumber() + " has multiple shipment assigned, " +
                    "can't further process this order");
        }
        ShipmentLine shipmentLine = shipmentLines.size() == 1? shipmentLines.get(0) : null;
        if(Objects.isNull(shipmentLine)) {
            // there's no shipment line yet
            // 1. if there's shipment for the same order in the same trailer, and the shipment
            //    has the same load sequence, then build the order line into the same shipment
            // 2. otherwise, build the order line into a new shipment
            Shipment matchedShipment = findMatchedShipmentForNewOrderInTrailerAppointment(
                    trailerAppointment, stopSequence, orderLine
            );
            if (Objects.isNull(matchedShipment)) {
                // OK, there's no matched shipment, let's plan a shipment for the order line , and then
                // build it into the trailer appointment
                List<Shipment> shipments = shipmentService.planShipments(warehouseId, List.of(orderLine));
                if (shipments.size() != 1) {

                    throw OrderOperationException.raiseException("fail to plan a new shipment for order " + orderLine.getOrder().getNumber()  +
                            "can't further process this order");
                }
                matchedShipment = shipments.get(0);
                // build the shipment into the trailer
                attachShipmentToTrailerAppointment(matchedShipment, trailerAppointment, stopSequence);
            }
            else {
                // ok, we find a shipment that match with the order line and is in the current
                // trailer appointment, we will just need to build the order line into the same shipment
                shipmentLine = shipmentService.addOrderLine(matchedShipment, orderLine);
                // we may need to add the shipment line to the shipment
                matchedShipment.getShipmentLines().add(shipmentLine);
            }
        }
        else {
            // ok, there's shipment line planned for the order line, attached the shipment to the current trailer
            attachShipmentToTrailerAppointment(shipmentLine.getShipment(), trailerAppointment, stopSequence);
        }

    }

    /**
     * When we want to build a order line into a trailer appointment, we will first check
     * if there's existing shipment that
     * 1. has the same stop sequence
     * 2. has the same ship to address
     * 3. the shipment is built for the same order as the line
     * @param trailerAppointment
     * @param stopSequence
     * @param orderLine
     * @return
     */
    private Shipment findMatchedShipmentForNewOrderInTrailerAppointment(TrailerAppointment trailerAppointment,
                                                                        int stopSequence,
                                                                        OrderLine orderLine) {

        for (Stop stop : trailerAppointment.getStops()) {
            if (stop.getSequence() != stopSequence) {
                // current order's load sequence doesn't match the exists load
                // we can't group the order into this stop
                continue;
            }
            if (!stop.orderValidForStop(orderLine.getOrder())) {
                continue;
            }
            // ok, the stop is valid for the order, see if there's shipment
            // in the stop that is for the same order

            for (Shipment shipment : stop.getShipments()) {
                // if the shipment has lines from other order, we will not group the current order
                // into this shipment as we don't want to group same orders into different
                boolean shipmentForDifferentOrder = false;
                for (ShipmentLine existingShipmentLine : shipment.getShipmentLines()) {
                    if (!existingShipmentLine.getOrderLine().getOrder().equals(orderLine.getOrder())) {
                        shipmentForDifferentOrder = true;
                    }
                }

                // OK, we didn't find shipment for this order line, then
                // see if we can group the order into the shipment
                logger.debug("The shipment {} has shipment from different order ? {}",
                        shipment.getNumber(), shipmentForDifferentOrder);
                if (!shipmentForDifferentOrder) {
                    return shipment;
                }
            }
        }
        return null;
    }

    private void attachShipmentToTrailerAppointment(Shipment shipment,
                                                    TrailerAppointment trailerAppointment,
                                                    Integer stopSequence) {
        // see if the shipment already have a stop
        if (Objects.nonNull(shipment.getStop()) && Objects.nonNull(shipment.getStop().getTrailerAppointmentId())) {
            // ok, the shipment already have a stop, validate the stop to make sure
            // 1. it is in the same trailer appointment
            // 2. it has the right stop sequence
            if (!trailerAppointment.getId().equals(shipment.getStop().getTrailerAppointmentId())) {

                throw OrderOperationException.raiseException("fail to assign the shipment " + shipment.getNumber() +
                                " to the trailer "  + trailerAppointment.getNumber() +
                        "as the shipment already assigned to a different trailer appointment with id " +
                                shipment.getStop().getTrailerAppointmentId());
            }
            if (!shipment.getStop().getSequence().equals(stopSequence)){

                throw OrderOperationException.raiseException("fail to assign the shipment " + shipment.getNumber() +
                        " to the trailer "  + trailerAppointment.getNumber() +
                        "as the shipment has a different stop sequence " + shipment.getStop().getSequence() +
                        "than the required one " + stopSequence);
            }
            // the shipment is already assigned to the trailer, do nothing
        }
        else if (Objects.nonNull(shipment.getStop())) {
            // if we are here, we know the shipment has a stop but the stop doesn't assigned to any trailer appointment yet
            // let's assign to the current trailer appointment
            Stop stop = shipment.getStop();
            stop =  stopService.assignTrailerAppointment(stop, trailerAppointment,
                    stopSequence);
            trailerAppointment.getStops().add(stop);

        }
        else {
            // ok, if we are here, we know the shipment has not been built into any stop yet, let's
            // create a stop for the shipment and then assign the stop to the trailer
            Stop stop = stopService.createStop(shipment.getWarehouseId(),
                    stopSequence,
                    shipment.getShipToContactorFirstname(),
                    shipment.getShipToContactorLastname(),
                    shipment.getShipToAddressCountry(),
                    shipment.getShipToAddressState(),
                    shipment.getShipToAddressCounty(),
                    shipment.getShipToAddressCity(),
                    shipment.getShipToAddressDistrict(),
                    shipment.getShipToAddressLine1(),
                    shipment.getShipToAddressLine2(),
                    shipment.getShipToAddressPostcode());
            logger.debug("stop {} / {} is created for shipment {} / {}",
                    stop.getId(), stop.getNumber(),
                    shipment.getId(), shipment.getNumber());

            // assign the shipment to the stop
            shipment = shipmentService.assignShipmentToStop(stop, shipment);

            logger.debug("shipment {} / {} is assigned to stop {} / {} ",
                    shipment.getId(), shipment.getNumber(),
                    shipment.getStop().getId(), shipment.getStop().getNumber());
            stop.getShipments().add(shipment);

            stop = stopService.assignTrailerAppointment(stop, trailerAppointment,
                    stopSequence);
            trailerAppointment.getStops().add(stop);
        }


    }
}

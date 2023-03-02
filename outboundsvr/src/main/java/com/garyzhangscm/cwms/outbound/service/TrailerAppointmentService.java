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
        stops.forEach(stop ->   stopService.allocateStop(stop) );
        // set the trailer appointment's status to in process
        return commonServiceRestemplateClient.changeTrailerAppointmentStatus(id, TrailerAppointmentStatus.INPROCESS);
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


    public String saveShippingTrailerAppointmentData(Long warehouseId, File file) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();
        shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 0.0);
        shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());


        List<ShippingTractorAppointmentLineCSVWrapper> shippingTractorAppointmentLineCSVWrappers =
                fileService.loadData(file, ShippingTractorAppointmentLineCSVWrapper.class);

        shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0);


        new Thread(() -> {

            int totalCount = shippingTractorAppointmentLineCSVWrappers.size();
            int index = 0;

            // before we start processing, we will sort the list by order and then order line
            // if we have a line with only order and a line with same order number but already have
            // order line, we will make sure we process the line without order line first, which
            // means we will process every line in the order and then we will error out with the
            // line that have order line number
            Collections.sort(shippingTractorAppointmentLineCSVWrappers, (line1, line2) -> {
                if (line1.getOrder().equalsIgnoreCase(line2.getOrder())) {
                    if (Strings.isBlank(line1.getLine())) {
                        return -1;
                    }
                    else if (Strings.isBlank(line2.getLine())) {
                        return 1;
                    }
                    else {
                        return line1.getLine().compareTo(line2.getLine());
                    }
                }
                else {
                    return line1.getOrder().compareTo(line2.getOrder());
                }
            });
            // a map to save the shipment we create in this transaction.
            // we will try to group all order lines from the same order into same shipment
            // key: order number
            // value: new shipment create in this transaction
            Map<String, Shipment> newShipmentMap = new HashMap<>();

            // a set to save the record that doesn't have order line.
            // we will keep track of the record so that if we meet a line with
            // same order but has order line information, we will error as we should
            // already process the line when we process at the order level
            Set<String> wholeOrderRecord =new HashSet<>();



            for (ShippingTractorAppointmentLineCSVWrapper shippingTractorAppointmentLineCSVWrapper
                    : shippingTractorAppointmentLineCSVWrappers) {
                shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));

                logger.debug("start to process line \n{}", shippingTractorAppointmentLineCSVWrapper);
                try {
                    if (Strings.isBlank(shippingTractorAppointmentLineCSVWrapper.getLine())) {
                        wholeOrderRecord.add(shippingTractorAppointmentLineCSVWrapper.getOrder());
                    }
                    else if (wholeOrderRecord.contains(shippingTractorAppointmentLineCSVWrapper.getOrder())) {
                        throw OrderOperationException.raiseException("can't process the order " +
                                shippingTractorAppointmentLineCSVWrapper.getOrder() + ", line " +
                                shippingTractorAppointmentLineCSVWrapper.getLine() + " as we already " +
                                " processed the order at a previous line that without order line information");
                    }

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));
                    TrailerAppointment trailerAppointment = commonServiceRestemplateClient.getTrailerAppointmentByNumber(
                            warehouseId, shippingTractorAppointmentLineCSVWrapper.getNumber());

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.25));
                    if (Objects.isNull(trailerAppointment)) {
                        logger.debug("Trailer appointment {} is not created yet, let's create the order on the fly ",
                                shippingTractorAppointmentLineCSVWrapper.getNumber());
                        trailerAppointment = commonServiceRestemplateClient.addTrailerAppointment(
                                warehouseId, shippingTractorAppointmentLineCSVWrapper.getTrailer(),
                                shippingTractorAppointmentLineCSVWrapper.getNumber(),
                                shippingTractorAppointmentLineCSVWrapper.getDescription(),
                                TrailerAppointmentType.SHIPPING
                        );
                    }

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.5));

                    // see if we already created a new shipment for the order. If so, we will pass into the command
                    // so all the lines from the same order will be group into same shipment
                    Shipment newlyCreatedShipment = newShipmentMap.get(
                            shippingTractorAppointmentLineCSVWrapper.getOrder()
                    );

                    if (Strings.isBlank(shippingTractorAppointmentLineCSVWrapper.getLine())) {
                        // if order line is not passed in, let's group the whole order into the trailer appointment
                        // it will automatically skip the line that already in other trailer appointment

                        attachOrderToTrailerAppointment(warehouseId, trailerAppointment,
                                shippingTractorAppointmentLineCSVWrapper.getStopSequence(),
                                shippingTractorAppointmentLineCSVWrapper.getOrder());

                    }
                    else {

                        Shipment shipment = attachOrderLineToTrailerAppointment(warehouseId, trailerAppointment,
                                shippingTractorAppointmentLineCSVWrapper.getStopSequence(),
                                shippingTractorAppointmentLineCSVWrapper.getOrder(),
                                shippingTractorAppointmentLineCSVWrapper.getLine(), newlyCreatedShipment);
                        if (Objects.isNull(newlyCreatedShipment) && Objects.nonNull(shipment)) {
                            // before this line, we haven't create any shipment yet for this order but during
                            // processing this line, we get a new shipment, let's save it to the map so that
                            // all the following order lines from this order will be group into same shipment
                            newShipmentMap.put(shippingTractorAppointmentLineCSVWrapper.getOrder(),
                                    shipment);
                        }
                    }

                    shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));


                    List<FileUploadResult> fileUploadResults =
                            shippingTrailerAppointmentFileUploadResultMap.getOrDefault(
                                    fileUploadProgressKey, new ArrayList<>()
                            );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            shippingTractorAppointmentLineCSVWrapper.toString(),
                            "success", ""
                    ));
                    shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process trailer appointment upload file record: {}, \n error message: {}",
                            shippingTractorAppointmentLineCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = shippingTrailerAppointmentFileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            shippingTractorAppointmentLineCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    shippingTrailerAppointmentFileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }

            }

            logger.debug("All lines are processed");
            shippingTrailerAppointmentFileUploadProgressMap.put(fileUploadProgressKey, 100.0);
        }).start();

        return fileUploadProgressKey;


    }

    private Shipment attachOrderToTrailerAppointment(Long warehouseId,
                                                 TrailerAppointment trailerAppointment,
                                                 Integer stopSequence,
                                                 String orderNumber) {

        Order order = orderService.findByNumber(warehouseId, orderNumber);
        if (Objects.isNull(order)) {
            throw OrderOperationException.raiseException("can't find order by number " + orderNumber);
        }
        if (order.getStatus().equals(OrderStatus.COMPLETE)) {

            throw OrderOperationException.raiseException("order " + orderNumber + " is already completed, " +
                    "can't further process this order");
        }

        // see if we have any order lines that can be group into the trailer appointment
        List<OrderLine> orderLines = order.getOrderLines().stream()
                .filter(
                    orderLine -> {
                        List<ShipmentLine> shipmentLines = orderLine.getShipmentLines().stream()
                                .filter(shipmentLine ->
                                        !shipmentLine.getStatus().equals(ShipmentLineStatus.CANCELLED) &&
                                                !shipmentLine.getStatus().equals(ShipmentLineStatus.DISPATCHED))
                                .collect(Collectors.toList());
                        if (shipmentLines.isEmpty() && orderLine.getOpenQuantity() == 0) {
                            // if there's no shipment line for the order line yet but the open quantity
                            // is 0, skip the line
                            logger.debug("The order {}, line {} doesn't have any shipment line" +
                                    " but no open quantity as well, skip the line",
                                    orderNumber, orderLine.getNumber());
                            return false;
                        }
                        else if (shipmentLines.stream().anyMatch(
                                shipmentLine -> Objects.nonNull(shipmentLine.getShipment().getStop()))) {
                            // skip the order line as it already active shipment line and the shipment line
                            // is attached to some trailer appointment
                            logger.debug("The order {}, line {} doesn't have active shipment line" +
                                            " that assigned to other trailer appointment, skip the line",
                                    orderNumber, orderLine.getNumber());
                            return false;
                        }
                        // we either don't have any shipment line for the order line yet
                        // or even if we have, the shipment line has not build into any trailer appointment yet
                        // then the order line(along with the possible shipment line) is good for the new trailer
                        // appointment
                        return true;
                    }
                ).collect(Collectors.toList());

        if (orderLines.isEmpty()) {

            throw OrderOperationException.raiseException("order " + orderNumber + " doesn't have  " +
                    " any valid order lines for the new trailer appointment");
        }

        Shipment shipment = null;
        for (OrderLine orderLine : orderLines) {
            Shipment newlyCreatedShipment =
                    attachOrderLineToTrailerAppointment(warehouseId,
                            trailerAppointment, stopSequence, orderLine, shipment);
            if (Objects.isNull(shipment) && Objects.nonNull(newlyCreatedShipment)) {
                // ok, we haven't created any shipment yet but this round, we created
                // a new shipment, let's record it
                // since we only need one shipment per order, we will replace the
                // shipment only when it is not setup yet
                shipment = newlyCreatedShipment;
            }
        }
        return shipment;

    }

    private Shipment attachOrderLineToTrailerAppointment(Long warehouseId,
                                                     TrailerAppointment trailerAppointment,
                                                         Integer stopSequence,
                                                     String orderNumber,
                                                     String orderLineNumber,
                                                         Shipment newlyCreatedShipment) {

        // make sure the line is not build into an active shipment line yet

        OrderLine orderLine = orderLineService.findByNumber(warehouseId,
                orderNumber, orderLineNumber);

        if (orderLine.getOrder().getStatus().equals(OrderStatus.COMPLETE)) {

            throw OrderOperationException.raiseException("order " + orderNumber + " is already completed, " +
                    "can't further process this order");
        }
        return attachOrderLineToTrailerAppointment(warehouseId,
                trailerAppointment, stopSequence,
                orderLine, newlyCreatedShipment);

    }

    /**
     * Attach order line to the trailer appointment
     * @param warehouseId
     * @param trailerAppointment
     * @param stopSequence
     * @param orderLine
     * @param newlyCreatedShipment optional. Passed in if we created a shipment for other lines from the same order in this transaction
     */
    private Shipment attachOrderLineToTrailerAppointment(Long warehouseId,
                                                         TrailerAppointment trailerAppointment,
                                                         Integer stopSequence,
                                                         OrderLine orderLine,
                                                         Shipment newlyCreatedShipment) {

        Shipment newShipment = null;
        // see if the we already have active shipment line for this order line
        List<ShipmentLine> shipmentLines = orderLine.getShipmentLines().stream()
                .filter(shipmentLine ->
                        !shipmentLine.getStatus().equals(ShipmentLineStatus.CANCELLED) &&
                        !shipmentLine.getStatus().equals(ShipmentLineStatus.DISPATCHED))
                .collect(Collectors.toList());

        if (shipmentLines.size() == 0) {
            // there's no active shipment line yet, let's plan a shipment line for this order line
            // before we create new shipment line, make sure there's still open quantity
            if (orderLine.getOpenQuantity() <= 0) {
                throw OrderOperationException.raiseException("Can't attach the order line into trailer appointment "
                        + trailerAppointment.getNumber() + " as the order " +
                        orderLine.getOrder().getNumber() + ", line " +
                        orderLine.getNumber() + " has 0 open quantity");
            }
            // see if we already have existing shipment for the same order. If so,
            // we will group into the same shipment
            if (Objects.isNull(newlyCreatedShipment)) {
                // shipment is not passed, we will create a shipment for the order line
                List<Shipment> shipments = shipmentService.planShipments(
                        warehouseId, List.of(orderLine)
                );
                // we should only get one shipment as we are planning for one line
                if (shipments.size() != 1) {
                    throw OrderOperationException.raiseException("error while plan order order " +
                            orderLine.getOrder().getNumber() + ", line " +
                                    orderLine.getNumber() + ", get " + shipments.size() +
                            " shipment");
                }
                newShipment = shipments.get(0);
                attachShipmentToTrailerAppointment(newShipment, trailerAppointment, stopSequence);
                // return newly created shipment
                return newShipment;
            }
            else {
                // shipment is passed in, let's build the order line to this shipment
                // since the passed in shipment is supposed to be create in this transaction, we will
                // assume it is already assigned to the trailer appointment
                shipmentService.addOrderLine(newlyCreatedShipment, orderLine);
            }
        }
        else if (shipmentLines.stream().anyMatch(
                shipmentLine -> Objects.nonNull(shipmentLine.getShipment().getStop()))) {
            // the order line has some shipment line that already group into stop / trailer appointment,
            // let's skip this order line
            throw OrderOperationException.raiseException("the order " +
                    orderLine.getOrder().getNumber() + ", line " +
                            orderLine.getNumber() + " is already in some trailer appointment");
        }
        else {
            // if we are here, we know that we already have shipment lines for this order line but
            // none of them are in any trailer appointment, let's build the shipment into the trailer
            // appointment with the right loading sequence
            Set<Shipment> shipments = shipmentLines.stream().map(
                    shipmentLine -> shipmentLine.getShipment()
            ).collect(Collectors.toSet());
            shipments.forEach(
                    existingShipment -> attachShipmentToTrailerAppointment(existingShipment, trailerAppointment, stopSequence)
            );

            // we will return nothing as we didn't create shipment in this transaction
            return null;
        }

        return null;
    }

    private void attachShipmentToTrailerAppointment(Shipment existingShipment,
                                                    TrailerAppointment trailerAppointment,
                                                    Integer stopSequence) {
        Stop stop = stopService.createStop(existingShipment);

        stopService.assignTrailerAppointment(stop, trailerAppointment,
                stopSequence);


    }


}

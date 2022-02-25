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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.Transient;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class OrderService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private OrderActivityService orderActivityService;

    @Autowired
    private PickService pickService;
    @Autowired
    private ShortAllocationService shortAllocationService;

    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private IntegrationService integrationService;

    @Value("${fileupload.test-data.orders:orders}")
    String testDataFile;

    public Order findById(Long id, boolean loadDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order not found by id: " + id));
        if (loadDetails) {
            loadOrderAttribute(order);
        }

        // calculateStatisticQuantities(order);
        return order;
    }

    public Order findById(Long id) {
        return findById(id, true);
    }


    public List<Order> findAll(Long warehouseId,
                               String number,
                               String status,
                               LocalDateTime startCompleteTime,
                               LocalDateTime endCompleteTime,
                               LocalDate specificCompleteDate,
                               Boolean loadDetails) {

        List<Order> orders =  orderRepository.findAll(
                (Root<Order> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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


                    if (StringUtils.isNotBlank(status)) {
                        OrderStatus orderStatus = OrderStatus.valueOf(status);
                        predicates.add(criteriaBuilder.equal(root.get("status"), orderStatus));

                    }

                    if (Objects.nonNull(startCompleteTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("completeTime"), startCompleteTime));

                    }

                    if (Objects.nonNull(endCompleteTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("completeTime"), endCompleteTime));

                    }
                    if (Objects.nonNull(specificCompleteDate)) {
                        LocalDateTime dateStartTime = specificCompleteDate.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = specificCompleteDate.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("completeTime"), dateStartTime, dateEndTime));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "createdTime")
        );

        if (orders.size() > 0 && loadDetails) {
            loadOrderAttribute(orders);
        }


        // calculateStatisticQuantities(orders);

        return orders;

    }

    public List<Order> findAll(Long warehouseId, String number, String status,
                               LocalDateTime startCompleteTime, LocalDateTime endCompleteTime,
                               LocalDate specificCompleteDate) {
        return findAll(warehouseId, number, status, startCompleteTime, endCompleteTime,
                specificCompleteDate, true);
    }


    public List<Order> findWavableOrders(Long warehouseId,
                                         String orderNumber,
                                         String customerName) {
        // We will get all the wavable order lines and constuct the order structure with those order line
        // As long as there's one line in the order is wavable, we will return the order but only with
        // those wavable lines
        logger.debug("start to find order lines with order: {}, customer：{}", orderNumber, customerName);
        List<OrderLine> wavableOrderLine = orderLineService.findWavableOrderLines(warehouseId,
                orderNumber, customerName);
        logger.debug("get order lines: {}", wavableOrderLine.size());
        Map<String, Order> wavableOrderMap = new HashMap<>();

        wavableOrderLine.forEach(orderLine -> {
            Order order;
            if (wavableOrderMap.containsKey(orderLine.getOrder().getNumber())) {
                order = wavableOrderMap.get(orderLine.getOrder().getNumber());
            }
            else {
                order = orderLine.getOrder();
                // clear all lines so that we will only have the wavable lines in the order
                order.setOrderLines(new ArrayList<>());

            }

            order.addOrderLine(orderLine);
            wavableOrderMap.put(order.getNumber(), order);

        });

        wavableOrderMap.values().forEach(order -> logger.debug("will return order # {} with {} wavable lines ", order.getNumber(), order.getOrderLines().size()));

        return new ArrayList<>(wavableOrderMap.values());
    }

    public Order findByNumber(Long warehouseId, String number, boolean loadDetails) {
        Order order = orderRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (order != null && loadDetails) {
            loadOrderAttribute(order);
        }
        return order;
    }

    public Order findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }


    public void loadOrderAttribute(List<Order> orders) {
        for (Order order : orders) {
            loadOrderAttribute(order);
        }
    }

    public void loadOrderAttribute(Order order) {
        // Load the details for client and supplier informaiton
        if (order.getClientId() != null && order.getClient() == null) {
            order.setClient(commonServiceRestemplateClient.getClientById(order.getClientId()));
        }
        if (order.getBillToCustomerId() != null && order.getBillToCustomer() == null) {
            order.setBillToCustomer(commonServiceRestemplateClient.getCustomerById(order.getBillToCustomerId()));
        }
        if (order.getShipToCustomerId() != null && order.getShipToCustomer() == null) {
            order.setShipToCustomer(commonServiceRestemplateClient.getCustomerById(order.getShipToCustomerId()));
        }

        if (order.getWarehouseId() != null && order.getWarehouse() == null) {
            order.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(order.getWarehouseId()));
        }


        if (Objects.nonNull(order.getStageLocationGroupId()) &&
             Objects.isNull(order.getStageLocationGroup())) {
            order.setStageLocationGroup(
                    warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                            order.getStageLocationGroupId()));
        }

        if (Objects.nonNull(order.getStageLocationId()) &&
                Objects.isNull(order.getStageLocation())) {
            order.setStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            order.getStageLocationId()));
        }


        // Load the item and inventory status information for each lines
        order.getOrderLines().forEach(orderLine -> loadOrderLineAttribute(orderLine));

        calculateStatisticQuantities(order);

    }

    private void calculateStatisticQuantities(List<Order> orders) {
        orders.forEach(order -> calculateStatisticQuantities(order));
    }
    private void calculateStatisticQuantities(Order order) {
        order.setTotalLineCount(order.getOrderLines().size());

        order.setTotalItemCount(
                (int) order.getOrderLines().stream().map(OrderLine::getItemId).distinct().count()
        );

        Long totalExpectedQuantity = 0L;
        Long totalOpenQuantity = 0L;
        Long totalInprocessQuantity = 0L;
        Long totalPendingAllocationQuantity = 0L;
        Long totalOpenPickQuantity = 0L;
        Long totalPickedQuantity = 0L;
        Long totalShippedQuantity = 0L;

        for(OrderLine orderLine : order.getOrderLines()) {
            totalExpectedQuantity += orderLine.getExpectedQuantity();
            totalOpenQuantity += orderLine.getOpenQuantity();
            totalInprocessQuantity += orderLine.getInprocessQuantity();

            totalShippedQuantity += orderLine.getShippedQuantity();
        }

        // pending allocation quantity are those open quantity
        // from shipment line
        List<ShipmentLine> shipmentLines = shipmentLineService.findByOrder(order);
        logger.debug("We find {} shipment line for this order {}",
                shipmentLines.size(), order.getNumber());
        for(ShipmentLine shipmentLine : shipmentLines) {
            logger.debug("Add shipment line {}'s open quantity {} to the total pending allocation quantity {}",
                    shipmentLine.getNumber(), shipmentLine.getOpenQuantity(),
                    totalPendingAllocationQuantity);
            totalPendingAllocationQuantity += shipmentLine.getOpenQuantity();
        }

        // Let's see the total picked quantity
        totalPickedQuantity
                = pickService.findByOrder(order).stream()
                .mapToLong(pick -> pick.getPickedQuantity()).sum();
        totalOpenPickQuantity
                =pickService.findByOrder(order).stream()
                .mapToLong(pick -> pick.getQuantity() - pick.getPickedQuantity()).sum();



        order.setTotalExpectedQuantity(totalExpectedQuantity);
        order.setTotalOpenQuantity(totalOpenQuantity);
        order.setTotalInprocessQuantity(totalInprocessQuantity);
        order.setTotalShippedQuantity(totalShippedQuantity);
        order.setTotalPendingAllocationQuantity(totalPendingAllocationQuantity);
        order.setTotalPickedQuantity(totalPickedQuantity);
        order.setTotalOpenPickQuantity(totalOpenPickQuantity);

    }

    private void loadOrderLineAttribute(OrderLine orderLine) {
        if (orderLine.getInventoryStatusId() != null && orderLine.getInventoryStatus() == null) {
            orderLine.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(orderLine.getInventoryStatusId()));
        }
        if (orderLine.getItemId() != null && orderLine.getItem() == null) {
            orderLine.setItem(inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()));
        }
    }


    public Order save(Order order) {
        Order newOrder = orderRepository.save(order);
        loadOrderAttribute(newOrder);
        return newOrder;
    }

    public Order saveOrUpdate(Order order) {
        if (Objects.isNull(order.getId()) &&
                Objects.nonNull(findByNumber(order.getWarehouseId(),order.getNumber()))) {
            order.setId(findByNumber(order.getWarehouseId(),order.getNumber()).getId());
        }
        return save(order);
    }


    public void delete(Order order) {
        orderRepository.delete(order);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    public void delete(String orderIds) {
        if (!orderIds.isEmpty()) {
            long[] orderIdArray = Arrays.asList(orderIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : orderIdArray) {
                delete(id);
            }
        }
    }


    public List<OrderCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("number").
                addColumn("shipToCustomer").
                addColumn("billToCustomer").
                addColumn("shipToContactorFirstname").
                addColumn("shipToContactorLastname").
                addColumn("shipToAddressCountry").
                addColumn("shipToAddressState").
                addColumn("shipToAddressCounty").
                addColumn("shipToAddressCity").
                addColumn("shipToAddressDistrict").
                addColumn("shipToAddressLine1").
                addColumn("shipToAddressLine2").
                addColumn("shipToAddressPostcode").
                addColumn("billToContactorFirstname").
                addColumn("billToContactorLastname").
                addColumn("billToAddressCountry").
                addColumn("billToAddressState").
                addColumn("billToAddressCounty").
                addColumn("billToAddressCity").
                addColumn("billToAddressDistrict").
                addColumn("billToAddressLine1").
                addColumn("billToAddressLine2").
                addColumn("billToAddressPostcode").
                addColumn("client").
                build().withHeader();

        return fileService.loadData(inputStream, schema, OrderCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<OrderCSVWrapper> orderCSVWrappers = loadData(inputStream);
            orderCSVWrappers.stream().forEach(orderCSVWrapper -> saveOrUpdate(convertFromWrapper(orderCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Order convertFromWrapper(OrderCSVWrapper orderCSVWrapper) {

        Order order = new Order();
        order.setNumber(orderCSVWrapper.getNumber());


        Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                            orderCSVWrapper.getCompany(),
                            orderCSVWrapper.getWarehouse());
        order.setWarehouseId(warehouse.getId());


        // if we specify the ship to customer, we load information with the customer
        if (!StringUtils.isBlank(orderCSVWrapper.getShipToCustomer())) {
            Customer shipToCustomer = commonServiceRestemplateClient.getCustomerByName(
                    warehouse.getId(), orderCSVWrapper.getShipToCustomer());

            order.setShipToCustomer(shipToCustomer);
            order.setShipToCustomerId(shipToCustomer.getId());

            order.setShipToContactorFirstname(shipToCustomer.getContactorFirstname());
            order.setShipToContactorLastname(shipToCustomer.getContactorLastname());
            order.setShipToAddressCountry(shipToCustomer.getAddressCountry());
            order.setShipToAddressState(shipToCustomer.getAddressState());
            order.setShipToAddressCounty(shipToCustomer.getAddressCounty());
            order.setShipToAddressCity(shipToCustomer.getAddressCity());
            order.setShipToAddressDistrict(shipToCustomer.getAddressDistrict());
            order.setShipToAddressLine1(shipToCustomer.getAddressLine1());
            order.setShipToAddressLine2(shipToCustomer.getAddressLine2());
            order.setShipToAddressPostcode(shipToCustomer.getAddressPostcode());
        } else {
            order.setShipToContactorFirstname(orderCSVWrapper.getShipToContactorFirstname());
            order.setShipToContactorLastname(orderCSVWrapper.getShipToContactorLastname());
            order.setShipToAddressCountry(orderCSVWrapper.getShipToAddressCountry());
            order.setShipToAddressState(orderCSVWrapper.getShipToAddressState());
            order.setShipToAddressCounty(orderCSVWrapper.getShipToAddressCounty());
            order.setShipToAddressCity(orderCSVWrapper.getShipToAddressCity());
            order.setShipToAddressDistrict(orderCSVWrapper.getShipToAddressDistrict());
            order.setShipToAddressLine1(orderCSVWrapper.getShipToAddressLine1());
            order.setShipToAddressLine2(orderCSVWrapper.getShipToAddressLine2());
            order.setShipToAddressPostcode(orderCSVWrapper.getShipToAddressPostcode());
        }

        if (!StringUtils.isBlank(orderCSVWrapper.getBillToCustomer())) {
            Customer billToCustomer = commonServiceRestemplateClient.getCustomerByName(warehouse.getId(),
                    orderCSVWrapper.getBillToCustomer());

            order.setBillToCustomer(billToCustomer);
            order.setBillToCustomerId(billToCustomer.getId());

            order.setBillToContactorFirstname(billToCustomer.getContactorFirstname());
            order.setBillToContactorLastname(billToCustomer.getContactorLastname());
            order.setBillToAddressCountry(billToCustomer.getAddressCountry());
            order.setBillToAddressState(billToCustomer.getAddressState());
            order.setBillToAddressCounty(billToCustomer.getAddressCounty());
            order.setBillToAddressCity(billToCustomer.getAddressCity());
            order.setBillToAddressDistrict(billToCustomer.getAddressDistrict());
            order.setBillToAddressLine1(billToCustomer.getAddressLine1());
            order.setBillToAddressLine2(billToCustomer.getAddressLine2());
            order.setBillToAddressPostcode(billToCustomer.getAddressPostcode());
        } else {
            order.setBillToContactorFirstname(orderCSVWrapper.getBillToContactorFirstname());
            order.setBillToContactorLastname(orderCSVWrapper.getBillToContactorLastname());
            order.setBillToAddressCountry(orderCSVWrapper.getBillToAddressCountry());
            order.setBillToAddressState(orderCSVWrapper.getBillToAddressState());
            order.setBillToAddressCounty(orderCSVWrapper.getBillToAddressCounty());
            order.setBillToAddressCity(orderCSVWrapper.getBillToAddressCity());
            order.setBillToAddressDistrict(orderCSVWrapper.getBillToAddressDistrict());
            order.setBillToAddressLine1(orderCSVWrapper.getBillToAddressLine1());
            order.setBillToAddressLine2(orderCSVWrapper.getBillToAddressLine2());
            order.setBillToAddressPostcode(orderCSVWrapper.getBillToAddressPostcode());
        }


        if (!StringUtils.isBlank(orderCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(), orderCSVWrapper.getClient());
            order.setClientId(client.getId());
            order.setClient(client);
        }

        return order;
    }

    public String getNextOrderLineNumber(Long id) {
        Order order = findById(id);
        if (order == null) {
            return "";
        } else if (order.getOrderLines().isEmpty()) {
            return "0";
        } else {
            // Suppose the line number is all numeric
            int max = 0;
            for (OrderLine orderLine : order.getOrderLines()) {
                try {
                    if (Integer.parseInt(orderLine.getNumber()) > max) {
                        max = Integer.parseInt(orderLine.getNumber());
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return String.valueOf(max + 1);
        }
    }

    @Transactional
    public Order allocate(Long orderId) {
        Order order = findById(orderId);
        logger.debug(">>>    Start to allocate order  {}  <<<",
                order.getNumber());

        // When we directly allocate the order, we will
        // 1. create a fake wave / shipment for the order
        // 2. allocate the shipment
        shipmentService.planShipments(order.getWarehouseId(), order.getOrderLines());

        // ok, if we are here, we may ends up with multiple shipments
        // with lines for the order,
        // let's allocate all of the shipment lines
        List<AllocationResult> allocationResults
                    = shipmentLineService.findByOrderNumber(order.getWarehouseId(), order.getNumber())
                        .stream()
                         .filter(shipmentLine -> shipmentLine.getOpenQuantity() >0)
                        .map(shipmentLine -> shipmentLineService.allocateShipmentLine(shipmentLine))
                        .collect(Collectors.toList());

        // logger.debug("After allocation, we get the following result: \n {}", allocationResults);

        // return findById(orderId);
        return order;

    }
    @Transactional
    public Order stage(Long orderId, boolean ignoreUnfinishedPicks) {
        Order order = findById(orderId);
        validateOrderForOutboundProcessing(order, OrderActivityType.SHIPMENT_STAGE);

        // Find any shipment that is ready for stage and stage the shipment
        logger.debug("Start to stage order: {}", order.getNumber());

        List<Shipment> shipments = shipmentService.findByOrder(order, false);
        logger.debug(">> find {} shipments for this order", shipments.size());

        // We will only stage the 'in process' shipment
        shipments.stream()
                .filter(shipment -> shipment.getStatus() == ShipmentStatus.INPROCESS)
                .forEach(shipment -> shipmentService.stage(shipment, ignoreUnfinishedPicks));


        return findById(orderId);

    }
    @Transactional
    public Order load(Long orderId, boolean ignoreUnfinishedPicks) {
        // Let's stage all the possible shipment first
        Order order = findById(orderId);
        validateOrderForOutboundProcessing(order, OrderActivityType.SHIPMENT_LOADING);

        logger.debug("Start to load order: {}", order.getNumber());

        // Find any shipments that are ready for load and load the shipments
        List<Shipment> shipments = shipmentService.findByOrder(order, false);
        logger.debug(">> find {} shipments for this order", shipments.size());

        // Let's get all the staged shipment and load those shipment onto trailer
        if (shipments.size() > 0 &&
                shipments.stream()
                     .filter(shipment -> shipment.getStatus() == ShipmentStatus.STAGED)
                     .count() == 0) {
            logger.debug(">> no shipment is staged, let's try to stage the shipment");
            // there's no staged shipment yet, let's
            // see if we can stage one in process shipment
            stage(orderId, ignoreUnfinishedPicks);

            // try to re-load the staged shipment
            shipments = shipmentService.findByOrder(order, false);
        }
        shipments.stream()
                .filter(shipment -> shipment.getStatus() == ShipmentStatus.STAGED).forEach(shipment -> {
                        logger.debug(">> Start to load shipment {}", shipment.getNumber());
                        shipmentService.loadShipment(shipment);
                        logger.debug(">> Shipment {} loaded", shipment.getNumber());
                });


        return findById(orderId);

    }
    @Transactional
    public Order dispatch(Long orderId, boolean ignoreUnfinishedPicks) {
        // Let's stage all the possible shipment first
        Order order = findById(orderId);
        validateOrderForOutboundProcessing(order, OrderActivityType.SHIPMENT_DISPATCH);
        logger.debug("Start to dispatch order: {}", order.getNumber());


        // Find any shipments that are ready for load and load the shipments
        List<Shipment> shipments = shipmentService.findByOrder(order, false);
        logger.debug(">> find {} shipments for this order", shipments.size());

        // Let's get all the staged shipment and load those shipment onto trailer
        if (shipments.size() > 0 &&
                shipments.stream()
                .filter(shipment -> shipment.getStatus() == ShipmentStatus.LOADED).count() == 0) {
            // there's no loaded shipment yet, let's
            // see if we can stage one in process shipment
            logger.debug(">> no shipment is loaded, let's try to load the shipment");
            load(orderId, ignoreUnfinishedPicks);

            // try to re-load the loaded shipment
            shipments = shipmentService.findByOrder(order, false);
        }
        // Get all the shipment that is in loaded status and will
        // only dispatch the trailer
        // 1. the shipment is in loaded status
        // 2. There's only one shipment in this trailer

        shipments.stream()
                .filter(shipment -> shipment.getStatus() == ShipmentStatus.LOADED)
                .forEach(shipment -> {
                    try {
                        logger.debug(">> Start to complete shipment {}", shipment.getNumber());
                        shipmentService.autoCompleteShipment(shipment);
                        logger.debug(">> Shipment {} completed!", shipment.getNumber());
                    } catch (IOException e) {
                        throw  OrderOperationException.raiseException("Can't complete the shipment. Error message: " + e.getMessage());
                    }
                });
        return findById(orderId);
    }

    // check if we can make outbound processing(stage / loading / shipping)
    // at order level. Normally those outbound processing activities should
    // be carried out at shipment level. For simple seek, we will allow
    // those activities on order level when there's only one active shipment
    // existing on the order
    private void validateOrderForOutboundProcessing(Order order, OrderActivityType orderActivityType) {
        List<Shipment> shipments = shipmentService.findByOrder(order, false);
        logger.debug("Get {} shipments by order number: {}, \n{}",
                shipments.size(), order.getNumber(), shipments);
        if (shipments.size() <= 1) {
            return ;
        }

        // There're more than 1 shipment exists for this order, let's make sure
        // there's at maximum only one shipment is active.
        // In case all the shipment are either cancelled, or dispatched,
        // we will still return true and let the caller decides what to do
        // with the order / shipment
        long activeShipmentsCount = shipments.stream()
                .filter(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED &&
                        shipment.getStatus() != ShipmentStatus.DISPATCHED)
                .count();
        if (activeShipmentsCount > 1) {
            throw  OrderOperationException.raiseException(
                    "There's multiple in process shipment for this order, please process each shipment individually");
        }
        else if (activeShipmentsCount == 1 &&
                (orderActivityType.equals(OrderActivityType.SHIPMENT_LOADING) ||
                        orderActivityType.equals(OrderActivityType.SHIPMENT_DISPATCH))){

            // when we have one active shipment for the order and we want to
            // load or dispatch the shipment, we will need to make sure either
            // 1. there's no trailer yet, so that when we do loading or dispatch, we will
            //    create fake stop and trailer
            // 2. there's at maximum one trailer for the order
            Shipment activeShipment = shipments.stream()
                    .filter(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED &&
                            shipment.getStatus() != ShipmentStatus.DISPATCHED).findFirst().orElse(null);

            if (activeShipment != null &&
                  activeShipment.getStop() != null) {
                // Let's see how many shipments this stop have
                int shipmentInTheSameStop = shipmentService.findByStop(order.getWarehouseId(), activeShipment.getStop().getId()).size();
                if (shipmentInTheSameStop > 1) {
                    throw  OrderOperationException.raiseException(
                            "There's multiple shipments in the same stop, please process each shipment individually");

                }
                // let's see how many shipments in the same trailer
                /***
                if (activeShipment.getStop().getTrailer() != null) {
                    int shipmentInTheSameTrailer = shipmentService.findByTrailer(
                            order.getWarehouseId(),
                            activeShipment.getStop().getTrailer().getId()).size();
                    if (shipmentInTheSameTrailer > 1) {
                        throw  OrderOperationException.raiseException(
                                "There's multiple shipments in the same trailer, please process each shipment individually");

                    }
                }
                 **/
            }
        }

    }


    @Transactional
    public Order completeOrder(Long orderId) {
        Order order = findById(orderId);
        // Let's make sure the order is still open
        if (order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw  OrderOperationException.raiseException(
                    "Complete the order " + order.getNumber() + " as it is already completed");
        }

        // Let's complete all the shipments related to this
        // order
        order.getOrderLines()
                .stream()
                .flatMap(orderLine -> orderLine.getShipmentLines().stream())
                .map(shipmentLine -> shipmentLine.getShipment())
                .distinct()
                .forEach(shipment ->
                        shipmentService.completeShipment(shipment, order));

        order.setStatus(OrderStatus.COMPLETE);
        order.setCompleteTime(LocalDateTime.now());



        logger.debug("Start to send order confirmation after the order {} is marked as completed",
                order.getNumber());
        sendOrderConfirmationIntegration(order);


        // release the location that is reserved by order

        warehouseLayoutServiceRestemplateClient.releaseLocations(order.getWarehouseId(), order);
        order.setStageLocationId(null);
        order.setStageLocationGroupId(null);


        return saveOrUpdate(order);

    }

    private void sendOrderConfirmationIntegration(Order order) {

        integrationService.process(new OrderConfirmation(order));
    }

    public ReportHistory generatePickReportByOrder(Long orderId, String locale)
            throws JsonProcessingException {

        return generatePickReportByOrder(findById(orderId), locale);
    }
    public ReportHistory generatePickReportByOrder(Order order, String locale)
            throws JsonProcessingException {

        Long warehouseId = order.getWarehouseId();


        Report reportData = new Report();
        setupOrderPickReportParameters(
                reportData, order
        );
        setupOrderPickReportData(
                reportData, order
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                    warehouseId, ReportType.ORDER_PICK_SHEET, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupOrderPickReportParameters(
            Report report, Order order) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("order_number", order.getNumber());

        report.addParameter("customer_name",
                order.getShipToContactorFirstname() + " " +
                        order.getShipToContactorLastname());

        Integer totalLineCount = order.getTotalLineCount();
        Integer totalItemCount = order.getTotalItemCount();
        Long totalQuantity =
                pickService.findByOrder(order).stream()
                .mapToLong(Pick::getQuantity).sum();

        report.addParameter("totalLineCount", totalLineCount);
        report.addParameter("totalItemCount", totalItemCount);
        report.addParameter("totalQuantity", totalQuantity);


        // we will assume we will only have one destination location for the
        // entire order
        List<Pick> picks = pickService.findByOrder(order);
        if (picks.size() > 0) {

            report.addParameter("destination_location", picks.get(0).getDestinationLocation().getName());

            // we will assume that each pick may use one pallet for now
            report.addParameter("total_pallet_count", picks.size());
        }
        else {
            report.addParameter("destination_location", "");

            // we will assume that each pick may use one pallet for now
            report.addParameter("total_pallet_count", 0);

        }
    }

    private void setupOrderPickReportData(Report report, Order order) {

        // set data to be all picks
        List<Pick> picks = pickService.findByOrder(order);
        report.setData(picks);
    }

    /**
     * @return All orders that has open pick and not assigned to anyone yet
     */
    public List<Order> getOrdersWithOpenPick(Long warehouseId) {

        // get the picks that has open quantity
        List<Pick> openPicks = pickService.getOpenPicks(warehouseId);

        logger.debug("=======  We get {} open picks  ======",
                openPicks.size());
        logger.debug(openPicks.toString());
        // get the picks that is not assigned yet
        List<Pick> unassignedOpenPick = openPicks.stream()
                .filter(pick -> Objects.isNull(pick.getWorkId()))
                .collect(Collectors.toList());

        logger.debug("=======  We get {} open unassigned picks  ======",
                unassignedOpenPick.size());
        logger.debug(unassignedOpenPick.toString());

        Set<Order> ordersWithOpenPickSet = unassignedOpenPick.stream()
                .filter(pick -> Objects.nonNull(pick.getShipmentLine()))
                .map(pick -> pick.getShipmentLine().getOrderLine().getOrder())
                .filter(order -> !order.getStatus().equals(OrderStatus.COMPLETE))
                .collect(Collectors.toSet());

        List<Order> ordersWithOpenPickList = new ArrayList<>(ordersWithOpenPickSet);



        logger.debug("=======  We get {} orders with open pick  ======",
                ordersWithOpenPickList.size());
        ordersWithOpenPickList.forEach(order -> calculateStatisticQuantities(order));
        logger.debug(ordersWithOpenPickList.toString());

        Collections.sort(ordersWithOpenPickList,
                Comparator.comparing(Order::getNumber));

        return ordersWithOpenPickList;



    }

    public Order addOrders(Order order) {
        logger.debug(">> Start to add order: {}",
                order.getNumber());
        // we will need to setup the lines of the order
        // to point to the order so when we save the order
        // the lines will be saved as well
        order.getOrderLines().forEach(
                orderLine -> {
                    orderLine.setOrder(order);

                    orderLine.setOpenQuantity(orderLine.getExpectedQuantity());
                }
        );


        Order newOrder =  saveOrUpdate(order);

        orderActivityService.saveOrderActivity(
                orderActivityService.createOrderActivity(
                        newOrder.getWarehouseId(), newOrder, OrderActivityType.ORDER_CREATE
                ));

        if (Objects.nonNull(order.getStageLocationId())) {
            // the user specify a location for this order
            // let's reserve it now so it won't reserved by other
            // orders. We will always use the order number as the reserve code

            warehouseLayoutServiceRestemplateClient.reserveLocation(
                    order.getStageLocationId(),
                    order.getNumber(),
                    getTotalVolume(order),
                    getTotalQuantity(order),
                    getTotalPalletQuantity(order)
            );
        }
        return newOrder;
    }

    private Integer getTotalPalletQuantity(Order order) {

        return order.getOrderLines().stream()
                .map(orderLine -> {
                    Long itemId = orderLine.getItemId();
                    Item item = inventoryServiceRestemplateClient.getItemById(itemId);
                    if (Objects.nonNull(item)) {
                        // let's estimate the item's volume by its first item package type
                        // and its biggest UOM
                        ItemPackageType itemPackageType = item.getItemPackageTypes().get(0);
                        ItemUnitOfMeasure biggestItemUnitOfMeasure = itemPackageType.getItemUnitOfMeasures().get(0);
                        for (ItemUnitOfMeasure itemUnitOfMeasure : itemPackageType.getItemUnitOfMeasures()) {
                            if (itemUnitOfMeasure.getQuantity() > biggestItemUnitOfMeasure.getQuantity()) {
                                biggestItemUnitOfMeasure = itemUnitOfMeasure;
                            }
                        }
                        logger.debug("Start to estimate the pallet quantity of the item {} in order {}",
                                item.getName(), order.getNumber());
                        logger.debug("based on uom {}, quantity {}, line quantity {}",
                                biggestItemUnitOfMeasure.getUnitOfMeasure().getName(),
                                biggestItemUnitOfMeasure.getQuantity(),
                                orderLine.getExpectedQuantity());
                        int palletCount = (int) Math.ceil(orderLine.getExpectedQuantity() * 1.0 / biggestItemUnitOfMeasure.getQuantity());
                        logger.debug(">> pallet count is {}", palletCount);
                        return palletCount;
                    }
                    else {
                        return 0;
                    }
                }).mapToInt(Integer::intValue).sum();
    }

    private Double getTotalVolume(Order order) {

        return order.getOrderLines().stream()
                .map(orderLine -> {
                    Long itemId = orderLine.getItemId();
                    Item item = inventoryServiceRestemplateClient.getItemById(itemId);
                    if (Objects.nonNull(item)) {
                        // let's estimate the item's volume by its first item package type
                        // and its smallest UOM
                        ItemPackageType itemPackageType = item.getItemPackageTypes().get(0);
                        ItemUnitOfMeasure itemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasures();
                        logger.debug("Start to estimate the volume of the item {} in order {}",
                                item.getName(), order.getNumber());
                        logger.debug("based on uom {}, quantity {}, size: {} x {} x {}, line quantity {}",
                                itemUnitOfMeasure.getUnitOfMeasure().getName(),
                                itemUnitOfMeasure.getQuantity(),
                                itemUnitOfMeasure.getLength(),
                                itemUnitOfMeasure.getWidth(),
                                itemUnitOfMeasure.getHeight(),
                                orderLine.getExpectedQuantity());
                        double volume = (orderLine.getExpectedQuantity() *
                                            itemUnitOfMeasure.getLength()  *
                                            itemUnitOfMeasure.getWidth() *
                                            itemUnitOfMeasure.getHeight()) / itemUnitOfMeasure.getQuantity();
                        logger.debug(">> volume is {}", volume);
                        return volume;
                    }
                    else {
                        return 0.0d;
                    }
                }).mapToDouble(Double::doubleValue).sum();
    }

    private Long getTotalQuantity(Order order) {
        return order.getOrderLines().stream().map(orderLine -> orderLine.getExpectedQuantity())
                .mapToLong(Long::longValue).sum();
    }

    public Order changeAssignedStageLocations(Long id, Long locationGroupId, Long locationId) {
        return changeAssignedStageLocations(findById(id), locationGroupId, locationId);
    }
    /**
     * Change the assigned staging location
     * @param locationGroupId
     * @param locationId
     * @return
     */
    @Transactional
    public Order changeAssignedStageLocations(Order order, Long locationGroupId, Long locationId) {
        logger.debug("changeAssignedStageLocations: order: {}, original assignment {} / {}, new assignment {} / {}",
                order.getNumber(),
                order.getStageLocationGroupId(), order.getStageLocationId(),
                locationGroupId, locationId);

        // 1. If we don't assign the location group id and the location , then both of
        //    then will be assigned when we generate the pick
        // 2. if we only assign the location group id, then the location will be assigned
        //    when we create the pick work
        // 3. if we assign both the location group id and the location, then we will need to
        //    reserve it now
        if (Objects.isNull(locationId)) {
            logger.debug("changeAssignedStageLocations: The user is trying to deassign the current stage location with a new empty staging location");
            // OK location is reset to null
            // if we already have a location assigned to the order, then let's unassign it first
            if (Objects.nonNull(order.getStageLocationId())) {
                // we already assigned a location to this order and the new
                // location is either null or a different location. In either way
                // we will have to unreserve  the location from the order
                // by default, we will
                logger.debug("changeAssignedStageLocations: will need to deassign original assignment: id {}",
                        order.getStageLocationId());
                warehouseLayoutServiceRestemplateClient.unreserveLocation(
                        order.getWarehouseId(), order.getStageLocationId());
            }

            logger.debug("changeAssignedStageLocations: reset to assignment: id {} / {}",
                    order.getStageLocationGroupId(), order.getStageLocationId());
            // assign the new location group
            order.setStageLocationId(locationId);
            order.setStageLocationGroupId(locationGroupId);

            return saveOrUpdate(order);
        }
        // the user pass in the location, we will only continue
        // if the new location is different from the original location
        else if (!Objects.equals(locationId, order.getStageLocationId())) {

            logger.debug("changeAssignedStageLocations: The user is trying to deassign the current stage location and assign to a new location {}",
                    locationId);
            // OK location is reset to some other location
            // then let's unassign the original location first
            if (Objects.nonNull(order.getStageLocationId())) {

                logger.debug("changeAssignedStageLocations: will need to deassign original assignment: id {}",
                        order.getStageLocationId());
                warehouseLayoutServiceRestemplateClient.unreserveLocation(
                        order.getWarehouseId(), order.getStageLocationId());
            }


            logger.debug("changeAssignedStageLocations: will need to reserve new location: id {}",
                    locationId);
            // assign the new location to this order
            warehouseLayoutServiceRestemplateClient.reserveLocation(
                    locationId,
                    order.getNumber(),
                    getTotalVolume(order),
                    getTotalQuantity(order),
                    getTotalPalletQuantity(order)
            );

            logger.debug("changeAssignedStageLocations: reset to assignment: id {} / {}",
                    order.getStageLocationGroupId(), order.getStageLocationId());
            // assign the new location group
            order.setStageLocationId(locationId);
            order.setStageLocationGroupId(locationGroupId);

            return saveOrUpdate(order);
        }
        else {
            // if we are here, it probably means the user pass in a location id
            // and it is the same as the one that is already assigned
            // let's do nothing and return the order
            return order;
        }
    }


    public ReportHistory generatePackingListByOrder(Long orderId, String locale)
            throws JsonProcessingException {

        return generatePackingListByOrder(findById(orderId), locale);
    }
    public ReportHistory generatePackingListByOrder(Order order, String locale)
            throws JsonProcessingException {

        Long warehouseId = order.getWarehouseId();


        Report reportData = new Report();
        setupOrderPackingListParameters(
                reportData, order
        );
        setupOrderPackingListData(
                reportData, order
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        //logger.debug("####   Report   Data  ######");
        //logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PACKING_SLIP, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupOrderPackingListParameters(
            Report report, Order order) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("ship_date", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
        ));

        report.addParameter("order_number",
                order.getNumber());

        // get the warehouse address as the ship from address
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                order.getWarehouseId()
        );
        if (Objects.nonNull(warehouse)) {
            report.addParameter("ship_from_address_line1",
                    warehouse.getAddressLine1());
            report.addParameter("ship_from_address_city_state_zipcode",
                    warehouse.getAddressCity() + ", " +
                    warehouse.getAddressState() + " " +
                    warehouse.getAddressPostcode());
        }
        else {
            report.addParameter("ship_from_address_line1",
                    "N/A");
            report.addParameter("ship_from_address_city_state_zipcode",
                    "N/A");
        }

        // get the ship to address
        report.addParameter("ship_to_address_line1",
                order.getShipToAddressLine1());
        report.addParameter("ship_to_address_city_state_zipcode",
                order.getShipToAddressCity() + ", " +
                        order.getShipToAddressState() + " " +
                        order.getShipToAddressPostcode());



    }

    private void setupOrderPackingListData(Report report, Order order) {

        long totalShippedQuantity = 0l;
        int totalPalletCount = 0;

        // set data to be all picks
        List<PackingSlipData> packingSlipDataList = new ArrayList<>();

        // get all the inventory that is picked but not shipped yet for the order and show the
        // inventory information in the pack slip

        List<Inventory> pickedInventories = new ArrayList<>();
        List<Pick> picks = pickService.findByOrder(order);
        if (picks.size() > 0) {
            pickedInventories
                    = inventoryServiceRestemplateClient.getPickedInventory(
                        order.getWarehouseId(), pickService.findByOrder(order),
                    true
                    );
        }

        // key: item name
        // value: quantity
        Map<String, Long> itemQuantityMap = new HashMap<>();

        // key: item name
        // value: lpn count
        Map<String, Set<String>> itemLpnMap = new HashMap<>();

        // key: item name
        // value: item description
        Map<String, String> itemDescriptionMap = new HashMap<>();


        for (Inventory pickedInventory : pickedInventories) {
            String itemName = pickedInventory.getItem().getName();

            // total quantity per item
            Long accumulatedQuantity = itemQuantityMap.getOrDefault(
                    itemName, 0l
            ) + pickedInventory.getQuantity();
            itemQuantityMap.put(itemName, accumulatedQuantity);

            // total LPN count per item
            Set<String> lpnSet = itemLpnMap.getOrDefault(
                    itemName, new HashSet<>()
            );
            lpnSet.add(pickedInventory.getLpn());
            itemLpnMap.put(itemName, lpnSet);

            // item name and description
            itemDescriptionMap.putIfAbsent(
                    itemName, pickedInventory.getItem().getDescription()
            );
        }
        for (Map.Entry<String, String> entry : itemDescriptionMap.entrySet()) {
            String itemName = entry.getKey();
            String itemDescription = entry.getValue();
            Long quantity = itemQuantityMap.getOrDefault(itemName, 0l);
            Integer lpnCount = itemLpnMap.getOrDefault(itemName, new HashSet<>()).size();

            packingSlipDataList.add(new PackingSlipData(itemName,
                    itemDescription, quantity, lpnCount
            ));
            totalPalletCount += lpnCount;
            totalShippedQuantity += quantity;
        }

        report.setData(packingSlipDataList);

        report.addParameter("total_shipped_quantity",
                totalShippedQuantity);
        report.addParameter("total_pallet_count",
                totalPalletCount);

    }




    public ReportHistory generateBillOfLadingByOrder(Long orderId, String locale)
            throws JsonProcessingException {

        return generateBillOfLadingByOrder(findById(orderId), locale);
    }
    public ReportHistory generateBillOfLadingByOrder(Order order, String locale)
            throws JsonProcessingException {

        Long warehouseId = order.getWarehouseId();


        Report reportData = new Report();
        setupOrderBillOfLadingParameters(
                reportData, order
        );
        setupOrderBillOfLadingData(
                reportData, order
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.BILL_OF_LADING, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupOrderBillOfLadingParameters(
            Report report, Order order) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("ship_date", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        ));

        report.addParameter("order_number",
                order.getNumber());

        // get the warehouse address as the ship from address
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                order.getWarehouseId()
        );
        if (Objects.nonNull(warehouse)) {
            report.addParameter("ship_from_name",
                    warehouse.getName());
            report.addParameter("ship_from_address_line1",
                    warehouse.getAddressLine1());
            report.addParameter("ship_from_address_city_state_zipcode",
                    warehouse.getAddressCity() + ", " +
                            warehouse.getAddressState() + " " +
                            warehouse.getAddressPostcode());
        }
        else {
            report.addParameter("ship_from_name",
                    "N/A");
            report.addParameter("ship_from_address_line1",
                    "N/A");
            report.addParameter("ship_from_address_city_state_zipcode",
                    "N/A");
        }

        // if we can get the customer's name, then display it
        // otherwise, get the first name and last name from
        // the ship to address
        if (Objects.nonNull(order.getShipToCustomer())) {

            report.addParameter("ship_to_name",
                    order.getShipToCustomer().getName());
        }
        else if (Objects.nonNull(order.getShipToCustomerId())) {
            Customer customer = commonServiceRestemplateClient.getCustomerById(
                    order.getShipToCustomerId()
            );
            if (Objects.nonNull(customer)) {
                report.addParameter("ship_to_name",
                        order.getShipToCustomer().getName());
            }
            else {
                report.addParameter("ship_to_name", "");
            }
        }
        else {

            report.addParameter("ship_to_name",
                    order.getShipToContactorFirstname() + " " +
                              order.getShipToContactorLastname());
        }

        // get the ship to address
        report.addParameter("ship_to_address_line1",
                order.getShipToAddressLine1());
        report.addParameter("ship_to_address_city_state_zipcode",
                order.getShipToAddressCity() + ", " +
                        order.getShipToAddressState() + " " +
                        order.getShipToAddressPostcode());



    }

    private void setupOrderBillOfLadingData(Report report, Order order) {

        long totalShippedQuantity = 0l;
        int totalPalletCount = 0;

        // set data to be all picks
        List<BillOfLadingData> billOfLadingDataList = new ArrayList<>();

        // get all the inventory that is picked but not shipped yet for the order and show the
        // inventory information in the pack slip

        List<Inventory> pickedInventories = new ArrayList<>();
        List<Pick> picks = pickService.findByOrder(order);
        if (picks.size() > 0) {
            pickedInventories
                    = inventoryServiceRestemplateClient.getPickedInventory(
                    order.getWarehouseId(), pickService.findByOrder(order),
                    true
            );
        }

        // key: item name
        // value: quantity
        Map<String, Long> itemQuantityMap = new HashMap<>();

        // key: item name
        // value: lpn count
        Map<String, Set<String>> itemLpnMap = new HashMap<>();

        // key: item name
        // value: item description
        Map<String, String> itemDescriptionMap = new HashMap<>();

        // key: item name
        // value: stock uom's name
        Map<String, String> itemStockUOMMap = new HashMap<>();

        // key: item name
        // value: item family name
        Map<String, String> itemFamilyNameMap = new HashMap<>();


        for (Inventory pickedInventory : pickedInventories) {
            String itemName = pickedInventory.getItem().getName();
            logger.debug("item name: {}", pickedInventory.getItem().getName());
            String stockUOMName = "N/A";
            if(Objects.nonNull(pickedInventory.getItem().getDefaultItemPackageType()) &&
                   Objects.nonNull(pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures()) &&
                   Objects.nonNull(pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures().getUnitOfMeasure())) {
                logger.debug("item default package type: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getName());
                logger.debug("item default package type's item uom: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures().getId());
                logger.debug("item default package type's item stock uom: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures().getUnitOfMeasure().getName());

                stockUOMName = pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures().getUnitOfMeasure().getName();
            }

            String itemFamilyName = "N/A";
            if (Objects.nonNull(pickedInventory.getItem().getItemFamily())) {
                itemFamilyName =
                        pickedInventory.getItem().getItemFamily().getName();
            }

            // total quantity per item
            Long accumulatedQuantity = itemQuantityMap.getOrDefault(
                    itemName, 0l
            ) + pickedInventory.getQuantity();
            itemQuantityMap.put(itemName, accumulatedQuantity);

            // total LPN count per item
            Set<String> lpnSet = itemLpnMap.getOrDefault(
                    itemName, new HashSet<>()
            );
            lpnSet.add(pickedInventory.getLpn());
            itemLpnMap.put(itemName, lpnSet);

            // item name and description
            itemStockUOMMap.putIfAbsent(
                    itemName, stockUOMName
            );


            itemFamilyNameMap.putIfAbsent(
                    itemName, itemFamilyName
            );

            itemDescriptionMap.putIfAbsent(
                    itemName,  pickedInventory.getItem().getDescription()
            );
        }
        for (Map.Entry<String, String> entry : itemDescriptionMap.entrySet()) {
            String itemName = entry.getKey();
            String itemDescription = entry.getValue();
            Long quantity = itemQuantityMap.getOrDefault(itemName, 0l);
            Integer lpnCount = itemLpnMap.getOrDefault(itemName, new HashSet<>()).size();
            String stockUOMName = itemStockUOMMap.getOrDefault(itemName, "N/A");
            String itemFamilyName = itemFamilyNameMap.getOrDefault(itemName, "N/A");
            String comment = String.valueOf(quantity / lpnCount) + " " + stockUOMName + "/PL";


            billOfLadingDataList.add(new BillOfLadingData(itemName,
                    itemDescription, quantity, lpnCount,stockUOMName, itemFamilyName,
                    comment
            ));

            totalPalletCount += lpnCount;
            totalShippedQuantity += quantity;
        }

        report.setData(billOfLadingDataList);

        report.addParameter("total_shipped_quantity",
                totalShippedQuantity);
        report.addParameter("total_pallet_count",
                totalPalletCount);

    }


    @Transactional
    public void removeOrder(Long id) {

        Order order = findById(id);
        // make sure we don't have any pick / short allocation
        if (pickService.findByOrder(order).size() > 0) {
            throw OrderOperationException.raiseException("Can't remove order while it has open picks");
        }
        if (shortAllocationService.findByOrder(order).size() > 0){
            throw OrderOperationException.raiseException("Can't remove order while it has open short allocations");
        }

        // if the order reserves any stage locations, release them
        changeAssignedStageLocations(order, null, null);


        // if we have shipment related to this order, remove the shipment as well
        Set<Long> shipmentIds = new HashSet<>();
        for (OrderLine orderLine : order.getOrderLines()) {
            orderLine.getShipmentLines().forEach(
                    shipmentLine -> shipmentIds.add(shipmentLine.getShipment().getId())
            );
        }
        logger.debug("We already have {} shipment assigned to this order", shipmentIds.size());
        for (Long shipmentId : shipmentIds) {
            shipmentService.removeShipment(shipmentId);
        }


        // remove the order
        delete(order);
    }

    public String validateNewOrderNumber(Long warehouseId, String orderNumber) {
        Order order =
                findByNumber(warehouseId, orderNumber, false);

        return Objects.isNull(order) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    /**
     * Re trigger order confirm integration
     * @param id
     */
    public Order retriggerOrderConfirmIntegration(Long id) {

        Order order = findById(id);
        if (!order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw  OrderOperationException.raiseException(
                    "Order is not complete yet, can't manually trigger an order confirm integration");
        }

        logger.debug("Start to send order confirmation after the order {} is marked as completed",
                order.getNumber());
        sendOrderConfirmationIntegration(order);
        return order;
    }

    public List<Order> getOpenOrdersForStop(Long warehouseId, String number) {

        if (Strings.isNotBlank(number)) {
            return orderRepository.findOpenOrdersForStopWithNumber(warehouseId, number);

        }
        else {
            return orderRepository.findOpenOrdersForStop(warehouseId);

        }
    }

    public void assignTrailerAppointment(long orderId, TrailerAppointment trailerAppointment) {
        logger.debug("Start to assign order to trailer appointment");
        Order order = findById(orderId);
        logger.debug("order: {}, trailer appointment: {}", order.getNumber(),
                trailerAppointment.getNumber());
        // if we already have the shipment created, then raise error, right now we don't
        // support to create trailer appointment from the order when there's shipment with the order
        // the user can create trailer appointment from the shipment
        if (order.getOrderLines().stream().anyMatch(
                orderLine -> Objects.nonNull(orderLine.getShipmentLines()) &&
                        !orderLine.getShipmentLines().isEmpty() &&
                        orderLine.getShipmentLines().stream().anyMatch(
                                shipmentLine -> !Objects.equals(shipmentLine.getStatus(), ShipmentLineStatus.CANCELLED)
                        )
        )) {
            // ok we found some order line that has shipment line and the shipment line is not cancelled
            throw OrderOperationException.raiseException("The order has shipment, please assign the trailer to the shipment");
        }

        // if we are here, we know we have no open shipment for the order,
        // let's create the shipment so that we can assign the stop and trailer appointment

        List<Shipment> shipments =
                shipmentService.planShipments(order.getWarehouseId(), order.getOrderLines());
        // we know for the same order, we should only get one shipment
        // we will assign the trailer appointment to this shipment
        shipments.stream().forEach(
                shipment -> shipmentService.assignTrailerAppointment(shipment.getId(), trailerAppointment)
        );
    }
}

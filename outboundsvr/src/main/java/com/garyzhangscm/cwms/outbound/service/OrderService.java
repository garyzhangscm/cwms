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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.Transient;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
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
    private PickService pickService;

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
    private FileService fileService;

    @Value("${fileupload.test-data.orders:orders}")
    String testDataFile;

    public Order findById(Long id, boolean loadDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order not found by id: " + id));
        if (loadDetails) {
            loadOrderAttribute(order);
        }
        return order;
    }

    public Order findById(Long id) {
        return findById(id, true);
    }


    public List<Order> findAll(String number, boolean loadDetails) {
        List<Order> orders;

        if (StringUtils.isBlank(number)) {
            orders = orderRepository.findAll();
        } else {
            Order order = orderRepository.findByNumber(number);
            if (order != null) {
                orders = Arrays.asList(new Order[]{order});
            } else {
                orders = new ArrayList<>();
            }
        }
        if (orders.size() > 0 && loadDetails) {
            loadOrderAttribute(orders);
        }
        return orders;
    }

    public List<Order> findAll(String number) {
        return findAll(number, true);
    }


    public List<Order> findWavableOrders(String orderNumber,
                                         String customerName) {
        // We will get all the wavable order lines and constuct the order structure with those order line
        // As long as there's one line in the order is wavable, we will return the order but only with
        // those wavable lines
        logger.debug("start to find order lines with order: {}, customer：{}", orderNumber, customerName);
        List<OrderLine> wavableOrderLine = orderLineService.findWavableOrderLines(orderNumber, customerName);
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

    public Order findByNumber(String number, boolean loadDetails) {
        Order order = orderRepository.findByNumber(number);
        if (order != null && loadDetails) {
            loadOrderAttribute(order);
        }
        return order;
    }

    public Order findByNumber(String number) {
        return findByNumber(number, true);
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

        // Load the item and inventory status information for each lines
        order.getOrderLines().forEach(orderLine -> loadOrderLineAttribute(orderLine));

        calculateStatisticQuantities(order);

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
        for(ShipmentLine shipmentLine : shipmentLines) {
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
        if (order.getId() == null && findByNumber(order.getNumber()) != null) {
            order.setId(findByNumber(order.getNumber()).getId());
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

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
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

        if (!StringUtils.isBlank(orderCSVWrapper.getWarehouse())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(orderCSVWrapper.getWarehouse());
            if (warehouse != null) {
                order.setWarehouseId(warehouse.getId());
            }
        }
        // if we specify the ship to customer, we load information with the customer
        if (!StringUtils.isBlank(orderCSVWrapper.getShipToCustomer())) {
            Customer shipToCustomer = commonServiceRestemplateClient.getCustomerByName(orderCSVWrapper.getShipToCustomer());

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
            Customer billToCustomer = commonServiceRestemplateClient.getCustomerByName(orderCSVWrapper.getBillToCustomer());

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
            Client client = commonServiceRestemplateClient.getClientByName(orderCSVWrapper.getClient());
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

        // When we directly allocate the order, we will
        // 1. create a fake wave / shipment for the order
        // 2. allocate the shipment
        shipmentService.planShipments(order.getWarehouseId(), order.getOrderLines());

        // ok, if we are here, we may ends up with multiple shipments
        // with lines for the order,
        // let's allocate all of the shipment lines
        List<AllocationResult> allocationResults
                    = shipmentLineService.findByOrderNumber(order.getWarehouseId(), order.getNumber())
                        .stream().filter(shipmentLine -> shipmentLine.getOpenQuantity() >0)
                        .map(shipmentLine -> shipmentLineService.allocateShipmentLine(shipmentLine))
                        .collect(Collectors.toList());

        logger.debug("After allocation, we get the following result: \n {}", allocationResults);
        return findById(orderId);

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
                    try {
                        logger.debug(">> Start to load shipment {}", shipment.getNumber());
                        shipmentService.loadShipment(shipment);
                        logger.debug(">> Shipment {} loaded", shipment.getNumber());
                    } catch (IOException e) {
                        throw OrderOperationException.raiseException(e.getMessage());
                    }
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
                if (activeShipment.getStop().getTrailer() != null) {
                    int shipmentInTheSameTrailer = shipmentService.findByTrailer(
                            order.getWarehouseId(),
                            activeShipment.getStop().getTrailer().getId()).size();
                    if (shipmentInTheSameTrailer > 1) {
                        throw  OrderOperationException.raiseException(
                                "There's multiple shipments in the same trailer, please process each shipment individually");

                    }
                }
            }
        }

    }


}

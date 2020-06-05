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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderLineRepository;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class OrderLineService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(OrderLineService.class);

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.order_lines:order_lines}")
    String testDataFile;

    public OrderLine findById(Long id) {
        return findById(id, true);
    }

    public OrderLine findById(Long id, boolean includeDetails) {
        OrderLine orderLine = orderLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order line not found by id: " + id));
        if (includeDetails) {
            loadOrderLineAttribute(orderLine);
        }
        return orderLine;
    }
    public List<OrderLine> findAll() {
        return findAll(true);
    }
    public List<OrderLine> findAll(boolean includeDetails) {


        List<OrderLine> orderLines = orderLineRepository.findAll();
        if (orderLines.size() > 0 && includeDetails) {
            loadOrderLineAttribute(orderLines);
        }
        return orderLines;
    }

    public List<OrderLine> findAll(Long warehouseId, Long shipmentId) {
        return findAll(warehouseId, shipmentId, true);
    }

    public List<OrderLine> findAll(Long warehouseId, Long shipmentId,
                                   boolean includeDetails) {
        logger.debug("## Will find order line with shipment ID: {} / {} ", shipmentId,  Objects.nonNull(shipmentId));

        List<OrderLine> orderLines =  orderLineRepository.findAll(
                (Root<OrderLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(shipmentId)) {

                        Join<OrderLine, ShipmentLine> joinShipmentLine = root.join("shipmentLines", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment = joinShipmentLine.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));
                        // only return the order line with shipment line that is not cancelled
                        predicates.add(criteriaBuilder.notEqual(joinShipmentLine.get("status"), ShipmentLineStatus.CANCELLED));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (orderLines.size() > 0 && includeDetails) {
            loadOrderLineAttribute(orderLines);
        }
        return orderLines;
    }

    public OrderLine findByNaturalKey(Long orderId, String number) {
        return orderLineRepository.findByNaturalKey(orderId, number);

    }


    public void loadOrderLineAttribute(List<OrderLine> orderLines) {
        for(OrderLine orderLine : orderLines) {
            loadOrderLineAttribute(orderLine);
        }

    }

    public void loadOrderLineAttribute(OrderLine orderLine) {

        // Load Item information
        if (Objects.nonNull(orderLine.getItemId()) && Objects.isNull(orderLine.getItem())) {
            orderLine.setItem(inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()));

        }
        if (Objects.nonNull(orderLine.getInventoryStatusId()) && Objects.isNull(orderLine.getInventoryStatus())) {
            orderLine.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(orderLine.getInventoryStatusId()));

        }

        if (Objects.nonNull(orderLine.getCarrierId()) && Objects.isNull(orderLine.getCarrier())) {
            orderLine.setCarrier(commonServiceRestemplateClient.getCarrierById(orderLine.getCarrierId()));
        }
        if (Objects.nonNull(orderLine.getCarrierServiceLevelId()) && Objects.isNull(orderLine.getCarrierServiceLevel())) {
            orderLine.setCarrierServiceLevel(commonServiceRestemplateClient.getCarrierServiceLevelById(
                    orderLine.getCarrierServiceLevelId()
            ));
        }
    }


    public List<OrderLine> findWavableOrderLines(String orderNumber,
                                         String customerName) {

        List<OrderLine> wavableOrderLine =  orderLineRepository.findAll(
                (Root<OrderLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    // the open quantity needs to be greater than 0 so we can plan a wave on this order line
                    predicates.add(criteriaBuilder.greaterThan(root.get("openQuantity"), 0L));

                    if (!StringUtils.isBlank(orderNumber)) {
                        Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("name"), orderNumber));

                    }
                    if (!StringUtils.isBlank(customerName)) {
                        Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);
                        Customer customer = commonServiceRestemplateClient.getCustomerByName(customerName);
                        if (customer != null) {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("shipToCustomerId"), customer.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("shipToCustomerId"), -999L));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (wavableOrderLine.size() > 0) {
            loadOrderLineAttribute(wavableOrderLine);
        }
        return wavableOrderLine;
    }

    public OrderLine save(OrderLine orderLine) {
        return orderLineRepository.save(orderLine);
    }

    public OrderLine saveOrUpdate(OrderLine orderLine) {

        if (Objects.isNull(orderLine.getId()) &&
                Objects.nonNull(findByNaturalKey(orderLine.getOrder().getId(), orderLine.getNumber()))) {
            orderLine.setId(findByNaturalKey(orderLine.getOrder().getId(), orderLine.getNumber()).getId());
        }
        return save(orderLine);
    }

    public void delete(OrderLine orderLine) {
        orderLineRepository.delete(orderLine);
    }
    public void delete(Long id) {
        orderLineRepository.deleteById(id);
    }
    public void delete(String orderLineIds) {
        if (!orderLineIds.isEmpty()) {
            long[] orderLineIdArray = Arrays.asList(orderLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : orderLineIdArray) {
                delete(id);
            }
        }
    }


    public List<OrderLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("order").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, OrderLineCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName  = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<OrderLineCSVWrapper> orderLineCSVWrappers = loadData(inputStream);
            orderLineCSVWrappers.stream().forEach(orderLineCSVWrapper -> saveOrUpdate(convertFromWrapper(orderLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private OrderLine convertFromWrapper(OrderLineCSVWrapper orderLineCSVWrapper) {

        OrderLine orderLine = new OrderLine();
        orderLine.setNumber(orderLineCSVWrapper.getNumber());
        orderLine.setExpectedQuantity(orderLineCSVWrapper.getExpectedQuantity());
        orderLine.setOpenQuantity(orderLineCSVWrapper.getExpectedQuantity());
        orderLine.setInprocessQuantity(0L);
        orderLine.setShippedQuantity(0L);

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(orderLineCSVWrapper.getWarehouse());

        orderLine.setWarehouseId(warehouse.getId());

        if (!StringUtils.isBlank(orderLineCSVWrapper.getOrder())) {
            Order order = orderService.findByNumber(warehouse.getId(), orderLineCSVWrapper.getOrder());
            orderLine.setOrder(order);
        }
        if (!StringUtils.isBlank(orderLineCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouse.getId(), orderLineCSVWrapper.getItem());
            orderLine.setItemId(item.getId());
        }
        if (!StringUtils.isBlank(orderLineCSVWrapper.getInventoryStatus())) {
            InventoryStatus inventoryStatus =
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouse.getId(), orderLineCSVWrapper.getInventoryStatus());
            orderLine.setInventoryStatusId(inventoryStatus.getId());
        }
        return orderLine;
    }

    public void markQuantityAsInProcess(OrderLine orderLine, Long inprocessQuantity) {
        Long openQuantity = orderLine.getOpenQuantity();
        if (openQuantity < inprocessQuantity) {
            logger.debug("expected inprocess quantity {} exceeds the open quantity {} ", inprocessQuantity, openQuantity);
            throw OrderOperationException.raiseException("Inprocess quantity can't exceed the open quantity");
        }
        orderLine.setOpenQuantity(openQuantity - inprocessQuantity);
        orderLine.setInprocessQuantity(orderLine.getInprocessQuantity() + inprocessQuantity);
        saveOrUpdate(orderLine);
    }

    @Transactional
    public void registerShipmentLineComplete(ShipmentLine shipmentLine, Long quantity) {
        // When we complete a shipment line, we will move the
        // quantity from order line's in process to shipped quantity
        OrderLine orderLine = shipmentLine.getOrderLine();
        orderLine.setInprocessQuantity(orderLine.getInprocessQuantity() - quantity);
        orderLine.setShippedQuantity(orderLine.getShippedQuantity() + quantity);
        saveOrUpdate(orderLine);
    }

    @Transactional
    public void shippingPackage(OrderLine orderLine, Inventory inventory) {

        orderLine.setShippedQuantity(orderLine.getShippedQuantity() + inventory.getQuantity());
        orderLine.setInprocessQuantity(orderLine.getInprocessQuantity() - inventory.getQuantity());
        saveOrUpdate(orderLine);

    }
}

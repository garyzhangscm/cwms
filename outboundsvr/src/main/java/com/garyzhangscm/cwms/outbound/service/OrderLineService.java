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
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderLineService{
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
    @Autowired
    private HualeiShippingService hualeiShippingService;

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

    public List<OrderLine> findAll(Long warehouseId, Long clientId,Long shipmentId,
                                   String orderNumber, String itemName, Long itemId,
                                   Long inventoryStatusId,
                                   ClientRestriction clientRestriction) {
        return findAll(warehouseId, clientId, shipmentId, orderNumber, itemName,
                itemId, inventoryStatusId, clientRestriction, true);
    }

    public List<OrderLine> findAll(Long warehouseId, Long clientId, Long shipmentId,
                                   String orderNumber, String itemName,Long itemId,
                                   Long inventoryStatusId,
                                   ClientRestriction clientRestriction,
                                   boolean includeDetails) {


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
                    if (Objects.nonNull(clientId)) {

                        Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("clientId"), clientId));
                    }

                    if (StringUtils.isNotBlank(orderNumber)) {

                        Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));


                    }

                    if (StringUtils.isNotBlank(itemName)) {

                        Item item = inventoryServiceRestemplateClient.getItemByName(
                                warehouseId, clientId, itemName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));


                    }
                    if (Objects.nonNull(itemId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    if (Objects.nonNull(inventoryStatusId)) {

                        predicates.add(criteriaBuilder.equal(root.get("inventoryStatusId"), inventoryStatusId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction)) {
                        return predicate;
                    }
                    else {

                        Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);
                        return
                                clientRestriction.addClientRestriction(predicate,
                                        joinOrder, criteriaBuilder);
                    }

                    // return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (orderLines.size() > 0 && includeDetails) {
            loadOrderLineAttribute(orderLines);
        }
        return orderLines;
    }

    public OrderLine findByNumber(Long warehouseId, String orderNumber, String lineNumber) {

        return orderLineRepository.findByNumber(warehouseId,
                orderNumber, lineNumber);
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

/**
    public List<OrderLine> findWavableOrderLines(Long warehouseId, String orderNumber,
                                                 Long clientId,
                                                 String customerName, Long customerId,
                                                 ZonedDateTime startCreatedTime,
                                                 ZonedDateTime endCreatedTime,
                                                 LocalDate specificCreatedDate,
                                                 ClientRestriction clientRestriction) {

        List<OrderLine> wavableOrderLine =  orderLineRepository.findAll(
                (Root<OrderLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    // the open quantity needs to be greater than 0 so we can plan a wave on this order line
                    predicates.add(criteriaBuilder.greaterThan(root.get("openQuantity"), 0L));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    Join<OrderLine, Order> joinOrder = root.join("order", JoinType.INNER);

                    // only return the order status that is not complete
                    predicates.add(criteriaBuilder.notEqual(joinOrder.get("status"),
                            OrderStatus.COMPLETE));

                    if (!StringUtils.isBlank(orderNumber)) {
                        predicates.add(criteriaBuilder.equal(joinOrder.get("name"), orderNumber));

                    }
                    if (!StringUtils.isBlank(customerName)) {
                        Customer customer = commonServiceRestemplateClient.getCustomerByName(null, warehouseId,
                                customerName);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("shipToCustomerId"), customer.getId()));

                    }
                    if (Objects.nonNull(customerId)) {
                        predicates.add(criteriaBuilder.equal(joinOrder.get("shipToCustomerId"), customerId));
                    }

                    if (Objects.nonNull(startCreatedTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                joinOrder.get("createdTime"), startCreatedTime));

                    }

                    if (Objects.nonNull(endCreatedTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                joinOrder.get("createdTime"), endCreatedTime));

                    }
                    if (Objects.nonNull(specificCreatedDate)) {
                        LocalDateTime dateStartTime = specificCreatedDate.atStartOfDay();
                        LocalDateTime dateEndTime = specificCreatedDate.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                joinOrder.get("createdTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }
                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(
                                joinOrder.get("clientId"), clientId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (wavableOrderLine.size() > 0) {
            loadOrderLineAttribute(wavableOrderLine);
        }
        return wavableOrderLine.stream().filter(
                orderLine -> orderLine.getOrder().getCategory().isWaveable()
        ).collect(Collectors.toList());
    }

 **/
    public OrderLine save(OrderLine orderLine) {
        boolean newOrderLineFlag = false;
        if (Objects.isNull(orderLine.getId())) {
            newOrderLineFlag = true;
        }
        OrderLine newOrderLine = orderLineRepository.save(orderLine);

        // see if we will need to auto request shipping label
        logger.debug("for new order line, let's see if we will need to automatically print shipping label " +
                        " when creating the new order {}, line {}",
                newOrderLine.getNumber(), newOrderLine.getNumber());
        logger.debug("newOrderLineFlag?: {}", newOrderLineFlag);
        logger.debug("newOrderLine.getAutoRequestShippingLabel()?: {}",
                Boolean.TRUE.equals(newOrderLine.getAutoRequestShippingLabel()));
        if (newOrderLineFlag && Boolean.TRUE.equals(newOrderLine.getAutoRequestShippingLabel())) {
            logger.debug("start a new thread to request the shipping label as it may take a while");
            new Thread(() -> autoRequestShippingLabel(orderLine)).start();
        }

        return newOrderLine;
    }

    /**
     * Automatically print the shipping label
     * NOW only works for hualei
     * @param orderLine
     */
    public void autoRequestShippingLabel(OrderLine orderLine) {

        // for now we only support hualei system so we will need to make sure the order line has
        // hualei's product id setup
        if (Strings.isBlank(orderLine.getHualeiProductId())) {
            logger.debug("skip the order {} / line {} as it doesn't have the hualei product id setup ",
                    orderLine.getOrderNumber(), orderLine.getNumber());
            return;
        }
        // we will always assume there's one package per carton(item's case UOM)
        Item item = Objects.isNull(orderLine.getItem()) ?
                inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()) :
                orderLine.getItem();

        if (Objects.isNull(item)) {
            throw OrderOperationException.raiseException("can't automatically request a shipping label as we can't " +
                    "find the item with id  " + orderLine.getItemId() + " for order " +
                    orderLine.getOrderNumber() + ", line " + orderLine.getNumber());
        }
        // get the case UOM from the default item package type
        if (Objects.isNull(item.getDefaultItemPackageType())) {
            throw OrderOperationException.raiseException("can't automatically request a shipping label as we can't " +
                    " find the default item package type for item " + item.getName());
        }
        ItemUnitOfMeasure caseItemUnitOfMeasure = item.getDefaultItemPackageType().getItemUnitOfMeasures().stream().filter(
                itemUnitOfMeasure -> Boolean.TRUE.equals(itemUnitOfMeasure.getCaseFlag())
        ).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.raiseException("can't automatically request a shipping label as we can't " +
                        " find the case Unit of Measure from the default Item Pacakge Type of item " + item.getName()));

        logger.debug("start to automatically request a hualei shipping label by parameters");
        logger.debug("warehouse id: {}", orderLine.getWarehouseId());
        logger.debug("product id: {}", orderLine.getHualeiProductId());
        logger.debug("order number: {}", orderLine.getOrder().getNumber());
        logger.debug("length: {}", caseItemUnitOfMeasure.getLength());
        logger.debug("width: {}", caseItemUnitOfMeasure.getWidth());
        logger.debug("height: {}", caseItemUnitOfMeasure.getHeight());
        logger.debug("weight: {}", caseItemUnitOfMeasure.getWeight());
        logger.debug("item name: {}", item.getName());
        logger.debug("quantity: {}", caseItemUnitOfMeasure.getQuantity());
        logger.debug("item price: {}", item.getUnitCost());
        hualeiShippingService.sendHualeiShippingRequest(
                orderLine.getWarehouseId(),
                orderLine.getHualeiProductId(),
                orderLine.getOrder(),
                caseItemUnitOfMeasure.getLength(),
                caseItemUnitOfMeasure.getWidth(),
                caseItemUnitOfMeasure.getHeight(),
                caseItemUnitOfMeasure.getWeight(),
                (int) Math.ceil(orderLine.getExpectedQuantity() / caseItemUnitOfMeasure.getQuantity()),
                item.getName(),
                caseItemUnitOfMeasure.getQuantity(),
                item.getUnitCost(),
                caseItemUnitOfMeasure.getLengthUnit(),
                caseItemUnitOfMeasure.getWeightUnit(),
                orderLine.getParcelInsured(),
                Objects.isNull(orderLine.getParcelInsuredAmountPerUnit()) ? 0 :
                        orderLine.getParcelInsuredAmountPerUnit() * caseItemUnitOfMeasure.getQuantity(),
                orderLine.getParcelSignatureRequired()
        );

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

    /**
     * Load both line data and order data in one file and create the order if necessary
     * @param inputStream
     * @return
     * @throws IOException
     */
    public List<OrderLineCSVWrapper> loadDataWithOrder(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("order").
                addColumn("shipToCustomer").
                addColumn("billToCustomerSameAsShipToCustomer").
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
                addColumn("billToAddressSameAsShipToAddress").
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
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                addColumn("allocationStrategyType").
                build().withHeader();

        return fileService.loadData(inputStream, schema, OrderLineCSVWrapper.class);
    }

    public List<OrderLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("order").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                addColumn("allocationStrategyType").
                build().withHeader();

        return fileService.loadData(inputStream, schema, OrderLineCSVWrapper.class);
    }

    /**
    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<OrderLineCSVWrapper> orderLineCSVWrappers = loadData(inputStream);
            orderLineCSVWrappers.stream().forEach(orderLineCSVWrapper -> saveOrUpdate(convertFromWrapper(orderLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
     **/

    private OrderLine convertFromWrapper(Long warehouseId, Long clientId,
                                         OrderLineCSVWrapper orderLineCSVWrapper,
                                         Order order) {

        OrderLine orderLine = new OrderLine();

        if (Strings.isBlank(orderLineCSVWrapper.getLine())) {
            // if order line is not passed in, get the next number
            logger.debug("Order line number is not in the CSV file, let's get the max number from existing order lines of order {}",
                    order.getNumber());
            int maxExistingOrderLineNumber = 0;
            for (OrderLine existingOrderLine : order.getOrderLines()) {
                try {
                    int orderLineNumber = Integer.parseInt(existingOrderLine.getNumber());
                    maxExistingOrderLineNumber = Math.max(maxExistingOrderLineNumber, orderLineNumber);
                }
                catch (Exception ex) {
                    // skip the current line if the order line is not an integer
                }
            }

            orderLine.setNumber(String.valueOf(maxExistingOrderLineNumber + 1));

            logger.debug("set the current order line's number to {} after the calculation",
                    orderLine.getNumber());
        }
        else {
            orderLine.setNumber(orderLineCSVWrapper.getLine());

        }


        orderLine.setColor(orderLineCSVWrapper.getColor());
        orderLine.setProductSize(orderLineCSVWrapper.getProductSize());
        orderLine.setStyle(orderLineCSVWrapper.getStyle());

        orderLine.setInprocessQuantity(0L);
        orderLine.setShippedQuantity(0L);

        orderLine.setHualeiProductId(orderLineCSVWrapper.getHualeiProductId());
        orderLine.setAutoRequestShippingLabel(
                Strings.isBlank(orderLineCSVWrapper.getAutoRequestShippingLabel()) ?
                        false
                        :
                        orderLineCSVWrapper.getAutoRequestShippingLabel().equals("1") ||
                                orderLineCSVWrapper.getAutoRequestShippingLabel().equalsIgnoreCase("true") ||
                                orderLineCSVWrapper.getAutoRequestShippingLabel().equalsIgnoreCase("yes")

        );

        orderLine.setParcelInsured(
                Strings.isBlank(orderLineCSVWrapper.getParcelInsured()) ?
                        false
                        :
                        orderLineCSVWrapper.getParcelInsured().equals("1") ||
                                orderLineCSVWrapper.getParcelInsured().equalsIgnoreCase("true") ||
                                orderLineCSVWrapper.getParcelInsured().equalsIgnoreCase("yes")

        );

        orderLine.setParcelInsuredAmountPerUnit(orderLineCSVWrapper.getParcelInsuredAmountPerUnit());

        orderLine.setParcelSignatureRequired(
                Strings.isBlank(orderLineCSVWrapper.getParcelSignatureRequired()) ?
                        false
                        :
                        orderLineCSVWrapper.getParcelSignatureRequired().equals("1") ||
                                orderLineCSVWrapper.getParcelSignatureRequired().equalsIgnoreCase("true") ||
                                orderLineCSVWrapper.getParcelSignatureRequired().equalsIgnoreCase("yes")

        );

        orderLine.setAllocateByReceiptNumber(orderLineCSVWrapper.getAllocateByReceiptNumber());
        orderLine.setWarehouseId(warehouseId);



        if (Objects.nonNull(order)) {
            orderLine.setOrder(order);
        }
        else if (Strings.isNotBlank(orderLineCSVWrapper.getOrder())) {
            order = orderService.findByNumber(warehouseId, clientId, orderLineCSVWrapper.getOrder());
            orderLine.setOrder(order);
        }
        if (!StringUtils.isBlank(orderLineCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouseId, order.getClientId(),
                    orderLineCSVWrapper.getItem());
            if (Objects.isNull(item)) {
                throw OrderOperationException.raiseException("Can't find item with name " + orderLineCSVWrapper.getItem());
            }

            orderLine.setItemId(item.getId());

            long unitOfMeasureQuantity = 1l;
            if (Strings.isNotBlank(orderLineCSVWrapper.getUnitOfMeasure()) &&
                    Objects.nonNull(item)) {
                unitOfMeasureQuantity = item.getDefaultItemPackageType().getItemUnitOfMeasures()
                        .stream().filter(itemUnitOfMeasure ->
                                itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(
                                        orderLineCSVWrapper.getUnitOfMeasure()
                                ))
                        .map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity())
                        .findFirst().orElse(1l);
            }
            orderLine.setExpectedQuantity(orderLineCSVWrapper.getExpectedQuantity() * unitOfMeasureQuantity);
            orderLine.setOpenQuantity(orderLine.getExpectedQuantity());

        }

        InventoryStatus inventoryStatus = null;
        if (Strings.isNotBlank(orderLineCSVWrapper.getInventoryStatus())) {
            inventoryStatus =
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, orderLineCSVWrapper.getInventoryStatus());
        }
        else {
            // default to available inventory status
            inventoryStatus =
                    inventoryServiceRestemplateClient.getAvailableInventoryStatus(warehouseId);

        }
        if (Objects.nonNull(inventoryStatus)) {
            orderLine.setInventoryStatusId(inventoryStatus.getId());
        }

        logger.debug("orderLineCSVWrapper.getAllocationStrategyType(): {} / {} : {}",
                orderLineCSVWrapper.getOrder(),
                orderLineCSVWrapper.getLine(),
                orderLineCSVWrapper.getAllocationStrategyType());

        if (Strings.isNotBlank(orderLineCSVWrapper.getAllocationStrategyType())) {
            orderLine.setAllocationStrategyType(AllocationStrategyType.valueOf(
                    orderLineCSVWrapper.getAllocationStrategyType()
            ));
            logger.debug("Order line's allocation strategy type: {}",
                    orderLine.getAllocationStrategyType());
        }
        else {
            orderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);
            logger.debug("Order line's allocation strategy type default to: {}",
                    orderLine.getAllocationStrategyType());
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

    /**
     * Return the inprocess quantity back to open quantity. This is normally happens when we
     * cancel the shipment line for the order line
     * @param orderLine
     * @param reducedInprocessQuantity
     */
    public void returnInProcessQuantity(OrderLine orderLine, Long reducedInprocessQuantity) {
        orderLine.setOpenQuantity(
                Math.min(orderLine.getExpectedQuantity(), orderLine.getOpenQuantity() + reducedInprocessQuantity));
        orderLine.setInprocessQuantity(
                Math.max(0, orderLine.getInprocessQuantity() - reducedInprocessQuantity));
        saveOrUpdate(orderLine);


    }
    /**
     * Return the inprocess quantity back to open quantity. This is normally happens when we
     * cancel the shipment line for the order line
     * @param orderLine
     * @param reducedInprocessQuantity
     */
    public void registerShipmentLineCancelled(OrderLine orderLine, Long reducedInprocessQuantity) {
        returnInProcessQuantity(orderLine, reducedInprocessQuantity);
        // see if we need to set the order back to 'PENDING' if all shipment line
        // has been cancelled and there's no in process quantity

        logger.debug("after we return the in process quantity back to the order line {} / {}, " +
                "see if we can reset the order {}'s status",
                orderLine.getOrder().getNumber(), orderLine.getNumber(),
                orderLine.getOrder().getNumber());
        orderService.registerShipmentLineCancelled(orderLine.getOrder());

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


    @Transactional
    public OrderLine registerProductionPlanLine(Long orderLineId,
                                            Long productionPlanQuantity) {
        OrderLine orderLine = findById(orderLineId);
        orderLine.setProductionPlanInprocessQuantity(
                orderLine.getProductionPlanInprocessQuantity() +
                        productionPlanQuantity
        );
        return saveOrUpdate(orderLine);
    }

    @Transactional
    public OrderLine registerProductionPlanProduced(Long orderLineId,
                                            Long productionPlanProducedQuantity) {
        OrderLine orderLine = findById(orderLineId);
        // move the quantity from production plan in process quantity
        // into produced quantity
        if (orderLine.getProductionPlanInprocessQuantity() < productionPlanProducedQuantity) {
            // it is possible that we produced more than the plan
            orderLine.setProductionPlanInprocessQuantity(0L);
        }
        else {
            orderLine.setProductionPlanInprocessQuantity(
                    orderLine.getProductionPlanInprocessQuantity() - productionPlanProducedQuantity
            );
        }
        orderLine.setProductionPlanProducedQuantity(
                orderLine.getProductionPlanProducedQuantity() +
                        productionPlanProducedQuantity
        );
        return saveOrUpdate(orderLine);
    }

    public List<OrderLine> findProductionPlanCandidate( Long warehouseId,
                                                        String orderNumber,
                                                        String itemName) {
        List<OrderLine> orderLines = findAll(warehouseId, null, null, orderNumber, itemName, null, null, null);
        logger.debug("Find {} candidate by parameters {}, {}, {}",
                orderLines.size(), warehouseId, orderNumber, itemName);
        return orderLines.stream().filter(orderLine -> isProductionPlanCandidate(orderLine))
                .collect(Collectors.toList());
    }

    private boolean isProductionPlanCandidate(OrderLine orderLine) {
        logger.debug("check if order line {} / {} can be a production plan candidate: {}",
                orderLine.getOrderNumber(), orderLine.getNumber(),
                (orderLine.getOpenQuantity().equals(orderLine.getExpectedQuantity())));
        return orderLine.getOpenQuantity().equals(orderLine.getExpectedQuantity());
    }

    public List<OrderLine> getAvailableOrderLinesForMPS(Long warehouseId, Long itemId) {

        List<OrderLine> orderLines = orderLineRepository.findOpenOrderLinesByItem(itemId);

        return orderLines;
    }

    public OrderLine addRequestReturnQuantity(Long warehouseId, Long orderLineId, Long requestReturnQuantity) {
        OrderLine orderLine = findById(orderLineId);
        orderLine.setRequestedReturnQuantity(
                Objects.isNull(orderLine.getRequestedReturnQuantity()) ?
                        0l : orderLine.getRequestedReturnQuantity()
                +
                        requestReturnQuantity
        );
        return saveOrUpdate(orderLine);
    }

    public OrderLine addActualReturnQuantity(Long warehouseId, Long orderLineId, Long actualReturnQuantity) {
        OrderLine orderLine = findById(orderLineId);
        orderLine.setActualReturnQuantity(
                Objects.isNull(orderLine.getActualReturnQuantity()) ?
                        0l : orderLine.getActualReturnQuantity()
                        +
                        actualReturnQuantity
        );
        return saveOrUpdate(orderLine);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for order line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        orderLineRepository.processItemOverride(oldItemId, newItemId, warehouseId);
    }

    public OrderLine saveOrderLineData(Long warehouseId, Long clientId, Order order, OrderLineCSVWrapper orderLineCSVWrapper) {


        return saveOrUpdate(
                convertFromWrapper(warehouseId, clientId, orderLineCSVWrapper, order)
        );
    }

    public Long getPalletQuantityEstimation(OrderLine orderLine, Long quantity) {
        return getPalletQuantityEstimation(orderLine.getItemId(), quantity);
    }
    public Long getPalletQuantityEstimation(Long itemId, Long quantity) {
        Item item = inventoryServiceRestemplateClient.getItemById(itemId);
        if (Objects.isNull(item)) {
            return quantity;
        }
        // get the default item package type from the item
        return getPalletQuantityEstimation(item, quantity);
    }
    public Long getPalletQuantityEstimation(Item item, Long quantity) {
        ItemPackageType defaultItemPackageType = item.getDefaultItemPackageType();
        if (Objects.isNull(defaultItemPackageType)) {
            logger.debug("Can't find default item package type for item {}", item.getName());
            return quantity;
        }
        return getPalletQuantityEstimation(defaultItemPackageType, quantity);

    }

    public Long getPalletQuantityEstimation(ItemPackageType itemPackageType, Long quantity) {
        // get the LPN uom or max UOM
        Long palletQuantity = 1l;
        if (Objects.nonNull(itemPackageType.getTrackingLpnUOM())) {
            palletQuantity = itemPackageType.getTrackingLpnUOM().getQuantity();
        }
        else {
            for (ItemUnitOfMeasure itemUnitOfMeasure : itemPackageType.getItemUnitOfMeasures()) {
                if (itemUnitOfMeasure.getQuantity() > palletQuantity) {
                    palletQuantity = itemUnitOfMeasure.getQuantity();
                }
            }
        }
        return (long)Math.ceil(quantity * 1.0 / palletQuantity);
    }
}

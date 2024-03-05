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
import com.garyzhangscm.cwms.outbound.clients.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private OrderCancellationRequestService orderCancellationRequestService;
    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

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
    @Autowired
    private UserService userService;
    @Autowired
    private PickReleaseService pickReleaseService;
    @Autowired
    private WalmartShippingCartonLabelService walmartShippingCartonLabelService;
    @Autowired
    private TargetShippingCartonLabelService targetShippingCartonLabelService;
    @Autowired
    private PalletPickLabelContentService palletPickLabelContentService;




    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultMap = new ConcurrentHashMap<>();




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
                               String numbers,
                               String status,
                               ZonedDateTime startCompleteTime,
                               ZonedDateTime endCompleteTime,
                               LocalDate specificCompleteDate,
                               ZonedDateTime startCreatedTime,
                               ZonedDateTime endCreatedTime,
                               LocalDate specificCreatedDate,
                               String category,
                               String customerName,
                               Long customerId,
                               Long clientId,
                               Long trailerAppointmentId,
                               String poNumber,
                               Boolean loadDetails,
                               ClientRestriction clientRestriction) {

        List<Order> orders =  orderRepository.findAll(
                (Root<Order> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    criteriaQuery.distinct(true);

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }
                    if (StringUtils.isNotBlank(numbers)) {

                        CriteriaBuilder.In<String> inNumbers = criteriaBuilder.in(root.get("number"));
                        for(String orderNumber : numbers.split(",")) {
                            inNumbers.value(orderNumber);
                        }
                        predicates.add(criteriaBuilder.and(inNumbers));
                    }



                    if (StringUtils.isNotBlank(status)) {
                        OrderStatus orderStatus = OrderStatus.valueOf(status);
                        predicates.add(criteriaBuilder.equal(root.get("status"), orderStatus));

                    }

                    if (StringUtils.isNotBlank(category)) {
                        OrderCategory orderCategory = OrderCategory.valueOf(category);
                        predicates.add(criteriaBuilder.equal(root.get("category"), orderCategory));

                    }
                    if (Strings.isNotBlank(poNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("poNumber"), poNumber));
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
                        LocalDateTime dateStartTime = specificCompleteDate.atStartOfDay();
                        LocalDateTime dateEndTime = specificCompleteDate.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("completeTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }

                    if (Objects.nonNull(startCreatedTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startCreatedTime));

                    }

                    if (Objects.nonNull(endCreatedTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endCreatedTime));

                    }
                    if (Objects.nonNull(specificCreatedDate)) {
                        LocalDateTime dateStartTime = specificCreatedDate.atStartOfDay();
                        LocalDateTime dateEndTime = specificCreatedDate.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }

                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("clientId"), clientId));
                    }

                    if (Objects.nonNull(customerId)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("shipToCustomerId"), customerId));
                    }
                    else  if (Strings.isNotBlank(customerName)) {

                        Customer customer = commonServiceRestemplateClient.getCustomerByName(null, warehouseId, customerName);

                            predicates.add(criteriaBuilder.equal(
                                    root.get("shipToCustomerId"), customer.getId()));
                    }
                    if (Objects.nonNull(trailerAppointmentId)) {

                        logger.debug("We will query by trailer appointment id {}", trailerAppointmentId);
                        Join<Order, OrderLine> joinOrderLine = root.join("orderLines", JoinType.INNER);
                        Join<OrderLine, ShipmentLine> joinShipmentLine = joinOrderLine.join("shipmentLines", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment = joinShipmentLine.join("shipment", JoinType.INNER);
                        Join<Shipment, Stop> joinStop = joinShipment.join("stop", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinStop.get("trailerAppointmentId"), trailerAppointmentId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    return Objects.isNull(clientRestriction) ?
                            predicate :
                            clientRestriction.addClientRestriction(predicate,
                                    root, criteriaBuilder);
/**
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl

                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction) ||
                            !Boolean.TRUE.equals(clientRestriction.getThreePartyLogisticsFlag()) ||
                            Boolean.TRUE.equals(clientRestriction.getAllClientAccess())) {
                        // not a 3pl warehouse, let's not put any restriction on the client
                        // (unless the client restriction is from the web request, which we already
                        // handled previously
                        return predicate;
                    }


                    // build the accessible client list predicated based on the
                    // client ID that the user has access
                    Predicate accessibleClientListPredicate;
                    if (clientRestriction.getClientAccesses().trim().isEmpty()) {
                        // the user can't access any client, then the user
                        // can only access the non 3pl data
                        accessibleClientListPredicate = criteriaBuilder.isNull(root.get("clientId"));
                    }
                    else {
                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientRestriction.getClientAccesses().trim().split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        accessibleClientListPredicate = criteriaBuilder.and(inClientIds);
                    }

                    if (Boolean.TRUE.equals(clientRestriction.getNonClientDataAccessible())) {
                        // the user can access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.isNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }
                    else {

                        // the user can NOT access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.and(
                                        criteriaBuilder.isNotNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }
 **/

                },
                Sort.by(Sort.Direction.DESC, "createdTime")
        );

        if (orders.size() > 0 && loadDetails) {
            loadOrderAttribute(orders);
        }


        // calculateStatisticQuantities(orders);

        return orders;

    }

    public List<Order> findAll(Long warehouseId, String number, String numbers, String status,
                               ZonedDateTime startCompleteTime, ZonedDateTime endCompleteTime,
                               LocalDate specificCompleteDate,
                               ZonedDateTime startCreatedTime, ZonedDateTime endCreatedTime,
                               LocalDate specificCreatedDate,
                               String category, String customerName, Long customerId,
                               Long clientId,
                              Long trailerAppointmentId, String poNumber,
                              ClientRestriction clientRestriction) {
        return findAll(warehouseId, number, numbers, status,
                startCompleteTime, endCompleteTime, specificCompleteDate,
                startCreatedTime, endCreatedTime, specificCreatedDate,
                category, customerName, customerId,
                clientId, trailerAppointmentId,  poNumber, true, clientRestriction);
    }

    /**
     * Return true if the order only have one line
     * @param order
     * @return
     */
    public boolean isSingleLineOrder(Order order) {
        return order.getOrderLines().size() == 1;
    }

    /**
     * Return true if the order has line(s) that only have quantity of 1
     * @param order
     * @return
     */
    public boolean isSingleUnitQuantityOrder(Order order) {
        return order.getOrderLines().size() > 0 &&
                order.getOrderLines().stream().noneMatch(
                        orderLine -> orderLine.getExpectedQuantity() > 1
                );
    }

    /**
     * Return true if the order has line(s) that only have quantity of 1 case
     * @param order
     * @return
     */
    public boolean isSingleCaseQuantityOrder(Order order) {
        return order.getOrderLines().size() > 0 &&
                order.getOrderLines().stream().noneMatch(
                        orderLine -> {
                            if (Objects.isNull(orderLine.getItem())) {
                                orderLine.setItem(
                                        inventoryServiceRestemplateClient.getItemById(
                                                orderLine.getItemId()
                                        )
                                );
                            }
                            if (Objects.isNull(orderLine.getItem()) ||
                                Objects.isNull(orderLine.getItem().getDefaultItemPackageType()) ||
                                Objects.isNull(orderLine.getItem().getDefaultItemPackageType().getCaseItemUnitOfMeasure())) {
                                // return false if we can't get the item for one of the order line
                                // so the item wil fail in the function isSingleCaseQuantityOrder
                                return true;
                            }
                            return orderLine.getExpectedQuantity() !=
                                    orderLine.getItem().getDefaultItemPackageType().getCaseItemUnitOfMeasure().getQuantity();

                        }
                );
    }


    public List<Order> findWavableOrders(Long warehouseId, String orderNumber,
                                         Long clientId,
                                         String customerName, Long customerId,
                                         ZonedDateTime startCreatedTime,
                                         ZonedDateTime endCreatedTime,
                                         LocalDate specificCreatedDate,
                                         Boolean singleOrderLineOnly,
                                         Boolean singleOrderQuantityOnly,
                                         Boolean singleOrderCaseQuantityOnly,
                                         ClientRestriction clientRestriction,
                                         int orderNumberCap) {

        List<Order> orders = findAll(warehouseId,
                orderNumber, null, OrderStatus.OPEN.toString(),
                null,
                null,
                null,
                startCreatedTime,
                endCreatedTime,
                specificCreatedDate,
                null,
                customerName,
                customerId,
                clientId,
                null,
                null,
                false, clientRestriction);

        if (orders.size() > orderNumberCap) {
            orders = orders.subList(0, orderNumberCap);
        }

        logger.debug("Get {} orders for waving", orders.size());

        // skip the completed ones and any orders that doesn't have any open quantity
        List<Order> waveableOrders =  orders.stream()
                .filter(
             order -> {
                if (Boolean.TRUE.equals(singleOrderLineOnly) && !isSingleLineOrder(order)) {
                    logger.debug("Skip order {}. We will need to return single line order but the order has mutliple lines",
                            order.getNumber());
                    return false;
                }
                 if (Boolean.TRUE.equals(singleOrderQuantityOnly) && !isSingleUnitQuantityOrder(order)) {
                     logger.debug("Skip order {}. We will need to return single quantity order but the order has lines with multiple quantity",
                             order.getNumber());
                     return false;
                 }
                 if (Boolean.TRUE.equals(singleOrderCaseQuantityOnly) && !isSingleCaseQuantityOrder(order)) {
                     logger.debug("Skip order {}. We will need to return single case quantity order but the order lines with multiple case quantity",
                             order.getNumber());
                     return false;
                 }
                return true;
             }
        ).filter(
                order -> order.getOrderLines().stream().anyMatch(
                        orderLine -> orderLine.getOpenQuantity() > 0
                )
        ).map(
                // remove the lines that doesn't have any open quantity
                order -> {
                    Iterator<OrderLine> orderLineIterator = order.getOrderLines().iterator();
                    while(orderLineIterator.hasNext()) {
                        OrderLine orderLine = orderLineIterator.next();
                        if (orderLine.getOpenQuantity() == 0) {
                            orderLineIterator.remove();
                        }
                    }
                    return order;
                }
        ).filter(
                order -> !order.getOrderLines().isEmpty()
        ).collect(Collectors.toList());

        if (waveableOrders.size() > 0) {
            loadOrderAttribute(waveableOrders);
        }
        return waveableOrders;

    }
/**
    public Order findByNumber(Long warehouseId, String number, boolean loadDetails) {
        return findByNumber(warehouseId, null, number, loadDetails);
    }
 **/
    public Order findByNumber(Long warehouseId, Long clientId, String number, boolean loadDetails) {
        Order order = Objects.isNull(clientId) ?
                orderRepository.findByWarehouseIdAndNumber(warehouseId, number)
                :
                orderRepository.findByWarehouseIdAndClientIdAndNumber(warehouseId, clientId, number);

        if (order != null && loadDetails) {
            loadOrderAttribute(order);
        }
        return order;
    }
/**
    public Order findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }
   **/
    public Order findByNumber(Long warehouseId, Long clientId, String number) {
        return findByNumber(warehouseId, clientId, number, true);
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

        if (Objects.nonNull(order.getSupplierId()) && Objects.isNull(order.getSupplier())) {
            order.setSupplier(
                    commonServiceRestemplateClient.getSupplierById(
                            order.getSupplierId()
                    )
            );
        }

        if (Objects.nonNull(order.getCarrierId()) && Objects.isNull(order.getCarrier())) {
            order.setCarrier(
                    commonServiceRestemplateClient.getCarrierById(
                            order.getCarrierId()
                    )
            );
        }

        if (Objects.nonNull(order.getCarrierServiceLevelId()) && Objects.isNull(order.getCarrierServiceLevel())) {
            order.setCarrierServiceLevel(
                    commonServiceRestemplateClient.getCarrierServiceLevelById(
                            order.getCarrierServiceLevelId()
                    )
            );
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
        return save(order, true);
    }
    public Order save(Order order, boolean loadDetails) {
        // send alert for new order or order change
        boolean newOrderFlag = false;
        if (Objects.isNull(order.getId())) {
            newOrderFlag = true;
        }
        if (!newOrderFlag) {
            validateOrderForModification(order);
        }
        // in case the order is created out of context, we will need to
        // setup the created by in the context and then pass the username
        // in the down stream so that when we send alert, the alert will
        // contain the right username
        // example: when we create order via uploading CSV file,
        // 1. we will save the username in the main thread
        // 2. in a separate thread, we will create the order according to the
        //    csv file and setup the order's create by with the username from
        //    the main thread
        // 3. we will fetch the right username here(who upload the file) and use
        //    it to send alert
        String username = order.getCreatedBy();

        Order newOrder = orderRepository.save(order);
        if (loadDetails) {

            loadOrderAttribute(newOrder);
        }
        if (newOrderFlag) {
            sendAlertForOrderCreation(order, username);
        }
        else {
            sendAlertForOrderModification(order, username);
        }
        // for new order, let's see if we will need to request shippping label for it

        if (newOrderFlag) {
            newOrder.getOrderLines().stream().filter(
                    orderLine -> Boolean.TRUE.equals(orderLine.getAutoRequestShippingLabel())
            ).forEach(
                    orderLine -> {
                        logger.debug("start a new thread to request the shipping label as it may take a while");
                        new Thread(() -> orderLineService.autoRequestShippingLabel(orderLine)).start();
                    }
            );
        }

        return newOrder;
    }


    /**
     * Check if we can modify an existing order
     * @param order
     */
    private void validateOrderForModification(Order order) {
        if (Objects.isNull(order.getId())) {
            // if the id of the order is null, then we assume it is
            // a new order
            return;
        }

        // if there's already active shipment for this order, let's
        // disallow the user to change the
        if (order.getStatus().equals(OrderStatus.CANCELLED) || order.getStatus().equals(OrderStatus.COMPLETE)) {
            // OK, if we are try to cancel the order or complete the order
            // we will always allow
            return;
        }
        // see if we are try to chance the quantity.

    }

    public Order saveOrUpdate(Order order) {
        return saveOrUpdate(order, true);
    }
    public Order saveOrUpdate(Order order, boolean loadDetails) {
        if (Objects.isNull(order.getId()) &&
                Objects.nonNull(findByNumber(order.getWarehouseId(), order.getClientId(), order.getNumber(), false))) {
            order.setId(findByNumber(order.getWarehouseId(), order.getClientId(), order.getNumber(), false).getId());
        }
        return save(order, loadDetails);
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

/**
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

**/

    private List<OrderLineCSVWrapper> loadDataWithLine(File file) throws IOException {


        // return fileService.loadData(file, getCsvSchemaWithLine(), OrderLineCSVWrapper.class);
        return fileService.loadData(file, OrderLineCSVWrapper.class);
    }
/**
    private CsvSchema getCsvSchemaWithLine() {
        return CsvSchema.builder().
                addColumn("client").
                addColumn("order").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                addColumn("allocationStrategyType").
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
                build().withHeader();
    }
 **/

    /**
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
     **/
/**
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
            Customer shipToCustomer = commonServiceRestemplateClient.getCustomerByName(warehouse.getCompanyId(),
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
            Customer billToCustomer = commonServiceRestemplateClient.getCustomerByName(warehouse.getCompanyId(), warehouse.getId(),
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
**/
    public Order convertFromWrapper(Long warehouseId,
                                     OrderLineCSVWrapper orderLineCSVWrapper) {

        Order order = new Order();
        order.setNumber(orderLineCSVWrapper.getOrder());


        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        order.setWarehouseId(warehouse.getId());

        boolean billToCustomerSameAsShipToCustomer =
                Strings.isNotBlank(orderLineCSVWrapper.getBillToCustomerSameAsShipToCustomer()) &&
                        (orderLineCSVWrapper.getBillToCustomerSameAsShipToCustomer().equalsIgnoreCase("1") ||
                            orderLineCSVWrapper.getBillToCustomerSameAsShipToCustomer().equalsIgnoreCase("true"));

        boolean billToAddressSameAsShipToAddress =
                Strings.isNotBlank(orderLineCSVWrapper.getBillToAddressSameAsShipToAddress()) &&
                        (orderLineCSVWrapper.getBillToAddressSameAsShipToAddress().equalsIgnoreCase("1") ||
                        orderLineCSVWrapper.getBillToAddressSameAsShipToAddress().equalsIgnoreCase("true"));

        logger.debug("billToCustomerSameAsShipToCustomer?: {}, billToAddressSameAsShipToAddress?: {}");
        if (billToCustomerSameAsShipToCustomer) {
            orderLineCSVWrapper.setBillToCustomer(
                    orderLineCSVWrapper.getShipToCustomer()
            );
        }
        if (billToAddressSameAsShipToAddress) {
            orderLineCSVWrapper.setBillToContactorFirstname(orderLineCSVWrapper.getShipToContactorFirstname());
            orderLineCSVWrapper.setBillToContactorLastname(orderLineCSVWrapper.getShipToContactorLastname());
            orderLineCSVWrapper.setBillToAddressCountry(orderLineCSVWrapper.getShipToAddressCountry());
            orderLineCSVWrapper.setBillToAddressState(orderLineCSVWrapper.getShipToAddressState());
            orderLineCSVWrapper.setBillToAddressCounty(orderLineCSVWrapper.getShipToAddressCounty());
            orderLineCSVWrapper.setBillToAddressCity(orderLineCSVWrapper.getShipToAddressCity());
            orderLineCSVWrapper.setBillToAddressDistrict(orderLineCSVWrapper.getShipToAddressDistrict());
            orderLineCSVWrapper.setBillToAddressLine1(orderLineCSVWrapper.getShipToAddressLine1());
            orderLineCSVWrapper.setBillToAddressLine2(orderLineCSVWrapper.getShipToAddressLine2());
            orderLineCSVWrapper.setBillToAddressPostcode(orderLineCSVWrapper.getShipToAddressPostcode());
        }


        // if we specify the ship to customer, we load information with the customer
        if (!StringUtils.isBlank(orderLineCSVWrapper.getShipToCustomer())) {
            Customer shipToCustomer = commonServiceRestemplateClient.getCustomerByName(warehouse.getCompanyId(),
                    warehouse.getId(), orderLineCSVWrapper.getShipToCustomer());

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
            order.setShipToContactorFirstname(orderLineCSVWrapper.getShipToContactorFirstname());
            order.setShipToContactorLastname(orderLineCSVWrapper.getShipToContactorLastname());
            order.setShipToContactorPhoneNumber(orderLineCSVWrapper.getShipToContactorPhoneNumber());
            order.setShipToAddressCountry(orderLineCSVWrapper.getShipToAddressCountry());
            order.setShipToAddressState(orderLineCSVWrapper.getShipToAddressState());
            order.setShipToAddressCounty(orderLineCSVWrapper.getShipToAddressCounty());
            order.setShipToAddressCity(orderLineCSVWrapper.getShipToAddressCity());
            order.setShipToAddressDistrict(orderLineCSVWrapper.getShipToAddressDistrict());
            order.setShipToAddressLine1(orderLineCSVWrapper.getShipToAddressLine1());
            order.setShipToAddressLine2(orderLineCSVWrapper.getShipToAddressLine2());
            order.setShipToAddressPostcode(orderLineCSVWrapper.getShipToAddressPostcode());
        }

        if (!StringUtils.isBlank(orderLineCSVWrapper.getBillToCustomer())) {
            Customer billToCustomer = commonServiceRestemplateClient.getCustomerByName(warehouse.getCompanyId(), warehouse.getId(),
                    orderLineCSVWrapper.getBillToCustomer());

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
            order.setBillToContactorFirstname(orderLineCSVWrapper.getBillToContactorFirstname());
            order.setBillToContactorLastname(orderLineCSVWrapper.getBillToContactorLastname());
            order.setBillToAddressCountry(orderLineCSVWrapper.getBillToAddressCountry());
            order.setBillToAddressState(orderLineCSVWrapper.getBillToAddressState());
            order.setBillToAddressCounty(orderLineCSVWrapper.getBillToAddressCounty());
            order.setBillToAddressCity(orderLineCSVWrapper.getBillToAddressCity());
            order.setBillToAddressDistrict(orderLineCSVWrapper.getBillToAddressDistrict());
            order.setBillToAddressLine1(orderLineCSVWrapper.getBillToAddressLine1());
            order.setBillToAddressLine2(orderLineCSVWrapper.getBillToAddressLine2());
            order.setBillToAddressPostcode(orderLineCSVWrapper.getBillToAddressPostcode());
        }


        if (!StringUtils.isBlank(orderLineCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(), orderLineCSVWrapper.getClient());
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
    public Order allocate(Long orderId, Boolean asynchronous) {

        Order order = findById(orderId);

        logger.debug(">>>    Start to allocate order  {} ,asynchronous? : {}  <<<",
                order.getNumber(), asynchronous);

        // if the order is already cancelled or request cancellation,
        // raise an error
        if (order.getStatus().equals(OrderStatus.COMPLETE)) {

            throw OrderOperationException.raiseException("Order " + order.getNumber() + " is already complete, can't allocate it");
        }
        if (order.getStatus().equals(OrderStatus.CANCELLED)) {

            throw OrderOperationException.raiseException("Order " + order.getNumber() + " is already cancelled, can't allocate it");
        }
        if (Boolean.TRUE.equals(order.getCancelRequested())) {

            throw OrderOperationException.raiseException("There's a cancel request on Order " + order.getNumber() + ", " +
                    "please cancel it before you want to continue");
        }

        // When we directly allocate the order, we will
        // 1. create a fake shipment for the order
        // 2. allocate the shipment
        // we will only plan the allocatable order lines into the shipment lines
        List<OrderLine> allocatableOrderLines =
                order.getOrderLines().stream().filter(
                        orderLine -> !Boolean.TRUE.equals(orderLine.getNonAllocatable())
                ).collect(Collectors.toList());

        if (allocatableOrderLines.size() == 0) {
            throw OrderOperationException.raiseException("There's no allocatable order line in this order");
        }

        order.setStatus(OrderStatus.ALLOCATING);


        // check if we will need to allocate asynchronously
        // 1. if the client explicitly want asynchronous
        // 2. if the warehouse is configured to allocate asynchronously
        if (Objects.isNull(asynchronous)) {
            // TO-DO: Will need to use pallet quantity instead of quantity
            long totalPalletQuantity = allocatableOrderLines.stream().map(
                    orderLine -> orderLineService.getPalletQuantityEstimation(
                            orderLine, orderLine.getExpectedQuantity() - orderLine.getShippedQuantity()
                    )
            ).mapToLong(Long::longValue).sum();

            asynchronous  = outboundConfigurationService.isSynchronousAllocationRequired(
                    order.getWarehouseId(),totalPalletQuantity);
        }

        logger.debug("allocate Asynchronously or Synchronously? {}",
                Boolean.TRUE.equals(asynchronous) ? "Asynchronously" : "Synchronously");
        if (Boolean.TRUE.equals(asynchronous)) {
            new Thread(() -> {

                logger.debug("start to allocate the order asynchronously");
                allocate(order, allocatableOrderLines);

                logger.debug("Asynchronously allocation is done");
                order.setStatus(OrderStatus.INPROCESS);

                saveOrUpdate(order);

            }).start();
        }
        else {
            allocate(order, allocatableOrderLines);
            logger.debug("Synchronously allocation is done");
            order.setStatus(OrderStatus.INPROCESS);

        }
        return saveOrUpdate(order);
    }

    @Transactional
    public void allocate(Order order, List<OrderLine> allocatableOrderLines) {
        shipmentService.planShipments(order.getWarehouseId(),allocatableOrderLines);


        // ok, if we are here, we may ends up with multiple shipments
        // with lines for the order,
        // let's allocate all of the shipment lines
        List<AllocationResult> allocationResults
                = shipmentLineService.findByOrderNumber(order.getWarehouseId(), order.getNumber())
                    .stream()
                    .filter(shipmentLine -> shipmentLine.getOpenQuantity() >0)
                    .map(shipmentLine -> shipmentLineService.allocateShipmentLine(shipmentLine))
                    .collect(Collectors.toList());

        postAllocationProcess(allocationResults);

    }

    /**
     * Post allocation process. For pick allocate by order, we will only have single pick
     * we will only have list pick and bulk pick when allocate by wave
     * release the pick
     * @param allocationResults
     */
    private void postAllocationProcess(List<AllocationResult> allocationResults) {

        logger.debug("start post allocation process for {} allocation result",
                allocationResults.size());

        releaseSinglePicks(allocationResults);

    }

    /**
     * Release the picks of the wave, which are not in any group of
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


    @Transactional
    public Order stage(Long orderId, boolean ignoreUnfinishedPicks) {
        Order order = findById(orderId);

        if (order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw OrderOperationException.raiseException(
                    "Can't stage the order " +
                            order.getNumber() + " as it is  already completed");
        }
        if (order.getStatus().equals(OrderStatus.CANCELLED)) {
            throw OrderOperationException.raiseException(
                    "Can't stage the order " +
                            order.getNumber() + " as it is  already cancelled");
        }
        if (Boolean.TRUE.equals(order.getCancelRequested())) {
            throw OrderOperationException.raiseException("There's a cancel request on the Order " + order.getNumber() + ", " +
                    "please cancel it before you want to continue");
        }

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

        if (order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw OrderOperationException.raiseException(
                    "Can't load the order " +
                            order.getNumber() + " as it is  already completed");
        }
        if (order.getStatus().equals(OrderStatus.CANCELLED)) {
            throw OrderOperationException.raiseException(
                    "Can't load the order " +
                            order.getNumber() + " as it is  already cancelled");
        }
        if (Boolean.TRUE.equals(order.getCancelRequested())) {
            throw OrderOperationException.raiseException("There's a cancel request on the Order " + order.getNumber() + ", " +
                    "please cancel it before you want to continue");
        }

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

        if (order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw OrderOperationException.raiseException(
                    "Can't dispatch the order " +
                            order.getNumber() + " as it is  already completed");
        }
        if (order.getStatus().equals(OrderStatus.CANCELLED)) {
            throw OrderOperationException.raiseException(
                    "Can't dispatch the order " +
                            order.getNumber() + " as it is  already cancelled");
        }
        if (Boolean.TRUE.equals(order.getCancelRequested())) {
            throw OrderOperationException.raiseException("There's a cancel request on the Order " + order.getNumber() + ", " +
                    "please cancel it before you want to continue");
        }

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
    public Order completeOrder(Long orderId, Order completedOrder) {
        Order existingOrder = findById(orderId);
        // Let's make sure the order is still open
        validateOrderForComplete(existingOrder);

        if (existingOrder.getCategory().isOutsourcingOrder()) {
            return completeOutsourcingOrder(existingOrder, completedOrder);
        }
        else {
            return completeWarehouseOrder(existingOrder);
        }
    }

    private void validateOrderForComplete(Order existingOrder) {

        if (existingOrder.getStatus().equals(OrderStatus.COMPLETE)) {
            throw OrderOperationException.raiseException(
                    "Can't complete the order " + existingOrder.getNumber() + " as it is already completed");
        }
        if (existingOrder.getStatus().equals(OrderStatus.CANCELLED)) {
            throw OrderOperationException.raiseException(
                    "Cancel complete the order " + existingOrder.getNumber() + " as it is already cancelled");
        }
        if (Boolean.TRUE.equals(existingOrder.getCancelRequested())) {

            throw OrderOperationException.raiseException("There's a cancel request on Order " + existingOrder.getNumber() + ", " +
                    "please cancel it before you want to continue");
        }
    }

    /**
     * Complete the order that is fulfilled by the warehouse
     * @param order
     * @return
     */
    @Transactional
    private Order completeWarehouseOrder(Order order) {

        // Let's complete all the shipments related to this
        // order
        order.getOrderLines()
                .stream()
                .flatMap(orderLine -> orderLine.getShipmentLines().stream())
                .map(shipmentLine -> shipmentLine.getShipment())
                .distinct()
                .filter(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED &&
                        shipment.getStatus() != ShipmentStatus.DISPATCHED)
                .forEach(shipment -> {
                            shipment.setOrder(order);

                            shipmentService.completeShipment(shipment);
                        });

        order.setStatus(OrderStatus.COMPLETE);
        order.setCompleteTime(ZonedDateTime.now(ZoneOffset.UTC));



        // we will on longer send order confirmation integration when we complete the order
        // instead, we will send order configmration integration when we complete the shipment
        // logger.debug("Start to send order confirmation after the order {} is marked as completed",
        //         order.getNumber());
        // sendOrderConfirmationIntegration(order);


        // release the location that is reserved by order
        releaseLocationsAfterOrderComplete(order);


        Order completedOrder =  saveOrUpdate(order);
        orderActivityService.sendOrderActivity(
                orderActivityService.createOrderActivity(
                        completedOrder.getWarehouseId(), completedOrder, OrderActivityType.ORDER_COMPLETE
                ));

        return completedOrder;

    }

    private void releaseLocationsAfterOrderComplete(Order order) {

        warehouseLayoutServiceRestemplateClient.releaseLocations(order.getWarehouseId(), order);
        order.setStageLocationId(null);
        order.setStageLocationGroupId(null);
    }

    /**
     * Complete the order that is fulfilled by the 3rd party
     * @param existingOrder order informaiton saved in our database
     * @param completedOrder the order with shipped quantity information from 3rd party
     * @return
     */
    @Transactional
    private Order completeOutsourcingOrder(Order existingOrder, Order completedOrder) {

        existingOrder.getOrderLines()
                .forEach(
                        orderLine -> {
                            // find the matched quantity from the completed order
                            Optional<OrderLine> matchedOrderLine = completedOrder.getOrderLines()
                                    .stream().filter(
                                            completedOrderLine -> StringUtils.equals(
                                                    orderLine.getNumber(),
                                                    completedOrderLine.getNumber()
                                            )
                                    ).findFirst();
                            if (matchedOrderLine.isPresent()) {
                                orderLine.setShippedQuantity(
                                        matchedOrderLine.get().getShippedQuantity()
                                );
                            }
                        }
                );
        existingOrder.setStatus(OrderStatus.COMPLETE);
        existingOrder.setCompleteTime(ZonedDateTime.now(ZoneOffset.UTC));

        logger.debug("Start to send order confirmation after the order {} is marked as completed",
                 existingOrder.getNumber());
         sendOrderConfirmationIntegration(existingOrder);

        // release the location that is reserved by order

        warehouseLayoutServiceRestemplateClient.releaseLocations(existingOrder.getWarehouseId(), existingOrder);
        existingOrder.setStageLocationId(null);
        existingOrder.setStageLocationGroupId(null);

        existingOrder = saveOrUpdate(existingOrder);
        orderActivityService.sendOrderActivity(
                orderActivityService.createOrderActivity(
                        existingOrder.getWarehouseId(), existingOrder, OrderActivityType.ORDER_COMPLETE
                ));

        return existingOrder;

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

        // Setup display field
        picks.forEach(
                pick -> {
                    // set the inventory attribute in one string
                    StringBuilder inventoryAttribute = new StringBuilder()
                            .append(Strings.isBlank(pick.getColor()) ? "" : pick.getColor()).append("    ")
                            .append(Strings.isBlank(pick.getProductSize()) ? "" : pick.getProductSize()).append("    ")
                            .append(Strings.isBlank(pick.getStyle()) ? "" : pick.getStyle())
                            .append(Strings.isBlank(pick.getAllocateByReceiptNumber()) ? "" : pick.getAllocateByReceiptNumber());
                    pick.setInventoryAttribute(inventoryAttribute.toString());

                    // setup the quantity by UOM from the pickable inventory in the source location
                    List<Inventory> pickableInventory = inventoryServiceRestemplateClient.getPickableInventory(
                            pick.getItemId(), pick.getInventoryStatusId(), pick.getSourceLocationId(), null,
                            pick.getColor(), pick.getProductSize(), pick.getStyle(), pick.getAllocateByReceiptNumber());

                    StringBuilder pickQuantityByUOM = new StringBuilder();
                    pickQuantityByUOM.append(pick.getQuantity());

                    if (pickableInventory != null && !pickableInventory.isEmpty() &&
                            Objects.nonNull(pickableInventory.get(0).getItemPackageType())) {
                        // get the information from the first inventory of the list
                        // we will assume all the pickable inventory in the same location
                        // has the same item UOM information. If the location is mixed with
                        // different package type, the warehouse may have some difficulty for picking
                        ItemUnitOfMeasure stockItemUnitOfMeasure =
                                pickableInventory.get(0).getItemPackageType().getStockItemUnitOfMeasure();
                        ItemUnitOfMeasure caseItemUnitOfMeasure =
                                pickableInventory.get(0).getItemPackageType().getCaseItemUnitOfMeasure();

                        if (Objects.nonNull(stockItemUnitOfMeasure) &&
                                Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {

                            pickQuantityByUOM.append(" ")
                                    .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                        }
                        // if the item package type has case UOM defined, show the quantity in case UOM as well.
                        if (Objects.nonNull(caseItemUnitOfMeasure) &&
                                Objects.nonNull(caseItemUnitOfMeasure.getUnitOfMeasure())) {

                            Long caseQuantity = pick.getQuantity() / caseItemUnitOfMeasure.getQuantity();
                            Long leftOverQuantity = pick.getQuantity() % caseItemUnitOfMeasure.getQuantity();

                            pickQuantityByUOM.append(" (").append(caseQuantity).append(" ")
                                    .append(caseItemUnitOfMeasure.getUnitOfMeasure().getName());
                            if (leftOverQuantity > 0) {
                                pickQuantityByUOM.append(", ").append(leftOverQuantity);
                                if (Objects.nonNull(stockItemUnitOfMeasure) &&
                                        Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {
                                    pickQuantityByUOM.append(" ")
                                            .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                                }
                            }
                            pickQuantityByUOM.append(")");
                        }
                    }
                    pick.setQuantityByUOM(pickQuantityByUOM.toString());
                }
        );


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
                .filter(pick -> Objects.isNull(pick.getWorkTaskId()))
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


        Order newOrder =  saveOrUpdate(order, false);

        orderActivityService.sendOrderActivity(
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
                        ItemUnitOfMeasure itemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasure();
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

        // for outsourcing order we will get supplier's address
        // as the shipping from address, otherwise,
        // we will get the warehouse address as the ship from address
        if (order.getCategory().isOutsourcingOrder()) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierById(order.getSupplierId());
            if (Objects.nonNull(supplier)) {

                report.addParameter("ship_from_address_line1",
                        supplier.getAddressLine1());
                report.addParameter("ship_from_address_city_state_zipcode",
                        supplier.getAddressCity() + ", " +
                                supplier.getAddressState() + " " +
                                supplier.getAddressPostcode());
            }
            else {
                report.addParameter("ship_from_address_line1",
                        "N/A");
                report.addParameter("ship_from_address_city_state_zipcode",
                        "N/A");
            }
        }
        else {

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
        }

        // get the ship to address
        report.addParameter("ship_to_address_line1",
                order.getShipToAddressLine1());
        report.addParameter("ship_to_address_city_state_zipcode",
                order.getShipToAddressCity() + ", " +
                        order.getShipToAddressState() + " " +
                        order.getShipToAddressPostcode());



    }

    /**
     * Setup order's packing list data
     * @param report
     * @param order
     */
    private void setupOrderPackingListData(Report report, Order order) {
        logger.debug("setup packing list data for order {}, category {}, is outsourcing order? {}",
                order.getNumber(),
                order.getCategory(),
                order.getCategory().isOutsourcingOrder());
        if (order.getCategory().isOutsourcingOrder()) {
            setupOutsourcingOrderPackingListData(report, order);
        }
        else {
            setupWarehouseOrderPackingListData(report, order);
        }

    }
    /**
     * Setup the order packing list data for order that fulfilled by 3rd party
     * @param report
     * @param order
     */
    private void setupOutsourcingOrderPackingListData(Report report, Order order) {

        // for outsourcing orders, we will printing whatever the 3rd party
        // told us that is shipped
        logger.debug("Start to setup packing list data for outsourcing order");
        List<PackingSlipData> packingSlipDataList = new ArrayList<>();

        Long totalShippedQuantity = 0l;
        for(OrderLine orderLine : order.getOrderLines()) {

            packingSlipDataList.add(new PackingSlipData(
                    orderLine.getItem().getName(),
                    orderLine.getItem().getDescription(),
                    orderLine.getShippedQuantity(),
                    0
            ));
            totalShippedQuantity += orderLine.getShippedQuantity();
        }
        report.setData(packingSlipDataList);

        report.addParameter("total_shipped_quantity",
                totalShippedQuantity);
        report.addParameter("total_pallet_count",
                0);
    }

    /**
     * Setup the order's packing list's data for order that fulfilled by warehouse
     * @param report
     * @param order
     */
    private void setupWarehouseOrderPackingListData(Report report, Order order) {

        logger.debug("Start to setup packing list data for warehouse order");
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

        // for outsourcing order we will get supplier's address
        // as the shipping from address, otherwise,
        // we will get the warehouse address as the ship from address
        if (order.getCategory().isOutsourcingOrder()) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierById(order.getSupplierId());
            if (Objects.nonNull(supplier)) {
                report.addParameter("ship_from_name",
                        supplier.getName());
                report.addParameter("ship_from_address_line1",
                        supplier.getAddressLine1());
                report.addParameter("ship_from_address_city_state_zipcode",
                        supplier.getAddressCity() + ", " +
                                supplier.getAddressState() + " " +
                                supplier.getAddressPostcode());
            }
            else {
                report.addParameter("ship_from_address_line1",
                        "N/A");
                report.addParameter("ship_from_address_city_state_zipcode",
                        "N/A");
            }
        }
        else {
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
        if (order.getCategory().isOutsourcingOrder()) {
            setupOutsourcingOrderBillOfLadingData(report, order);
        }
        else {
            setupWarehouseOrderBillOfLadingData(report, order);
        }
    }

    /**
     * Setup BOL data for order fulfilled by 3rd party
     * @param report
     * @param order
     */
    private void setupOutsourcingOrderBillOfLadingData(Report report, Order order) {

        List<BillOfLadingData> billOfLadingDataList = new ArrayList<>();
        // for orders that fulfilled by 3rd party, we will just use the data that whatever
        // the 3rd party tell us
        Long totalShippedQuantity = 0l;
        for(OrderLine orderLine : order.getOrderLines()) {
            String itemFamily = Objects.nonNull(orderLine.getItem().getItemFamily()) ?
                    orderLine.getItem().getItemFamily().getName() : "N/A";
            String stockUOM =
                    Objects.isNull(orderLine.getItem()) ? "N/A" :
                            Objects.isNull(orderLine.getItem().getDefaultItemPackageType()) ? "N/A" :
                                    Objects.isNull(orderLine.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure()) ? "N/A" :
                                            Objects.isNull(orderLine.getItem().getDefaultItemPackageType()
                                                    .getStockItemUnitOfMeasure().getUnitOfMeasure()) ? "N/A" :
                                                        orderLine.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure().getName();
            billOfLadingDataList.add(new BillOfLadingData(
                    orderLine.getItem().getName(),
                    orderLine.getItem().getDescription(),
                    orderLine.getShippedQuantity(),
                    0,
                    stockUOM,
                    itemFamily,
                    "N/A"
            ));
            totalShippedQuantity += orderLine.getShippedQuantity();
        }

        report.setData(billOfLadingDataList);

        report.addParameter("total_shipped_quantity",
            totalShippedQuantity);
        report.addParameter("total_pallet_count",
        0);
    }

    /**
     * Setup BOL data for order fulfilled by warehouse
     * @param report
     * @param order
     */
    private void setupWarehouseOrderBillOfLadingData(Report report, Order order) {

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
                   Objects.nonNull(pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure()) &&
                   Objects.nonNull(pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure())) {
                logger.debug("item default package type: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getName());
                logger.debug("item default package type's item uom: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure().getId());
                logger.debug("item default package type's item stock uom: {}",
                        pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure().getName());

                stockUOMName = pickedInventory.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure().getUnitOfMeasure().getName();
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

    public String validateNewOrderNumber(Long warehouseId, Long clientId,  String orderNumber) {
        Order order =
                findByNumber(warehouseId, clientId, orderNumber, false);

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
        assignTrailerAppointment(findById(orderId), trailerAppointment);
    }
    public void assignTrailerAppointment(Order order, TrailerAppointment trailerAppointment) {
        logger.debug("Start to assign order to trailer appointment");

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

    /**
     * Generate manual pick for the order
     * @param orderId
     * @param lpn
     * @param pickWholeLPN
     * @return
     */
    public List<Pick> generateManualPick(Long orderId, String lpn, Boolean pickWholeLPN) {

        Order order = findById(orderId);

        validateOrderStatusForManualPick(order);

        List<Pick> picks = new ArrayList<>();
        ///// TO-DO
        return picks;
    }

    private void validateOrderStatusForManualPick(Order order) {
        if (order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw OrderOperationException.raiseException("Can't generate manual pick for order " +
                    order.getNumber() + " as its status is " +
                    order.getStatus() + " and not suitable for pick");
        }
    }

    public String saveOrderData(Long warehouseId,
                                     File localFile) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();

        fileUploadProgress.put(fileUploadProgressKey, 0.0);
        fileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());

        List<OrderLineCSVWrapper> orderLineCSVWrappers = loadDataWithLine(localFile);

        logger.debug("start to save {} order lines ", orderLineCSVWrappers.size());

        fileUploadProgress.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {
            int totalCount = orderLineCSVWrappers.size();
            int index = 0;
            for (OrderLineCSVWrapper orderLineCSVWrapper : orderLineCSVWrappers) {

                try {
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));
                    Client client = Strings.isNotBlank(orderLineCSVWrapper.getClient()) ?
                            commonServiceRestemplateClient.getClientByName(warehouseId, orderLineCSVWrapper.getClient())
                            : null;
                    Long clientId = Objects.isNull(client) ? null : client.getId();

                    Order order = findByNumber(warehouseId, clientId, orderLineCSVWrapper.getOrder());
                    if (Objects.isNull(order)) {
                        logger.debug("order {} is not created yet, let's create the order on the fly ", orderLineCSVWrapper.getOrder());
                        // the order is not created yet, let's

                        // we have to do it manually since the user name is only available in the main http session
                        // but we will create the receipt / receipt line in a separate transaction
                        order = convertFromWrapper(warehouseId, orderLineCSVWrapper);
                        order.setCreatedBy(username);
                        order = saveOrUpdate(order);
                    }
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.5));
                    logger.debug("start to create order line {} for item {}, quantity {}, for order {}",
                            orderLineCSVWrapper.getLine(),
                            orderLineCSVWrapper.getItem(),
                            orderLineCSVWrapper.getExpectedQuantity(),
                            order.getNumber());
                    orderLineService.saveOrderLineData(warehouseId, clientId, order, orderLineCSVWrapper);

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            orderLineCSVWrapper.toString(),
                            "success", ""
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receiving order upload file record: {}, \n error message: {}",
                            orderLineCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            orderLineCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));
                }
                finally {

                    index++;
                }
            }
        }).start();

        return fileUploadProgressKey;

    }

    private void clearFileUploadMap() {

        if (fileUploadProgress.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadProgress.keySet().iterator();
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

        if (fileUploadResultMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadResultMap.keySet().iterator();
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


    public double getOrderFileUploadProgress(String key) {
        return fileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getOrderFileUploadResult(Long warehouseId, String key) {
        return fileUploadResultMap.getOrDefault(key, new ArrayList<>());
    }

    private void sendAlertForOrderCreation(Order order,  String username) {

        sendAlertForOrder(order, AlertType.NEW_ORDER, username);
    }
    private void sendAlertForOrderModification(Order order, String username) {

        sendAlertForOrder(order, AlertType.MODIFY_ORDER, username);
    }
    private void sendAlertForOrderCancellationRequest(Order order, String username) {

        sendAlertForOrder(order, AlertType.REQUEST_ORDER_CANCELLATION, username);
    }
    private void sendAlertForOrderCancellation(Order order, String username) {

        sendAlertForOrder(order, AlertType.CANCEL_ORDER, username);
    }
    /**
     * Send alert for new order or changing order
     * @param order
     */
    private void sendAlertForOrder(Order order, AlertType alertType, String username) {

        if (Strings.isBlank(username)) {

            try {
                username = userService.getCurrentUserName();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                logger.debug("We got error while getting username from the session, let's just ignore.\nerror: {}",
                        ex.getMessage());
            }

        }
        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(order.getWarehouseId()).getCompanyId();
        StringBuilder alertParameters = new StringBuilder();
        alertParameters.append("number=").append(order.getNumber())
                .append("&lineCount=").append(order.getOrderLines().size());
        Alert alert = null;
        switch (alertType) {
            case NEW_ORDER:
                alert = new Alert(companyId, alertType,
                    "NEW-ORDER-" + companyId + "-" + order.getWarehouseId() + "-" + order.getNumber(),
                    "Outbound Order " + order.getNumber() + " created, by " + username,
                    "", alertParameters.toString());
            break;
            case MODIFY_ORDER:
                alert = new Alert(companyId, alertType,
                        "MODIFY-ORDER-" + companyId + "-" + order.getWarehouseId() + "-" +
                                order.getNumber() + "-" + System.currentTimeMillis(),
                        "Outbound Order " + order.getNumber() + " is changed, by " + username,
                        "", alertParameters.toString());
            break;
            case REQUEST_ORDER_CANCELLATION:
                alert = new Alert(companyId, alertType,
                        "REQUEST-ORDER-CANCELLATION" + companyId + "-" + order.getWarehouseId() +
                                "-" + order.getNumber() + "-" + System.currentTimeMillis(),
                        "Outbound Order " + order.getNumber() + " 's cancellation , requested by " + username,
                        "", alertParameters.toString());
            break;
            case CANCEL_ORDER:
                alert = new Alert(companyId, alertType,
                        "CANCEL_ORDER" + companyId + "-" + order.getWarehouseId() + "-" + order.getNumber(),
                        "Outbound Order " + order.getNumber() + "  is cancelled by " + username,
                        "", alertParameters.toString());
                break;


        }
        if (Objects.nonNull(alert)) {

            kafkaSender.send(alert);
        }
    }

    public List<OrderQueryWrapper> getOrdersForQuery(Long warehouseId, String number,
                                                     String numbers,
                                                     String status,
                                                     ClientRestriction clientRestriction) {

        List<Order> orders = findAll(warehouseId, number, numbers,
                status, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, true, clientRestriction);

        return orders.stream().map(order -> convertForQuery(order)).collect(Collectors.toList());
    }

    /**
     * Convert the order to the POJO for return of query request
     * @param order
     * @return
     */
    private OrderQueryWrapper convertForQuery(Order order) {
        return new OrderQueryWrapper(order);

    }


    public Order findOrder(Long warehouseId, Long orderId,
                           Long clientId,
                           String clientName, String orderNumber) {
        Order order = null;
        if (Objects.nonNull(orderId)) {
            order = findById(orderId);
        }
        else if (Strings.isNotBlank(orderNumber)) {
            // order number is passed in, let's see if we will need to
            // query by the client as well
            if (Strings.isBlank(clientName) && Objects.isNull(clientId)) {
                // for non client environment
                order = findByNumber(warehouseId, null, orderNumber);
            }
            else if (Objects.nonNull(clientId)){
                order = findByNumber(warehouseId, clientId, orderNumber );
            }
            else {
                // client name is passed in without client id
                Client client = commonServiceRestemplateClient.getClientByName(warehouseId, clientName);
                if (Objects.isNull(client)) {

                    throw OrderOperationException.raiseException("Invalid client by name " + clientName);
                }
                order = findByNumber(warehouseId, client.getId(), orderNumber );
            }
        }
        else {

            throw OrderOperationException.raiseException("At least one of order id or order number needs to be present");
        }
        return order;
    }
    public OrderCancellationRequest cancelOrder(Long warehouseId, Long orderId,
                             Long clientId,
                             String clientName, String orderNumber) {
        Order order = findOrder(warehouseId, orderId, clientId, clientName, orderNumber);
        if (Objects.isNull(order)) {

            throw OrderOperationException.raiseException("Not able to find order by parameters. " +
                    (Objects.nonNull(orderId) ? "order id = " + orderId : "" ) +
                            (Objects.nonNull(clientId) ? "client id = " + clientId : "" ) +
                            (Strings.isNotBlank(clientName) ? "client name = " + clientName : "" ) +
                            (Strings.isNotBlank(orderNumber) ? "order number = " + orderNumber : "" )
                    );
        }
        // ok, we get the right order. see if we can cancel it
        return startOrderCancellation(order);
    }

    public OrderCancellationRequest startOrderCancellation(Order order) {

        if (order.getStatus().equals(OrderStatus.CANCELLED)) {
            return orderCancellationRequestService.createFailedOrderCancellationRequest(
                    order, userService.getCurrentUserName(),
                    "Fail to cancel order " + order.getNumber() +
                            " as it is already cancelled"
            );
        }
        if (order.getStatus().equals(OrderStatus.COMPLETE)) {

            return orderCancellationRequestService.createFailedOrderCancellationRequest(
                    order, userService.getCurrentUserName(),
                    "Fail to cancel order " + order.getNumber() +
                            " as it is already completed"
            );
        }

        // save the order activity for order cancellation
        OrderActivity orderActivity = orderActivityService.createOrderActivity(
                order.getWarehouseId(), order, OrderActivityType.ORDER_CANCELLATION_REQUEST
        );
        orderActivityService.sendOrderActivity(orderActivity);
        // send alert for order cancellation request

        sendAlertForOrderCancellationRequest(order, "");

        if (pickService.findByOrder(order).size() > 0 || shortAllocationService.findByOrder(order).size() > 0) {
            // there're picks / short allocations on it, let's just mark it as cancel request and
            // let the user cancel those picks / short allocation first
            return sendCancelOrderRequest(order);

        }
        else {
            return markOrderCancelled(order);
        }

    }

    private OrderCancellationRequest sendCancelOrderRequest(Order order) {

        if (Boolean.TRUE.equals(order.getCancelRequested())) {
            // order request is already sent, do nothing
            return orderCancellationRequestService.createFailedOrderCancellationRequest(
                    order, userService.getCurrentUserName(),
                    "Fail to cancel order " + order.getNumber() +
                            " as there's already cancel request on this order"
            );
        }
        order.setCancelRequested(true);
        order.setCancelRequestedTime(ZonedDateTime.now());
        order.setCancelRequestedUsername(userService.getCurrentUserName());


        // sent notification for the order cancellation
        saveOrUpdate(order);

        return orderCancellationRequestService.createOrderCancellationRequest(
                order, userService.getCurrentUserName(),
                OrderCancellationRequestResult.REQUESTED,
                "order cancellation request for " + order.getNumber() +
                        " is sent, wait for the warehouse to cancel everything for the order"
        );
    }

    /**
     * Change the order's status to cancelled
     * @param order
     * @return
     */
    private OrderCancellationRequest markOrderCancelled(Order order) {

        // ok, the order is ready to be cancelled.
        // let's
        // 1. mark the order as cancel requested
        // 2. cancel all the shipments
        // 3. mark the order as cancelled
        order.setCancelRequested(true);
        order.setCancelRequestedTime(ZonedDateTime.now());
        order.setCancelRequestedUsername(userService.getCurrentUserName());

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

        order.setStatus(OrderStatus.CANCELLED);
        // clear all shipment line assignment
        order.getOrderLines().forEach(
                orderLine -> orderLine.setShipmentLines(new ArrayList<>())
        );


        order = saveOrUpdate(order, true);

        // send alert for order cancellation
        sendAlertForOrderCancellation(order, "");

        return orderCancellationRequestService.createOrderCancellationRequest(
                order, userService.getCurrentUserName(),
                OrderCancellationRequestResult.CANCELLED,
                order.getNumber() + " is cancelled"
        );
    }

    public Order clearOrderCancellationRequest(Long warehouseId, Long orderId, Long clientId, String clientName, String orderNumber) {

        Order order = findOrder(warehouseId, orderId, clientId, clientName, orderNumber);
        if (Objects.isNull(order)) {

            throw OrderOperationException.raiseException("Not able to find order by parameters. " +
                    (Objects.nonNull(orderId) ? "order id = " + orderId : "" ) +
                    (Objects.nonNull(clientId) ? "client id = " + clientId : "" ) +
                    (Strings.isNotBlank(clientName) ? "client name = " + clientName : "" ) +
                    (Strings.isNotBlank(orderNumber) ? "order number = " + orderNumber : "" )
            );
        }

        order.setCancelRequested(false);
        order.setCancelRequestedUsername(null);
        order.setCancelRequestedTime(null);

        return saveOrUpdate(order);


    }

    /**
     * Get quantity in open orders
     * @param warehouseId
     * @param clientId
     * @param itemId
     * @param inventoryStatusId
     * @param color
     * @param productSize
     * @param style
     * @param exactMatch
     * @return
     */
    public Long getQuantityInOrder(Long warehouseId, Long clientId, Long itemId, Long inventoryStatusId,
                                   String color, String productSize, String style, boolean exactMatch,
                                   ClientRestriction clientRestriction) {
        List<OrderLine> orderLines = orderLineService.findAll(
                warehouseId, clientId, null,
                null, null, itemId, inventoryStatusId, clientRestriction, false
        );
        orderLines = orderLines.stream()
                // filter out those order that are completed or cancelled
                .filter(orderLine -> !orderLine.getOrder().getStatus().equals(OrderStatus.COMPLETE) &&
                        !orderLine.getOrder().getStatus().equals(OrderStatus.CANCELLED))
                .filter(
                    orderLine -> matchOrderLineAttributeWithInventoryAttribute(orderLine.getColor(), color, exactMatch) &&
                            matchOrderLineAttributeWithInventoryAttribute(orderLine.getProductSize(), productSize, exactMatch) &&
                            matchOrderLineAttributeWithInventoryAttribute(orderLine.getStyle(), style, exactMatch)
        ).collect(Collectors.toList());

        return orderLines.stream().map(orderLine -> orderLine.getExpectedQuantity() > orderLine.getShippedQuantity() ?
                orderLine.getExpectedQuantity() - orderLine.getShippedQuantity() : 0l).mapToLong(Long::longValue).sum();
    }

    private boolean matchOrderLineAttributeWithInventoryAttribute(String orderLineAttribute,
                                                                  String inventoryAttribute,
                                                                  boolean exactMatch) {
        if (Strings.isBlank(orderLineAttribute) && Strings.isBlank(inventoryAttribute)) {
            return true;
        }
        if (Strings.isBlank(orderLineAttribute)) {
            // the order line doesn't have any requirement on the attribute but the inventory
            // has the attribute, return true if we are not looking for an exact match
            return !exactMatch;
        }
        if (Strings.isBlank(inventoryAttribute)) {
            // the order line has order line attribute setup but the inventory doesn't have the attribute
            // we know for sure the inventory is not for the order line
            return false;
        }
        // both the order line and the inventory has the attribute setup, let's return true if they
        // have the same value
        return orderLineAttribute.equalsIgnoreCase(inventoryAttribute);
    }

    public Integer getOpenOrderCount(Long warehouseId, ClientRestriction clientRestriction) {

        List<Order> orders = findAll(warehouseId,
                null, null, OrderStatus.OPEN.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false, clientRestriction);

        return orders.size();
    }

    public Integer getTodayOrderCount(Long warehouseId, ClientRestriction clientRestriction) {

        List<Order> orders = findAll(warehouseId,
                null, null, null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                false, clientRestriction);

        return orders.size();
    }
    public Integer getTodayCompletedOrderCount(Long warehouseId, ClientRestriction clientRestriction) {

        List<Order> orders = findAll(warehouseId,
                null, null, OrderStatus.COMPLETE.toString(),
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                false, clientRestriction);

        return orders.size();
    }

    /**
     * Generate walmart shipping carton labels
     * @param warehouseId
     * @param id
     * @param itemName
     * @return
     */
    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                             int copies,
                                                             String locale,
                                                             boolean nonAssignedOnly,
                                                             boolean nonPrintedOnly,
                                                             int labelCount)  {
        return generateWalmartShippingCartonLabels(warehouseId,
                findById(id), itemName, copies, locale, nonAssignedOnly, nonPrintedOnly, labelCount);
    }

    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                             int copies,
                                                             String locale)  {
        return generateWalmartShippingCartonLabels(warehouseId,
                findById(id), itemName, copies, locale, true, true, Integer.MAX_VALUE);
    }

    /**
     * Generate walmart shipping carton labels
     * @param warehouseId
     * @param itemName
     * @return
     */
    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId,Order order, String itemName,
                                                             int copies, String locale,
                                                             boolean nonAssignedOnly,
                                                             boolean nonPrintedOnly,
                                                             int labelCount)   {

        List<WalmartShippingCartonLabel> walmartShippingCartonLabels =
                walmartShippingCartonLabelService.findByPoNumberAndItem(
                        warehouseId, order.getNumber(), itemName,
                        nonAssignedOnly, nonPrintedOnly, labelCount);

        return generateWalmartShippingCartonLabels(warehouseId,
                copies, locale, walmartShippingCartonLabels);


    }

    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId,
                                                             int copies, String locale,
                                                             List<WalmartShippingCartonLabel> walmartShippingCartonLabels)   {

        Report reportData = new Report();


        setupWalmartShippingCartonLabelData(
                reportData, copies, walmartShippingCartonLabels );


        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("Will print {} labels", reportData.getData().size());
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.WALMART_SHIPPING_CARTON_LABEL, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());

        // we will need to update the labels' print date as well
        walmartShippingCartonLabels.forEach(
                walmartShippingCartonLabel -> {
                    logger.debug("start to set the print time for walmart shipping carton label with SSCC18: {}",
                            walmartShippingCartonLabel.getSSCC18());
                    walmartShippingCartonLabel.setLastPrintTime(ZonedDateTime.now(ZoneOffset.UTC));
                    walmartShippingCartonLabelService.saveOrUpdate(walmartShippingCartonLabel);
                }
        );
        return reportHistory;
    }



    /**
     * Generate walmart shipping label along with pallet label. It will return a list of report history with
     * one pallet label follow by shipping labels for cartons on this pallet
     * a second pallet follow by shipping labels for cartons on this pallet
     * a third pallet  follow by shipping labels for cartons on this pallet
     * until it complete all the pallets for the order
     * @param warehouseId
     * @param id  order id
     * @param copies
     * @param locale
     * @return
     */
    public List<ReportHistory> generateWalmartShippingCartonLabelsWithPalletLabels(Long warehouseId, Long id ,
                                                                                   int copies, String locale,
                                                                                   Boolean regeneratePalletLabels) {

        return generateWalmartShippingCartonLabelsWithPalletLabels(warehouseId,
                findById(id),   copies, locale, regeneratePalletLabels);
    }
    public List<ReportHistory> generateWalmartShippingCartonLabelsWithPalletLabels(Long warehouseId, Order order,
                                                                                   int copies, String locale,
                                                                                   Boolean regeneratePalletLabels) {

        logger.debug("Start to generate walmart shipping carton labels with pallet pick labels, for order {}",
                order.getNumber());
        // make sure we can start printing the shipping label with pallet labels for the order
        validateOrdersForWalmartShippingCartonLabelsWithPalletLabels(order);

        // if the user would like to regenerate the pallet pick labels, let's just refresh
        // the new pallet labels and assign the existing shipping carton label to this new
        // pallet label
        List<PalletPickLabelContent> palletPickLabelContents = new ArrayList<>();
        if (!Boolean.TRUE.equals(regeneratePalletLabels)) {

            // we will need to get the pallet information
            // it can be an estimation or an input from the user
            palletPickLabelContents =
                    palletPickLabelContentService.findAll(
                            warehouseId, order.getId(), order.getNumber());
        }

        if (palletPickLabelContents.isEmpty()) {
            // there's no estimation yet, let's create one
            logger.debug("There's no pallet pick label estimation for this order {} yet, " +
                    "let's create one",
                    order.getNumber());
            palletPickLabelContents = palletPickLabelContentService.generateAndSavePalletPickLabelEstimation(order);
        }
        if (palletPickLabelContents.isEmpty()) {
            throw OrderOperationException.raiseException("fail to generate pallet pick label for order " + order.getNumber());
        }
        int index = 0;
        logger.debug("===       start to print label for pallet pick label   =====");
        for (PalletPickLabelContent palletPickLabelContent : palletPickLabelContents) {
            index ++;
            logger.debug("pallet {}: height = {}, size = {}", index, palletPickLabelContent.getHeight(),
                    palletPickLabelContent.getVolume());
            for (PalletPickLabelPickDetail palletPickLabelPickDetail : palletPickLabelContent.getPalletPickLabelPickDetails()) {
                logger.debug(">> pick: {}, item = {}, quantity = {}, size = {} ",
                        palletPickLabelPickDetail.getPick().getNumber(),
                        Objects.nonNull(palletPickLabelPickDetail.getPick().getItem()) ?
                            palletPickLabelPickDetail.getPick().getItem().getName() :
                            palletPickLabelPickDetail.getPick().getItemId(),
                        palletPickLabelPickDetail.getPick().getQuantity(),
                        palletPickLabelPickDetail.getVolume());
            }
        }

        List<ReportHistory> result = new ArrayList<>();
        // see if we already have walmart shipping carton labels that attached to this pallet

        // show 1 / 2, 2 / 2 on the pallet pick label so the user knows how many
        // pallet labels being printed for this order
        int labelIndex = 0;

        for (PalletPickLabelContent palletPickLabelContent : palletPickLabelContents) {

            labelIndex++;
            List<WalmartShippingCartonLabel> walmartShippingCartonLabels =
                    walmartShippingCartonLabelService.findByPalletPickLabel(
                            palletPickLabelContent
                    );
            logger.debug("We got {} shipping carton labels that already assigned to current pallet pick label",
                    walmartShippingCartonLabels.size());
            if (walmartShippingCartonLabels.isEmpty()) {
                // ok, we haven't assign any walmart shipping carton label to this
                // pallet pick label yet, let's assign now
                logger.debug("Since there's no shipping carton label assigned, yet, let's start to assign process" +
                        " and find some available shipping carton labels for this pallet");
                walmartShippingCartonLabels =
                        walmartShippingCartonLabelService.assignShippingCartonLabel(
                                order, palletPickLabelContent
                        );
                logger.debug("We got {} AVAILABLE shipping carton labels and assigned to the current pallet pick label {}",
                        walmartShippingCartonLabels.size(),
                        palletPickLabelContent.getNumber());
            }
            result.addAll(
                    generateWalmartShippingCartonLabelsWithPalletLabel(
                            warehouseId, palletPickLabelContent, walmartShippingCartonLabels,
                            copies, locale, labelIndex, palletPickLabelContents.size())
            );

        }
        logger.debug("we get {} labels to be printed",
                result.size());
        int i = 1;
        for (ReportHistory reportHistory : result) {
            logger.debug("==========  Label  " + i++ + ", type: " + reportHistory.getType() + "      ========");

        }
        return result;


    }

    /**
     * Generate one pallet labels and all the walmart shipping carton labels for the cartons on this pallet
     * @param palletPickLabelContent
     * @param walmartShippingCartonLabels
     * @return
     */
    private List<ReportHistory> generateWalmartShippingCartonLabelsWithPalletLabel(
            Long warehouseId,
            PalletPickLabelContent palletPickLabelContent,
            List<WalmartShippingCartonLabel> walmartShippingCartonLabels,
            int copies, String locale,
            int index,
            int totalLabelCount) {
        List<ReportHistory> result = new ArrayList<>();
        // we will generate the pallet pick label first

        // note, if we will need multiple copies, then we will print one copy
        // of pallet pick label following by one copy of shipping carton label
        // then start a new copy of pallet pick label and so on
        for (int i = 0; i < copies; i++) {

            // generate the pallet pick label
            // note: if the pallet contains multiple picks(as of now, more than 6)
            // we may need to print multiple labels for the same pallet, each one with
            // same header and footer but different contents of picks
            result.addAll(
                    palletPickLabelContentService.generatePalletPickLabel(warehouseId,
                            1, locale,
                            palletPickLabelContent,  index, totalLabelCount));

            // let's generate the shipping labels
            result.add(
                    generateWalmartShippingCartonLabels(warehouseId,
                            1, locale, walmartShippingCartonLabels));

        }
        return result;


    }

    private void validateOrdersForWalmartShippingCartonLabelsWithPalletLabels(Order order) {
        logger.debug("validate order {} to see if it is a valid order for walmart shipping carton label" +
                " and pallet pick label", order.getNumber());
        if (Objects.nonNull(order.getShipToCustomer())) {
            Customer customer = order.getShipToCustomer();
            if(!Boolean.TRUE.equals(customer.getCustomerIsWalmart())) {
                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is not walmart, " +
                        " can't print walmart shipping carton for it");
            }
            if (!Boolean.TRUE.equals(customer.getAllowPrintShippingCartonLabelWithPalletLabel())) {

                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is configured not to" +
                        " print walmart shipping label with pallet label");
            }
            if (!Boolean.TRUE.equals(customer.getAllowPrintShippingCartonLabelWithPalletLabelWhenShort()) &&
                  !isOrderFullyAllocated(order)) {

                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is configured not to" +
                        " print walmart shipping label with pallet label while the order is short allocated " +
                        ", but the order is not fully allocated");
            }
        }

        logger.debug(" order {} passed the validation. We can print walmart shipping carton label" +
                " and pallet pick label", order.getNumber());
    }

    private void setupWalmartShippingCartonLabelData(Report reportData,
                                                     int copies,
                                                     List<WalmartShippingCartonLabel> walmartShippingCartonLabels) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();

        walmartShippingCartonLabels.forEach(
                walmartShippingCartonLabel -> {
                        for (int i = 0; i < copies; i++) {

                            lpnLabelContents.add(getWalmartShippingCartonLabelContent(
                                    walmartShippingCartonLabel
                            ));
                        }

                }
        );
        reportData.setData(lpnLabelContents);
    }

    private Map<String, Object> getWalmartShippingCartonLabelContent(WalmartShippingCartonLabel walmartShippingCartonLabel) {
        Map<String, Object> lpnLabelContent = new HashMap<>();

        lpnLabelContent.put("address1", walmartShippingCartonLabel.getAddress1());
        lpnLabelContent.put("BOL", walmartShippingCartonLabel.getBOL());
        lpnLabelContent.put("carrierNumber", walmartShippingCartonLabel.getCarrierNumber());
        lpnLabelContent.put("cityStateZip", walmartShippingCartonLabel.getCityStateZip());
        lpnLabelContent.put("DC", walmartShippingCartonLabel.getDC());
        lpnLabelContent.put("dept", walmartShippingCartonLabel.getDept());
        lpnLabelContent.put("poNumber", walmartShippingCartonLabel.getPoNumber());
        lpnLabelContent.put("shipTo", walmartShippingCartonLabel.getShipTo());
        lpnLabelContent.put("SSCC18", walmartShippingCartonLabel.getSSCC18());
        lpnLabelContent.put("type", walmartShippingCartonLabel.getType());
        lpnLabelContent.put("WMIT", walmartShippingCartonLabel.getWMIT());

        return lpnLabelContent;
    }

    public List<WalmartShippingCartonLabel> getWalmartShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                                           boolean nonAssignedOnly, boolean nonPrintedOnly) {
        Order order = findById(id);
        // for walmart shipping carton label, we can only find by PO number
        if (Strings.isBlank(order.getPoNumber())) {
            logger.debug("Order {} doesn't have a PO number, return nothing",
                    order.getNumber());
            return new ArrayList<>();
        }
        if (Strings.isNotBlank(itemName)) {

            logger.debug("start to find walmart shipping carton labels for order {} with PO number {}, item name: {}",
                    order.getNumber(), order.getPoNumber(), itemName);
            return walmartShippingCartonLabelService.findByPoNumberAndItem(warehouseId, order.getPoNumber(),
                    itemName, nonAssignedOnly, nonPrintedOnly);
        }
        else {
            logger.debug("start to find walmart shipping carton labels for order {} with PO number {}, WITHOUT item",
                    order.getNumber(), order.getPoNumber());
            return walmartShippingCartonLabelService.findAll(
                    warehouseId, null, null,
                    order.getPoNumber(), null,null,
                    itemName, null, nonPrintedOnly, nonAssignedOnly,
                    null
            );
        }
    }

    /**
     * Check if the order is fully allocated
     * @param order
     * @return
     */
    private boolean isOrderFullyAllocated(Order order) {
        // get all the picks and sum up the pick quantity to match with the order line's required quantity
        // and see if the order is fully allocated
        List<Pick> picks = pickService.findByOrder(order);
        // Map to save the required quantity and allocated quantity
        // key: item id
        // value: quantity
        Map<Long, Long> requiredQuantitiesMap = new HashMap<>();
        Map<Long, Long> allocatedQuantitiesMap = new HashMap<>();
        order.getOrderLines().forEach(
                orderLine -> {
                    Long quantity = requiredQuantitiesMap.getOrDefault(orderLine.getItemId(), 0l);
                    requiredQuantitiesMap.put(orderLine.getItemId(), quantity + orderLine.getExpectedQuantity());
                }
        );

        picks.forEach(
                pick -> {
                    Long quantity = allocatedQuantitiesMap.getOrDefault(pick.getItemId(), 0l);
                    allocatedQuantitiesMap.put(pick.getItemId(), quantity + pick.getQuantity());
                }
        );

        // loop through each order line to make sure there's enough allocated quantity for each item
        return requiredQuantitiesMap.entrySet().stream().noneMatch(
                entry -> {
                    Long itemId = entry.getKey();
                    Long requiredQuantity = entry.getValue();
                    Long allocatedQuantity = allocatedQuantitiesMap.getOrDefault(itemId, 0l);
                    if (allocatedQuantity < requiredQuantity) {
                        logger.debug("order {} with item id {} is short allocated, required quantity: {}, allocated quantity: {}",
                                order.getNumber(),
                                itemId,
                                requiredQuantity, allocatedQuantity);
                        return true;
                    }
                    // return false if the item is fully allocated
                    return false;
                }
        );
    }

    public ReportHistory generateCombinedTargetShippingCartonLabelsWithPalletLabels(Long warehouseId, Long id ,
                                                                                  int copies, String locale,
                                                                                  Boolean regeneratePalletLabels) {

        List<ReportHistory> reportHistories =
                generateTargetShippingCartonLabelsWithPalletLabels(
                        warehouseId,
                        id, copies, locale,
                        regeneratePalletLabels);
        // let's combine the labels into one label
        // to make sure the labels will be printed all together in the right sequence
        // otherwise, even we can make sure the client will send the print request to the printer
        // in the right sequence, there's no grantee that the printer will process the request
        // in the same sequence
        logger.debug("get {} report history and we will try to combine into one file",
                reportHistories.size());
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        return resourceServiceRestemplateClient.combineLabels(
                warehouse.getCompanyId(),
                warehouseId,
                reportHistories
        );

    }

    public List<ReportHistory> generateTargetShippingCartonLabelsWithPalletLabels(Long warehouseId, Long id ,
                                                                                  int copies, String locale,
                                                                                  Boolean regeneratePalletLabels) {

        return generateTargetShippingCartonLabelsWithPalletLabels(warehouseId,
                findById(id),   copies, locale, regeneratePalletLabels);
    }

    public List<ReportHistory> generateTargetShippingCartonLabelsWithPalletLabels(Long warehouseId, Order order,
                                                                                   int copies, String locale,
                                                                                   Boolean regeneratePalletLabels) {

        logger.debug("Start to generate target shipping carton labels with pallet pick labels, for order {}",
                order.getNumber());
        // make sure we can start printing the shipping label with pallet labels for the order
        validateOrdersForTargetShippingCartonLabelsWithPalletLabels(order);

        // if the user would like to regenerate the pallet pick labels, let's just refresh
        // the new pallet labels and assign the existing shipping carton label to this new
        // pallet label
        List<PalletPickLabelContent> palletPickLabelContents = new ArrayList<>();
        if (!Boolean.TRUE.equals(regeneratePalletLabels)) {

            // we will need to get the pallet information
            // it can be an estimation or an input from the user
            palletPickLabelContents =
                    palletPickLabelContentService.findAll(
                            warehouseId, order.getId(), order.getNumber());
        }

        if (palletPickLabelContents.isEmpty()) {
            // there's no estimation yet, let's create one
            logger.debug("There's no pallet pick label estimation for this order {} yet, " +
                            "let's create one",
                    order.getNumber());
            palletPickLabelContents = palletPickLabelContentService.generateAndSavePalletPickLabelEstimation(order);
        }
        if (palletPickLabelContents.isEmpty()) {
            throw OrderOperationException.raiseException("fail to generate pallet pick label for order " + order.getNumber());
        }
        String batchNumber = palletPickLabelContents.get(0).getBatchNumber();

        int index = 0;
        logger.debug("===       start to print label for pallet pick label   =====");
        for (PalletPickLabelContent palletPickLabelContent : palletPickLabelContents) {
            index ++;
            logger.debug("pallet {}: height = {}, size = {}", index, palletPickLabelContent.getHeight(),
                    palletPickLabelContent.getVolume());
            for (PalletPickLabelPickDetail palletPickLabelPickDetail : palletPickLabelContent.getPalletPickLabelPickDetails()) {
                logger.debug(">> pick: {}, item = {}, quantity = {}, size = {} ",
                        palletPickLabelPickDetail.getPick().getNumber(),
                        Objects.nonNull(palletPickLabelPickDetail.getPick().getItem()) ?
                                palletPickLabelPickDetail.getPick().getItem().getName() :
                                palletPickLabelPickDetail.getPick().getItemId(),
                        palletPickLabelPickDetail.getPick().getQuantity(),
                        palletPickLabelPickDetail.getVolume());
            }
        }

        List<ReportHistory> result = new ArrayList<>();
        // see if we already have walmart shipping carton labels that attached to this pallet

        // start with a summary label(or lots of summary label since each label can only display
        // 6 items due to the size limit),
        //
        result.addAll(
                palletPickLabelContentService.generatePalletPickSummaryLabel(
                        warehouseId, copies, locale,
                        batchNumber, order.getPoNumber(),
                        palletPickLabelContents)
        );
        // show 1 / 2, 2 / 2 on the pallet pick label so the user knows how many
        // pallet labels being printed for this order
        int labelIndex = 0;

        for (PalletPickLabelContent palletPickLabelContent : palletPickLabelContents) {

            labelIndex++;
            List<TargetShippingCartonLabel> targetShippingCartonLabels =
                    targetShippingCartonLabelService.findByPalletPickLabel(
                            palletPickLabelContent
                    );
            logger.debug("We got {} shipping carton labels that already assigned to current pallet pick label",
                    targetShippingCartonLabels.size());
            if (targetShippingCartonLabels.isEmpty()) {
                // ok, we haven't assign any target shipping carton label to this
                // pallet pick label yet, let's assign now
                logger.debug("Since there's no shipping carton label assigned, yet, let's start to assign process" +
                        " and find some available shipping carton labels for this pallet");
                targetShippingCartonLabels =
                        targetShippingCartonLabelService.assignShippingCartonLabel(
                                order, palletPickLabelContent
                        );
                logger.debug("We got {} AVAILABLE shipping carton labels and assigned to the current pallet pick label {}",
                        targetShippingCartonLabels.size(),
                        palletPickLabelContent.getNumber());
            }
            result.addAll(
                    generateTargetShippingCartonLabelsWithPalletLabel(
                            warehouseId, palletPickLabelContent, targetShippingCartonLabels,
                            copies, locale, labelIndex, palletPickLabelContents.size())
            );

        }
        logger.debug("we get {} labels to be printed",
                result.size());
        int i = 1;
        for (ReportHistory reportHistory : result) {
            logger.debug("==========  Label  " + i++ + ", type: " + reportHistory.getType() + "      ========");

        }
        return result;


    }

    private void validateOrdersForTargetShippingCartonLabelsWithPalletLabels(Order order) {
        logger.debug("validate order {} to see if it is a valid order for target shipping carton label" +
                " and pallet pick label", order.getNumber());
        if (Objects.nonNull(order.getShipToCustomer())) {
            Customer customer = order.getShipToCustomer();
            if(!Boolean.TRUE.equals(customer.getCustomerIsTarget())) {
                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is not target, " +
                        " can't print target shipping carton for it");
            }
            if (!Boolean.TRUE.equals(customer.getAllowPrintShippingCartonLabelWithPalletLabel())) {

                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is configured not to" +
                        " print shipping label with pallet label");
            }
            if (!Boolean.TRUE.equals(customer.getAllowPrintShippingCartonLabelWithPalletLabelWhenShort()) &&
                    !isOrderFullyAllocated(order)) {

                throw OrderOperationException.raiseException("order " + order.getNumber()
                        + "'s ship to customer " + customer.getName() + " is configured not to" +
                        " print shipping label with pallet label while the order is short allocated " +
                        ", but the order is not fully allocated");
            }
        }

        logger.debug(" order {} passed the validation. We can print target shipping carton label" +
                " and pallet pick label", order.getNumber());
    }

    /**
     * Generate one pallet labels and all the walmart shipping carton labels for the cartons on this pallet
     * @param palletPickLabelContent
     * @return
     */
    private List<ReportHistory> generateTargetShippingCartonLabelsWithPalletLabel(
            Long warehouseId,
            PalletPickLabelContent palletPickLabelContent,
            List<TargetShippingCartonLabel> targetShippingCartonLabels,
            int copies, String locale,
            int index,
            int totalLabelCount) {
        List<ReportHistory> result = new ArrayList<>();
        // we will generate the pallet pick label first

        // note, if we will need multiple copies, then we will print one copy
        // of pallet pick label following by one copy of shipping carton label
        // then start a new copy of pallet pick label and so on
        for (int i = 0; i < copies; i++) {

            // generate the pallet pick label
            // note: if the pallet contains multiple picks(as of now, more than 6)
            // we may need to print multiple labels for the same pallet, each one with
            // same header and footer but different contents of picks
            result.addAll(
                    palletPickLabelContentService.generatePalletPickLabel(warehouseId,
                            1, locale,
                            palletPickLabelContent,  index, totalLabelCount));

            // let's generate the shipping labels
            result.add(
                    generateTargetShippingCartonLabels(warehouseId,
                            1, locale, targetShippingCartonLabels));

        }
        return result;

    }


    /**
     * Generate target shipping carton labels
     * @param warehouseId
     * @param id
     * @param itemName
     * @return
     */
    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                             int copies,
                                                             String locale,
                                                             boolean nonAssignedOnly,
                                                             boolean nonPrintedOnly,
                                                             int labelCount)  {
        return generateTargetShippingCartonLabels(warehouseId,
                findById(id), itemName, copies, locale, nonAssignedOnly, nonPrintedOnly, labelCount);
    }

    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                             int copies,
                                                             String locale)  {
        return generateTargetShippingCartonLabels(warehouseId,
                findById(id), itemName, copies, locale, true, true, Integer.MAX_VALUE);
    }

    /**
     * Generate target shipping carton labels
     * @param warehouseId
     * @param itemName
     * @return
     */
    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId,Order order, String itemName,
                                                             int copies, String locale,
                                                             boolean nonAssignedOnly,
                                                             boolean nonPrintedOnly,
                                                             int labelCount)   {

        List<TargetShippingCartonLabel> targetShippingCartonLabels =
                targetShippingCartonLabelService.findByPoNumberAndItem(
                        warehouseId, order.getNumber(), itemName,
                        nonAssignedOnly, nonPrintedOnly, labelCount);

        return generateTargetShippingCartonLabels(warehouseId,
                copies, locale, targetShippingCartonLabels);


    }

    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId,
                                                             int copies, String locale,
                                                             List<TargetShippingCartonLabel> targetShippingCartonLabels)   {

        Report reportData = new Report();


        setupTargetShippingCartonLabelData(
                reportData, copies, targetShippingCartonLabels );


        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("Will print {} labels", reportData.getData().size());
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.TARGET_SHIPPING_CARTON_LABEL, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());

        // we will need to update the labels' print date as well
        targetShippingCartonLabels.forEach(
                targetShippingCartonLabel -> {
                    logger.debug("start to set the print time for target shipping carton label with SSCC18: {}",
                            targetShippingCartonLabel.getSSCC18());
                    targetShippingCartonLabel.setLastPrintTime(ZonedDateTime.now(ZoneOffset.UTC));
                    targetShippingCartonLabelService.saveOrUpdate(targetShippingCartonLabel);
                }
        );
        return reportHistory;
    }

    private void setupTargetShippingCartonLabelData(Report reportData,
                                                     int copies,
                                                     List<TargetShippingCartonLabel> targetShippingCartonLabels) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();

        targetShippingCartonLabels.forEach(
                targetShippingCartonLabel -> {
                    for (int i = 0; i < copies; i++) {

                        lpnLabelContents.add(getTargetShippingCartonLabelContent(
                                targetShippingCartonLabel
                        ));
                    }

                }
        );
        reportData.setData(lpnLabelContents);
    }

    private Map<String, Object> getTargetShippingCartonLabelContent(TargetShippingCartonLabel targetShippingCartonLabel) {
        return targetShippingCartonLabelService.getTargetShippingCartonLabelContent(targetShippingCartonLabel);
    }

    public List<TargetShippingCartonLabel> getTargetShippingCartonLabels(Long warehouseId, Long id, String itemName,
                                                                           boolean nonAssignedOnly, boolean nonPrintedOnly) {
        Order order = findById(id);
        // for walmart shipping carton label, we can only find by PO number
        if (Strings.isBlank(order.getPoNumber())) {
            logger.debug("Order {} doesn't have a PO number, return nothing",
                    order.getNumber());
            return new ArrayList<>();
        }
        if (Strings.isNotBlank(itemName)) {

            logger.debug("start to find target shipping carton labels for order {} with PO number {}, item name: {}",
                    order.getNumber(), order.getPoNumber(), itemName);
            return targetShippingCartonLabelService.findByPoNumberAndItem(warehouseId, order.getPoNumber(),
                    itemName, nonAssignedOnly, nonPrintedOnly);
        }
        else {
            logger.debug("start to find target shipping carton labels for order {} with PO number {}, WITHOUT item",
                    order.getNumber(), order.getPoNumber());
            return targetShippingCartonLabelService.findAll(
                    warehouseId, null, null,
                    order.getPoNumber(),
                    itemName, null, nonPrintedOnly, nonAssignedOnly,
                    null
            );
        }
    }

    /**
     * when the shipment line is cancelled, see if all shipment line has been cancelled for the order,
     * if so, set the order back to PENDING
     * @param order
     */
    public void registerShipmentLineCancelled(Order order) {
        if (order.getStatus().equals(OrderStatus.INPROCESS)) {

            logger.debug("order {}'s status is in process, let's see if we will need to set it back to OPEN",
                    order.getNumber());
            boolean setOrderToOpen =
                    order.getOrderLines().stream().noneMatch(
                            orderLine -> orderLine.getInprocessQuantity() > 0
                    );

            if (setOrderToOpen) {
                logger.debug("we will set order {}'s status back to OPEN", order.getNumber());
                order.setStatus(OrderStatus.OPEN);
                saveOrUpdate(order, false);
            }
        }

    }
}

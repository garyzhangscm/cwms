package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderLineRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DBBasedOrderIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedOrderIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedOrderRepository dbBasedOrderRepository;
    @Autowired
    DBBasedOrderLineRepository dbBasedOrderLineRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public List<DBBasedOrder> findAll(String companyCode,
                                      Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                      String statusList, Long id) {

        return dbBasedOrderRepository.findAll(
                (Root<DBBasedOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyCode"), companyCode));

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }


                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"),
                                dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }
                    if (Strings.isNotBlank(statusList)) {
                        CriteriaBuilder.In<IntegrationStatus> inStatus = criteriaBuilder.in(root.get("status"));
                        for(String status : statusList.split(",")) {
                            inStatus.value(IntegrationStatus.valueOf(status));
                        }
                        predicates.add(criteriaBuilder.and(inStatus));
                    }

                    if (Objects.nonNull(id)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("id"), id));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public DBBasedOrder findById(Long id) {
        return dbBasedOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order data not found by id: " + id));
    }


    public IntegrationOrderData addIntegrationOrderData(DBBasedOrder dbBasedOrder) {
        dbBasedOrder.setStatus(IntegrationStatus.PENDING);
        dbBasedOrder.getOrderLines().forEach(
                dbBasedOrderLine -> {
                    dbBasedOrderLine.setOrder(dbBasedOrder);
                    dbBasedOrderLine.setStatus(IntegrationStatus.ATTACHED);
                }
        );
        return dbBasedOrderRepository.save(dbBasedOrder);
    }

    private List<DBBasedOrder> findPendingIntegration() {
        return dbBasedOrderRepository.findAll(
                (Root<DBBasedOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedOrder save(DBBasedOrder dbBasedOrder) {
        return dbBasedOrderRepository.save(dbBasedOrder);
    }

    public void listen() {
        logger.debug("Start to process Order data");
        List<DBBasedOrder> dbBasedOrders = findPendingIntegration();
        logger.debug(">> get {} Order data to be processed", dbBasedOrders.size());
        dbBasedOrders.forEach(dbBasedOrder -> process(dbBasedOrder));
    }

    private void process(DBBasedOrder dbBasedOrder) {

        try {


            Order order = dbBasedOrder.convertToOrder(warehouseLayoutServiceRestemplateClient,
                    inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient);

            // we will support client to send name
            // instead of id for the following field . In such case, we will
            // set to setup the IDs from the name so that
            // the correpondent service can handle it
            // 1. warehouse
            // 2. customer: bill to customer and ship to customer
            // 3. carrier
            // 4. carrier service level
            // 5. client
            setupMissingField(order, dbBasedOrder);


            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Order:\n{}", order);

            kafkaSender.send(IntegrationType.INTEGRATION_ORDER,
                    order.getWarehouseId() + "-" + dbBasedOrder.getId(), order);

            dbBasedOrder.setStatus(IntegrationStatus.SENT);
            dbBasedOrder.setErrorMessage("");

            dbBasedOrder = save(dbBasedOrder);

            // Save the order line as well
            dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine ->{
                dbBasedOrderLine.setStatus(IntegrationStatus.SENT);
                dbBasedOrderLine.setErrorMessage("");

                dbBasedOrderLineRepository.save(dbBasedOrderLine);
            });

            logger.debug(">> Order data process, {}", dbBasedOrder.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedOrder.setStatus(IntegrationStatus.ERROR);
            dbBasedOrder.setErrorMessage(ex.getMessage());

            dbBasedOrder = save(dbBasedOrder);

            // Save the order line as well
            dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine ->{
                dbBasedOrderLine.setStatus(IntegrationStatus.ERROR);
                dbBasedOrderLine.setErrorMessage(ex.getMessage());

                dbBasedOrderLineRepository.save(dbBasedOrderLine);
            });
        }
    }


    private void setupMissingField(Order order, DBBasedOrder dbBasedOrder) throws UnsupportedEncodingException {

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(order.getWarehouseId());

        if (Objects.isNull(order.getShipToCustomerId()) &&
            Strings.isNotBlank(dbBasedOrder.getShipToCustomerName())) {
            Customer customer = commonServiceRestemplateClient.getCustomerByName(
                    warehouse.getCompany().getId(),
                    warehouse.getId(),
                    dbBasedOrder.getShipToCustomerName()
            );
            if (Objects.nonNull(customer)) {
                order.setShipToCustomerId(customer.getId());
                // setup the bill to address as well if it is the same as the ship to address
                if (dbBasedOrder.getShipToCustomerName().equals(
                        dbBasedOrder.getBillToCustomerName()
                )) {
                    order.setBillToCustomerId(customer.getId());
                }
            }
        }

        if (Objects.isNull(order.getBillToCustomerId()) &&
                Strings.isNotBlank(dbBasedOrder.getBillToCustomerName())) {
            Customer customer = commonServiceRestemplateClient.getCustomerByName(
                    warehouse.getCompany().getId(),
                    warehouse.getId(),
                    dbBasedOrder.getBillToCustomerName()
            );
            if (Objects.nonNull(customer)) {
                order.setBillToCustomerId(customer.getId());
            }
        }
        if (Strings.isBlank(dbBasedOrder.getCategory())) {
            // default the category to SALES ORDER
            order.setCategory(OrderCategory.SALES_ORDER.name());
        }

        if (Objects.isNull(order.getClientId()) &&
                Strings.isNotBlank(dbBasedOrder.getClientName())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(), dbBasedOrder.getClientName()
            );
            if (Objects.nonNull(client)) {
                logger.debug("get client id {} from name {}",
                        client.getId(), dbBasedOrder.getClientName());
                order.setClientId(client.getId());

            }
        }
        for(OrderLine orderLine : order.getOrderLines()) {
            // Get the matched order line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            // 3. inventory status ID
            // 4. carrier ID
            // 5. carrier service level id
            for(DBBasedOrderLine dbBasedOrderLine: dbBasedOrder.getOrderLines()) {
                if (orderLine.getNumber().equals(dbBasedOrderLine.getNumber())) {
                    setupMissingField(warehouse, orderLine, dbBasedOrderLine);
                }
            }

        }

    }

    /**
     * Setup the missing field. When we read from the database, we allow the host
     * to pass in name instead of id for the following feilds. We will need to
     * translate to id so that the correspondent service can recognize it     *
     * 1. item Id
     * 2. warehouse Id
     * 3. inventory status ID
     * 4. carrier ID
     * 5. carrier service level id
     * @param warehouse      Warehouse
     * @param orderLine
     * @param dbBasedOrderLine
     */
    private void setupMissingField(Warehouse warehouse, OrderLine orderLine, DBBasedOrderLine dbBasedOrderLine) throws UnsupportedEncodingException {

        // 1. item Id
        if (Objects.isNull(orderLine.getItemId())) {
            Item item = null;
            logger.debug("item id is not passed in for line {}, let's set it up",
                    orderLine.getNumber());

            logger.debug("item name: {}", dbBasedOrderLine.getItemName());
            logger.debug("item quickbook list id: {}", dbBasedOrderLine.getItemQuickbookListId());

            if (Strings.isNotBlank(dbBasedOrderLine.getItemName())) {
                item = inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getCompany().getId(),
                        orderLine.getWarehouseId(),
                        dbBasedOrderLine.getOrder().getCarrierId(),
                        dbBasedOrderLine.getItemName()
                );
            }
            else if (Strings.isNotBlank(dbBasedOrderLine.getItemQuickbookListId())) {
                item = inventoryServiceRestemplateClient.getItemByQuickbookListId(
                        warehouse.getCompany().getId(),
                        orderLine.getWarehouseId(), dbBasedOrderLine.getItemQuickbookListId()
                );
            }
            else {
                throw MissingInformationException.raiseException("Either item id, or item name, or quickbook item list id " +
                        " needs to be present in order to identify the item for this order line");
            }
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item based on the order line's information");
            }
            orderLine.setItemId(item.getId());
        }



        // 2. warehouse Id

        if(Objects.isNull(orderLine.getItemId())) {
            orderLine.setWarehouseId(warehouse.getId());
        }

        // 3. inventory status ID
        if(Objects.isNull(orderLine.getInventoryStatusId())) {
            if (Strings.isBlank(dbBasedOrderLine.getInventoryStatusName())) {
                // if both inventory status id and name is null, then get the available inventory status
                InventoryStatus inventoryStatus = inventoryServiceRestemplateClient.getAvailableInventoryStatus(warehouse.getId());
                if(Objects.nonNull(inventoryStatus)) {
                    orderLine.setInventoryStatusId(inventoryStatus.getId());
                }
                else {
                    throw ResourceNotFoundException.raiseException("No inventory status is specified and can't find the default" +
                            " inventory status for Available inventory");
                }
            }
            else {

                orderLine.setInventoryStatusId(
                        inventoryServiceRestemplateClient.getInventoryStatusByName(
                                warehouse.getId(), dbBasedOrderLine.getInventoryStatusName()
                        ).getId()
                );
            }
        }


        // 4. carrier ID
        // Carrier is optional and may be different from the order's carrier info
        Carrier carrier = null;
        if(Objects.isNull(orderLine.getCarrierId()) &&
                Strings.isNotBlank(dbBasedOrderLine.getCarrierName())) {
            carrier = commonServiceRestemplateClient.getCarrierByName(
                    warehouse.getId(),
                    dbBasedOrderLine.getCarrierName());
            orderLine.setCarrierId(carrier.getId());
        }

        // 5. carrier service level id
        // Carrier service level is optional and may be different from the order's carrier info
        if(Objects.isNull(orderLine.getCarrierServiceLevelId()) &&
                Strings.isNotBlank(dbBasedOrderLine.getCarrierServiceLevelName())) {
            if (Objects.isNull(carrier) ) {
                // If carrier is not setup yet by the above step, let's try
                // to get by carrier id
                carrier = commonServiceRestemplateClient.getCarrierById(
                        orderLine.getCarrierId()
                );
                // Get all the available carrier service level
                carrier.getCarrierServiceLevels().forEach(carrierServiceLevel -> {
                    if (carrierServiceLevel.getName().equals(dbBasedOrderLine.getCarrierServiceLevelName())) {
                        orderLine.setCarrierServiceLevelId(carrierServiceLevel.getId());
                    }
                });
            }
        }

        if (Objects.isNull(orderLine.getAutoRequestShippingLabel())) {
            orderLine.setAutoRequestShippingLabel(false);
        }
    }


    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the customer integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedOrder dbBasedOrder = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedOrder.setStatus(integrationStatus);
        dbBasedOrder.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedOrder);

        dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine ->{
            dbBasedOrderLine.setStatus(integrationStatus);
            dbBasedOrderLine.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedOrderLineRepository.save(dbBasedOrderLine);
        });



    }

    public IntegrationOrderData resendOrderData(Long id) {
        DBBasedOrder dbBasedOrder =
                findById(id);
        dbBasedOrder.setStatus(IntegrationStatus.PENDING);
        dbBasedOrder.setErrorMessage("");
        return save(dbBasedOrder);
    }

}

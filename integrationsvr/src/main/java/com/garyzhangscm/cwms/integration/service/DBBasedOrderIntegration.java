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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    public List<DBBasedOrder> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {

        return dbBasedOrderRepository.findAll(
                (Root<DBBasedOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("insertTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("insertTime"), endTime));

                    }
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("insertTime"), dateStartTime, dateEndTime));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public IntegrationOrderData findById(Long id) {
        return dbBasedOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order data not found by id: " + id));
    }


    public IntegrationOrderData addIntegrationOrderData(DBBasedOrder dbBasedOrder) {
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


            Order order = dbBasedOrder.convertToOrder(warehouseLayoutServiceRestemplateClient, inventoryServiceRestemplateClient);

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

            kafkaSender.send(IntegrationType.INTEGRATION_ORDER, order);

            dbBasedOrder.setStatus(IntegrationStatus.COMPLETED);
            dbBasedOrder.setErrorMessage("");
            dbBasedOrder.setLastUpdateTime(LocalDateTime.now());
            dbBasedOrder = save(dbBasedOrder);

            // Save the order line as well
            dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine ->{
                dbBasedOrderLine.setStatus(IntegrationStatus.COMPLETED);
                dbBasedOrderLine.setErrorMessage("");
                dbBasedOrderLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedOrderLineRepository.save(dbBasedOrderLine);
            });

            logger.debug(">> Order data process, {}", dbBasedOrder.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedOrder.setStatus(IntegrationStatus.ERROR);
            dbBasedOrder.setErrorMessage(ex.getMessage());
            dbBasedOrder.setLastUpdateTime(LocalDateTime.now());
            dbBasedOrder = save(dbBasedOrder);

            // Save the order line as well
            dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine ->{
                dbBasedOrderLine.setStatus(IntegrationStatus.ERROR);
                dbBasedOrderLine.setErrorMessage(ex.getMessage());
                dbBasedOrderLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedOrderLineRepository.save(dbBasedOrderLine);
            });
        }
    }


    private void setupMissingField(Order order, DBBasedOrder dbBasedOrder){

        order.getOrderLines().forEach(orderLine -> {
            // Get the matched order line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            // 3. inventory status ID
            // 4. carrier ID
            // 5. carrier service level id
            dbBasedOrder.getOrderLines().forEach(dbBasedOrderLine -> {
                if (orderLine.getNumber().equals(dbBasedOrderLine.getNumber())) {
                    setupMissingField(order.getWarehouseId(), orderLine, dbBasedOrderLine);
                }
            });

        });

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
     * @param warehouseId      Warehouse id
     * @param orderLine
     * @param dbBasedOrderLine
     */
    private void setupMissingField(Long warehouseId, OrderLine orderLine, DBBasedOrderLine dbBasedOrderLine){

        // 1. item Id
        if(Objects.isNull(orderLine.getItemId())) {
                orderLine.setItemId(
                        inventoryServiceRestemplateClient.getItemByName(
                                warehouseId, dbBasedOrderLine.getItemName()
                        ).getId()
                );
        }


        // 2. warehouse Id

        if(Objects.isNull(orderLine.getItemId())) {
            orderLine.setWarehouseId(warehouseId);
        }

        // 3. inventory status ID
        if(Objects.isNull(orderLine.getInventoryStatusId())) {
            orderLine.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, dbBasedOrderLine.getInventoryStatusName()
                    ).getId()
            );
        }


        // 4. carrier ID
        // Carrier is optional and may be different from the order's carrier info
        Carrier carrier = null;
        if(Objects.isNull(orderLine.getCarrierId()) &&
                Objects.nonNull(dbBasedOrderLine.getCarrierName())) {
            carrier = commonServiceRestemplateClient.getCarrierByName(
                    dbBasedOrderLine.getCarrierName());
            orderLine.setCarrierId(carrier.getId());
        }

        // 5. carrier service level id
        // Carrier service level is optional and may be different from the order's carrier info
        if(Objects.isNull(orderLine.getCarrierServiceLevelId()) &&
                Objects.nonNull(dbBasedOrderLine.getCarrierServiceLevelName())) {
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
    }



}

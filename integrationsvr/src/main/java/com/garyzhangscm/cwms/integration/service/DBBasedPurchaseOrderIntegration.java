package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedPurchaseOrderLineRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedPurchaseOrderRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptLineRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptRepository;
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
public class DBBasedPurchaseOrderIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedPurchaseOrderIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedPurchaseOrderRepository dbBasedPurchaseOrderRepository;
    @Autowired
    DBBasedPurchaseOrderLineRepository dbBasedPurchaseOrderLineRepository;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;



    public List<DBBasedPurchaseOrder> findAll(String companyCode,
                                              Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                              String statusList, Long id) {

        return dbBasedPurchaseOrderRepository.findAll(
                (Root<DBBasedPurchaseOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedPurchaseOrder findById(Long id) {
        return dbBasedPurchaseOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("purchase order data not found by id: " + id));
    }


    public IntegrationPurchaseOrderData addIntegrationPurchaseOrderData(DBBasedPurchaseOrder dbBasedPurchaseOrder) {
        int index = 0;
        dbBasedPurchaseOrder.setStatus(IntegrationStatus.PENDING);

        if (Objects.isNull(dbBasedPurchaseOrder.getAllowUnexpectedItem())) {
            dbBasedPurchaseOrder.setAllowUnexpectedItem(false);
        }
        for (DBBasedPurchaseOrderLine dbBasedPurchaseOrderLine : dbBasedPurchaseOrder.getPurchaseOrderLines()) {

            dbBasedPurchaseOrderLine.setStatus(IntegrationStatus.ATTACHED);
            dbBasedPurchaseOrderLine.setPurchaseOrder(dbBasedPurchaseOrder);
            dbBasedPurchaseOrderLine.setId(null);
            if (Strings.isBlank(dbBasedPurchaseOrderLine.getNumber())) {
                dbBasedPurchaseOrderLine.setNumber(index + "");
            }
            index++;



        }
        dbBasedPurchaseOrder.setId(null);

        return dbBasedPurchaseOrderRepository.save(dbBasedPurchaseOrder);
    }

    private List<DBBasedPurchaseOrder> findPendingIntegration() {
        return dbBasedPurchaseOrderRepository.findAll(
                (Root<DBBasedPurchaseOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedPurchaseOrder save(DBBasedPurchaseOrder dbBasedPurchaseOrder) {
        return dbBasedPurchaseOrderRepository.save(dbBasedPurchaseOrder);
    }

    public void listen() {
        logger.debug("Start to process Purchase Order data");
        List<DBBasedPurchaseOrder> dbBasedPurchaseOrders = findPendingIntegration();
        logger.debug(">> get {} Purchase Order data to be processed", dbBasedPurchaseOrders.size());
        dbBasedPurchaseOrders.forEach(dbBasedPurchaseOrder -> process(dbBasedPurchaseOrder));
    }

    private void process(DBBasedPurchaseOrder dbBasedPurchaseOrder) {


        try {

            PurchaseOrder purchaseOrder
                    = dbBasedPurchaseOrder.convertToPurchaseOrder(
                            warehouseLayoutServiceRestemplateClient,
                    inventoryServiceRestemplateClient, commonServiceRestemplateClient);

            // we will support host to send name
            // instead of id for the following field . In such case, we will
            // set to setup the IDs from the name so that
            // the correpondent service can handle it
            // 1. warehouse
            // 2. client
            // 3. Supplier
            setupMissingField(purchaseOrder, dbBasedPurchaseOrder);


            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process purchase order:\n{}", purchaseOrder);

            kafkaSender.send(IntegrationType.INTEGRATION_PURCHASE_ORDER,
                    purchaseOrder.getWarehouseId() + "-" + dbBasedPurchaseOrder.getId(), purchaseOrder);

            dbBasedPurchaseOrder.setStatus(IntegrationStatus.SENT);
            dbBasedPurchaseOrder.setErrorMessage("");

            dbBasedPurchaseOrder = save(dbBasedPurchaseOrder);

            // Save the order line as well
            dbBasedPurchaseOrder.getPurchaseOrderLines().forEach(dbBasedPurchaseOrderLine ->{
                dbBasedPurchaseOrderLine.setStatus(IntegrationStatus.SENT);
                dbBasedPurchaseOrderLine.setErrorMessage("");

                dbBasedPurchaseOrderLineRepository.save(dbBasedPurchaseOrderLine);
            });

            logger.debug(">> purchase order data process, {}", dbBasedPurchaseOrder.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedPurchaseOrder.setStatus(IntegrationStatus.ERROR);
            dbBasedPurchaseOrder.setErrorMessage(ex.getMessage());

            dbBasedPurchaseOrder = save(dbBasedPurchaseOrder);

            // Save the order line as well
            dbBasedPurchaseOrder.getPurchaseOrderLines().forEach(dbBasedPurchaseOrderLine ->{
                dbBasedPurchaseOrderLine.setStatus(IntegrationStatus.ERROR);
                dbBasedPurchaseOrderLine.setErrorMessage(ex.getMessage());

                dbBasedPurchaseOrderLineRepository.save(dbBasedPurchaseOrderLine);
            });
        }

    }


    private void setupMissingField(PurchaseOrder purchaseOrder, DBBasedPurchaseOrder dbBasedPurchaseOrder) throws UnsupportedEncodingException {

        if (Objects.isNull(purchaseOrder.getSupplierId())) {
            // if the supplier id is not set up
            // let's get the supplier from the
            // 1. quickbook list id if the integration is from quickbook
            // 2. by name
            Supplier supplier = null;
            if (Strings.isNotBlank(purchaseOrder.getQuickbookVendorListId())) {
                supplier = commonServiceRestemplateClient.getSupplierByQuickbookListId(
                        purchaseOrder.getWarehouseId(),
                        purchaseOrder.getQuickbookVendorListId()
                );
                logger.debug("Get supplier {} by quickbook list id {}",
                        supplier.getName(),
                        purchaseOrder.getQuickbookVendorListId());
            }
            else if (Strings.isNotBlank(dbBasedPurchaseOrder.getSupplierName())) {
                supplier = commonServiceRestemplateClient.getSupplierByName(
                        purchaseOrder.getWarehouseId(),
                        dbBasedPurchaseOrder.getSupplierName()
                );
            }
            if (Objects.nonNull(supplier)) {
                purchaseOrder.setSupplierId(supplier.getId());
            }
        }

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(purchaseOrder.getWarehouseId());
        purchaseOrder.getPurchaseOrderLines().forEach(purchaseOrderLine -> {
            // Get the matched receipt line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            dbBasedPurchaseOrder.getPurchaseOrderLines().forEach(dbBasedPurchaseOrderLine -> {
                if (purchaseOrderLine.getNumber().equals(dbBasedPurchaseOrderLine.getNumber())) {
                    setupMissingField(warehouse, purchaseOrderLine, dbBasedPurchaseOrderLine);
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
     * @param warehouse      Warehouse
     */
    private void setupMissingField(Warehouse warehouse, PurchaseOrderLine purchaseOrderLine,
                                   DBBasedPurchaseOrderLine dbBasedPurchaseOrderLine){

        // 1. item Id
        if (Objects.isNull(purchaseOrderLine.getItemId())) {
            Item item = null;
            logger.debug("item id is not passed in for line {}, let's set it up",
                    purchaseOrderLine.getNumber());

            logger.debug("item name: {}", dbBasedPurchaseOrderLine.getItemName());
            logger.debug("item quickbook list id: {}", dbBasedPurchaseOrderLine.getItemQuickbookListId());

            if (Strings.isNotBlank(dbBasedPurchaseOrderLine.getItemQuickbookListId())) {
                item = inventoryServiceRestemplateClient.getItemByQuickbookListId(
                        warehouse.getCompany().getId(),
                        purchaseOrderLine.getWarehouseId(), dbBasedPurchaseOrderLine.getItemQuickbookListId()
                );
            }
            else if (Strings.isNotBlank(dbBasedPurchaseOrderLine.getItemName())) {
                item = inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getCompany().getId(),
                        purchaseOrderLine.getWarehouseId(),
                        dbBasedPurchaseOrderLine.getPurchaseOrder().getClientId(),
                        dbBasedPurchaseOrderLine.getItemName()
                );
            }
            else {
                throw MissingInformationException.raiseException("Either item id, or item name, or quickbook item list id " +
                        " needs to be present in order to identify the item for this order line");
            }
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item based on the order line's information");
            }
            purchaseOrderLine.setItemId(item.getId());
        }

        purchaseOrderLine.setItem(
                inventoryServiceRestemplateClient.getItemById(
                        purchaseOrderLine.getItemId()
                )
        );


        // 2. warehouse Id

        if(Objects.isNull(purchaseOrderLine.getWarehouseId())) {
            purchaseOrderLine.setWarehouseId(warehouse.getId());
        }



    }



    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the purchase order integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedPurchaseOrder dbBasedPurchaseOrder = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedPurchaseOrder.setStatus(integrationStatus);
        dbBasedPurchaseOrder.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedPurchaseOrder);

        dbBasedPurchaseOrder.getPurchaseOrderLines().forEach(dbBasedPurchaseOrderLine ->{
            dbBasedPurchaseOrderLine.setStatus(integrationStatus);
            dbBasedPurchaseOrderLine.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedPurchaseOrderLineRepository.save(dbBasedPurchaseOrderLine);
        });

    }

    public IntegrationPurchaseOrderData resendPurchaseOrderData(Long id) {
        DBBasedPurchaseOrder dbBasedPurchaseOrder =
                findById(id);
        dbBasedPurchaseOrder.setStatus(IntegrationStatus.PENDING);
        dbBasedPurchaseOrder.setErrorMessage("");
        return save(dbBasedPurchaseOrder);
    }
}

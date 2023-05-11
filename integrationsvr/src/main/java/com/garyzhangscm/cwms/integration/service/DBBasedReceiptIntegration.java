package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
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
public class DBBasedReceiptIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedReceiptIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedReceiptRepository dbBasedReceiptRepository;
    @Autowired
    DBBasedReceiptLineRepository dbBasedReceiptLineRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;



    public List<DBBasedReceipt> findAll(String companyCode,
                                        Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                        String statusList, Long id) {

        return dbBasedReceiptRepository.findAll(
                (Root<DBBasedReceipt> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedReceipt findById(Long id) {
        return dbBasedReceiptRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("receipt data not found by id: " + id));
    }


    public IntegrationReceiptData addIntegrationReceiptData(DBBasedReceipt dbBasedReceipt) {
        int index = 0;

        if (Objects.isNull(dbBasedReceipt.getAllowUnexpectedItem())) {
            dbBasedReceipt.setAllowUnexpectedItem(false);
        }
        for (DBBasedReceiptLine dbBasedReceiptLine : dbBasedReceipt.getReceiptLines()) {

            dbBasedReceiptLine.setReceipt(dbBasedReceipt);
            dbBasedReceiptLine.setId(null);
            if (Strings.isBlank(dbBasedReceiptLine.getNumber())) {
                dbBasedReceiptLine.setNumber(index + "");
            }
            index++;

            if (Objects.isNull(dbBasedReceiptLine.getOverReceivingQuantity())) {
                dbBasedReceiptLine.setOverReceivingQuantity(0l);
            }
            if (Objects.isNull(dbBasedReceiptLine.getOverReceivingPercent())) {
                dbBasedReceiptLine.setOverReceivingPercent(0.0);
            }

        }
        dbBasedReceipt.setId(null);

        return dbBasedReceiptRepository.save(dbBasedReceipt);
    }

    private List<DBBasedReceipt> findPendingIntegration() {
        return dbBasedReceiptRepository.findAll(
                (Root<DBBasedReceipt> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedReceipt save(DBBasedReceipt dbBasedReceipt) {
        return dbBasedReceiptRepository.save(dbBasedReceipt);
    }

    public void listen() {
        logger.debug("Start to process Receipt data");
        List<DBBasedReceipt> dbBasedReceipts = findPendingIntegration();
        logger.debug(">> get {} Receipt data to be processed", dbBasedReceipts.size());
        dbBasedReceipts.forEach(dbBasedReceipt -> process(dbBasedReceipt));
    }

    private void process(DBBasedReceipt dbBasedReceipt) {


        try {

            Receipt receipt = dbBasedReceipt.convertToReceipt(warehouseLayoutServiceRestemplateClient);

            // we will support host to send name
            // instead of id for the following field . In such case, we will
            // set to setup the IDs from the name so that
            // the correpondent service can handle it
            // 1. warehouse
            // 2. client
            // 3. Supplier
            setupMissingField(receipt, dbBasedReceipt);


            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Receipt:\n{}", receipt);

            kafkaSender.send(IntegrationType.INTEGRATION_RECEIPT,
                    receipt.getWarehouseId() + "-" + dbBasedReceipt.getId(), receipt);

            dbBasedReceipt.setStatus(IntegrationStatus.SENT);
            dbBasedReceipt.setErrorMessage("");

            dbBasedReceipt = save(dbBasedReceipt);

            // Save the order line as well
            dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine ->{
                dbBasedReceiptLine.setStatus(IntegrationStatus.SENT);
                dbBasedReceiptLine.setErrorMessage("");

                dbBasedReceiptLineRepository.save(dbBasedReceiptLine);
            });

            logger.debug(">> Receipt data process, {}", dbBasedReceipt.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedReceipt.setStatus(IntegrationStatus.ERROR);
            dbBasedReceipt.setErrorMessage(ex.getMessage());

            dbBasedReceipt = save(dbBasedReceipt);

            // Save the order line as well
            dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine ->{
                dbBasedReceiptLine.setStatus(IntegrationStatus.ERROR);
                dbBasedReceiptLine.setErrorMessage(ex.getMessage());

                dbBasedReceiptLineRepository.save(dbBasedReceiptLine);
            });
        }

    }


    private void setupMissingField(Receipt receipt, DBBasedReceipt dbBasedReceipt) throws UnsupportedEncodingException {

        if (Objects.isNull(receipt.getClientId()) &&
                Strings.isNotBlank(dbBasedReceipt.getClientName())) {
            receipt.setClientId(
                    commonServiceRestemplateClient.getClientByName(
                            receipt.getWarehouseId(),
                            dbBasedReceipt.getClientName()
                    ).getId()
            );
        }
        if (Objects.isNull(receipt.getSupplierId()) &&
               Strings.isNotBlank(dbBasedReceipt.getSupplierName())) {
            receipt.setSupplierId(

                    commonServiceRestemplateClient.getSupplierByName(
                            receipt.getWarehouseId(),
                            dbBasedReceipt.getSupplierName()
                    ).getId()
            );
        }

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(receipt.getWarehouseId());
        receipt.getReceiptLines().forEach(receiptLine -> {
            // Get the matched receipt line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine -> {
                if (receiptLine.getNumber().equals(dbBasedReceiptLine.getNumber())) {
                    setupMissingField(warehouse, receiptLine, dbBasedReceiptLine);
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
     * @param receiptLine
     * @param dbBasedReceiptLine
     */
    private void setupMissingField(Warehouse warehouse, ReceiptLine receiptLine, DBBasedReceiptLine dbBasedReceiptLine){

        // 1. item Id
        if(Objects.isNull(receiptLine.getItemId())) {
            receiptLine.setItemId(
                        inventoryServiceRestemplateClient.getItemByName(
                                warehouse.getCompany().getId(),
                                warehouse.getId(),
                                dbBasedReceiptLine.getReceipt().getClientId(),
                                dbBasedReceiptLine.getItemName()
                        ).getId()
                );
        }

        receiptLine.setItem(
                inventoryServiceRestemplateClient.getItemById(
                        receiptLine.getItemId()
                )
        );


        // 2. warehouse Id

        if(Objects.isNull(receiptLine.getWarehouseId())) {
            receiptLine.setWarehouseId(warehouse.getId());
        }



    }



    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the receipt integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedReceipt dbBasedReceipt = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedReceipt.setStatus(integrationStatus);
        dbBasedReceipt.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedReceipt);

        dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine ->{
            dbBasedReceiptLine.setStatus(integrationStatus);
            dbBasedReceiptLine.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedReceiptLineRepository.save(dbBasedReceiptLine);
        });

    }

    public IntegrationReceiptData resendReceiptData(Long id) {
        DBBasedReceipt dbBasedReceipt =
                findById(id);
        dbBasedReceipt.setStatus(IntegrationStatus.PENDING);
        dbBasedReceipt.setErrorMessage("");
        return save(dbBasedReceipt);
    }
}

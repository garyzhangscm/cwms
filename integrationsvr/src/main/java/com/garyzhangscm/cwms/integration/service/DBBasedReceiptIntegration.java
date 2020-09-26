package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderLineRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptLineRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


    public List<DBBasedReceipt> findAll() {
        return dbBasedReceiptRepository.findAll();
    }
    public DBBasedReceipt findById(Long id) {
        return dbBasedReceiptRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order data not found by id: " + id));
    }


    public IntegrationReceiptData addIntegrationReceiptData(DBBasedReceipt dbBasedReceipt) {
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
        );
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

        Receipt receipt = dbBasedReceipt.convertToReceipt();

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

        kafkaSender.send(IntegrationType.INTEGRATION_RECEIPT, receipt);

        dbBasedReceipt.setStatus(IntegrationStatus.COMPLETED);
        dbBasedReceipt.setLastUpdateTime(LocalDateTime.now());
        dbBasedReceipt = save(dbBasedReceipt);

        // Save the order line as well
        dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine ->{
            dbBasedReceiptLine.setStatus(IntegrationStatus.COMPLETED);
            dbBasedReceiptLine.setLastUpdateTime(LocalDateTime.now());
            dbBasedReceiptLineRepository.save(dbBasedReceiptLine);
        });

        logger.debug(">> Receipt data process, {}", dbBasedReceipt.getStatus());
    }


    private void setupMissingField(Receipt receipt, DBBasedReceipt dbBasedReceipt){



        receipt.getReceiptLines().forEach(receiptLine -> {
            // Get the matched receipt line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            dbBasedReceipt.getReceiptLines().forEach(dbBasedReceiptLine -> {
                if (receiptLine.getNumber().equals(dbBasedReceiptLine.getNumber())) {
                    setupMissingField(receipt.getWarehouseId(), receiptLine, dbBasedReceiptLine);
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
     * @param warehouseId      Warehouse id
     * @param receiptLine
     * @param dbBasedReceiptLine
     */
    private void setupMissingField(Long warehouseId, ReceiptLine receiptLine, DBBasedReceiptLine dbBasedReceiptLine){

        // 1. item Id
        if(Objects.isNull(receiptLine.getItemId())) {
            receiptLine.setItemId(
                        inventoryServiceRestemplateClient.getItemByName(
                                warehouseId, dbBasedReceiptLine.getItemName()
                        ).getId()
                );
        }


        // 2. warehouse Id

        if(Objects.isNull(receiptLine.getItemId())) {
            receiptLine.setWarehouseId(warehouseId);
        }



    }



}

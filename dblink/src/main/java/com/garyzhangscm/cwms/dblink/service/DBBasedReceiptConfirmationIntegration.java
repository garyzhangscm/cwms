package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedReceiptConfirmation;
import com.garyzhangscm.cwms.dblink.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.dblink.repository.DBBasedReceiptConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class DBBasedReceiptConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedReceiptConfirmationIntegration.class);


    @Autowired
    private DBBasedReceiptConfirmationRepository dbBasedReceiptConfirmationRepository;

    public DBBasedReceiptConfirmation save(DBBasedReceiptConfirmation dbBasedReceiptConfirmation) {

        dbBasedReceiptConfirmation.getReceiptLines().forEach(
                dbBasedReceiptLineConfirmation -> {
                    dbBasedReceiptLineConfirmation.setReceipt(
                            dbBasedReceiptConfirmation
                    );
                    dbBasedReceiptLineConfirmation.setTransactionDate(LocalDateTime.now());
                }
        );
        return dbBasedReceiptConfirmationRepository.save(dbBasedReceiptConfirmation);
    }


}

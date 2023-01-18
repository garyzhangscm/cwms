package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrderReceivingConfirmation;
import com.garyzhangscm.cwms.dblink.model.InventoryQuantityChangeType;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderReceivingConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class DBBasedInventoryAdjustmentConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAdjustmentConfirmationIntegration.class);


    @Autowired
    private DBBasedWorkOrderReceivingConfirmationRepository dbBasedWorkOrderReceivingConfirmationRepository;

    public DBBasedInventoryAdjustmentConfirmation save(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        logger.debug("Save the data based on the type: {}",
                dbBasedInventoryAdjustmentConfirmation.getInventoryQuantityChangeType());
        if (Objects.isNull(dbBasedInventoryAdjustmentConfirmation.getInventoryQuantityChangeType())) {
            dbBasedInventoryAdjustmentConfirmation.setInventoryQuantityChangeType(
                    InventoryQuantityChangeType.UNKNOWN
            );
        }
        switch (dbBasedInventoryAdjustmentConfirmation.getInventoryQuantityChangeType()) {
            case PRODUCING:
                saveWorkOrderReceivingConfirmation(dbBasedInventoryAdjustmentConfirmation);
                break;
            default:
                break;
        }
        return dbBasedInventoryAdjustmentConfirmation;
    }

    private void saveWorkOrderReceivingConfirmation(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        logger.debug("Will save as a work order receiving confirmation data");
        DBBasedWorkOrderReceivingConfirmation dbBasedWorkOrderReceivingConfirmation
                = new DBBasedWorkOrderReceivingConfirmation(dbBasedInventoryAdjustmentConfirmation);

        logger.debug(dbBasedWorkOrderReceivingConfirmation.toString());
        dbBasedWorkOrderReceivingConfirmationRepository.save(
                dbBasedWorkOrderReceivingConfirmation
        );

    }


}

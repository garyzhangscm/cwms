package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrderReceivingConfirmation;
import com.garyzhangscm.cwms.dblink.model.InventoryQuantityChangeType;
import com.garyzhangscm.cwms.dblink.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderReceivingConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class DBBasedOrderConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedOrderConfirmationIntegration.class);


    @Autowired
    private DBBasedOrderConfirmationRepository dbBasedOrderConfirmationRepository;

    public DBBasedOrderConfirmation save(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        return dbBasedOrderConfirmationRepository.save(dbBasedOrderConfirmation);
    }


}

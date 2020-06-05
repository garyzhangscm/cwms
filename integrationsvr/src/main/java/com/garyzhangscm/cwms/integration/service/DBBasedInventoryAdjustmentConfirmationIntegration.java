package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DBBasedInventoryAdjustmentConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAdjustmentConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedInventoryAdjustmentConfirmationRepository dbBasedInventoryAdjustmentConfirmationRepository;


    public List<DBBasedInventoryAdjustmentConfirmation> findAll() {
        return dbBasedInventoryAdjustmentConfirmationRepository.findAll();
    }
    public DBBasedInventoryAdjustmentConfirmation findById(Long id) {
        return dbBasedInventoryAdjustmentConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    private DBBasedInventoryAdjustmentConfirmation save(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationRepository.save(dbBasedInventoryAdjustmentConfirmation);
    }

    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {

        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation =
                getDBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);

        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.COMPLETED);
        dbBasedInventoryAdjustmentConfirmation.setLastUpdateTime(LocalDateTime.now());
        return save(dbBasedInventoryAdjustmentConfirmation);
    }

    private DBBasedInventoryAdjustmentConfirmation getDBBasedInventoryAdjustmentConfirmation(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return new DBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);
    }
}

package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAttributeChangeConfirmationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DBBasedInventoryAttributeChangeConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAttributeChangeConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedInventoryAttributeChangeConfirmationRepository dbBasedInventoryAttributeChangeConfirmationRepository;


    public List<DBBasedInventoryAttributeChangeConfirmation> findAll() {
        return dbBasedInventoryAttributeChangeConfirmationRepository.findAll();
    }
    public DBBasedInventoryAttributeChangeConfirmation findById(Long id) {
        return dbBasedInventoryAttributeChangeConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    private DBBasedInventoryAttributeChangeConfirmation save(DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAttributeChangeConfirmationRepository.save(dbBasedInventoryAdjustmentConfirmation);
    }

    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {

        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAttributeChangeConfirmation =
                getDBBasedInventoryAttributeChangeConfirmation(inventoryAttributeChangeConfirmation);

        dbBasedInventoryAttributeChangeConfirmation.setStatus(IntegrationStatus.COMPLETED);
        dbBasedInventoryAttributeChangeConfirmation.setLastUpdateTime(LocalDateTime.now());
        return save(dbBasedInventoryAttributeChangeConfirmation);
    }

    private DBBasedInventoryAttributeChangeConfirmation getDBBasedInventoryAttributeChangeConfirmation(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return new DBBasedInventoryAttributeChangeConfirmation(inventoryAttributeChangeConfirmation);
    }
}

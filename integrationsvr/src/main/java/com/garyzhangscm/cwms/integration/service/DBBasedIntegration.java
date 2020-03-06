package com.garyzhangscm.cwms.integration.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"aws-dev","default", "dev"})
public class DBBasedIntegration implements Integration{
    private static final Logger logger = LoggerFactory.getLogger(DBBasedIntegration.class);

    @Autowired
    DBBasedCustomerIntegration dbBasedCustomerIntegration;

    @Autowired
    DBBasedSupplierIntegration dbBasedSupplierIntegration;

    @Autowired
    DBBasedClientIntegration dbBasedClientIntegration;

    @Autowired
    DBBasedItemUnitOfMeasureIntegration dbBasedItemUnitOfMeasureIntegration;

    public void listen() {
        logger.debug("Will use DBBasedIntegration to process the integration data");

        logger.debug("#1 Customer data");
        dbBasedCustomerIntegration.listen();

        logger.debug("#2 Supplier data");
        dbBasedSupplierIntegration.listen();

        logger.debug("#3 Client data");
        dbBasedClientIntegration.listen();

        logger.debug("#7 Item Unit Of Measure");
        dbBasedItemUnitOfMeasureIntegration.listen();

    }
}

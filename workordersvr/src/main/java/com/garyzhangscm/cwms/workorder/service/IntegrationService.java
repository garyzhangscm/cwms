package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.KafkaSender;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.model.WorkOrderConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);


    @Autowired
    private KafkaSender kafkaSender;

    public void process(WorkOrderConfirmation workOrderConfirmation) {

        kafkaSender.send(workOrderConfirmation);
        logger.debug(">> work order confirmation sent!");

    }


}

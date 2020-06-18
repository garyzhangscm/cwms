package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);

    @Autowired
    private OrderService orderService;
    // Add/ change item
    public void process(Order order) {
         orderService.saveOrUpdate(order);
        logger.debug(">> order information saved!");



    }


}

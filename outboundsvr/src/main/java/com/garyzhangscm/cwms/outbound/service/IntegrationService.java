package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.KafkaSender;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.instrument.messaging.SleuthMessagingProperties;
import org.springframework.stereotype.Service;


@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);


    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaSender kafkaSender;

    // Add/ change item
    public void process(Order order) {
        // We will init the order with some default 0 quantity
        // Since when we get order from integration, the integration
        // data will only need to specify the expect quantity
        order.getOrderLines().forEach(orderLine -> {
            orderLine.setOpenQuantity(orderLine.getExpectedQuantity());
            orderLine.setInprocessQuantity(0L);
            orderLine.setShippedQuantity(0L);
            orderLine.setOrder(order);
        });
         orderService.addOrders(order);
        logger.debug(">> order information saved!");

    }

    public void process(OrderConfirmation orderConfirmation) {

        kafkaSender.send(orderConfirmation);
        logger.debug(">> order confirmation sent!");

    }


}

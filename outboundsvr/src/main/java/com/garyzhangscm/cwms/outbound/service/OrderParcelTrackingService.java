package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.Carrier;
import com.garyzhangscm.cwms.outbound.model.CarrierServiceLevel;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderParcelTracking;
import com.garyzhangscm.cwms.outbound.repository.OrderParcelTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class OrderParcelTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderParcelTrackingService.class);

    @Autowired
    private OrderParcelTrackingRepository orderParcelTrackingRepository;

    public OrderParcelTracking findByTrackingNumber(String trackingNumber) {
        return orderParcelTrackingRepository.findByTrackingNumber(trackingNumber);
    }

    public OrderParcelTracking save(OrderParcelTracking orderParcelTracking) {
        return orderParcelTrackingRepository.save(orderParcelTracking);
    }

    public OrderParcelTracking saveOrUpdate(OrderParcelTracking orderParcelTracking) {
        if (Objects.isNull(orderParcelTracking.getId()) &&
            Objects.nonNull(findByTrackingNumber(orderParcelTracking.getTrackingNumber()))) {
            orderParcelTracking.setId(
                    findByTrackingNumber(orderParcelTracking.getTrackingNumber()).getId()
            );
        }

        return save(orderParcelTracking);
    }
    public OrderParcelTracking addTracking(Order order,
                                           String trackingNumber,
                                           Long carrierId,
                                           Long carrierServiceLevelId) {

        OrderParcelTracking orderParcelTracking = new OrderParcelTracking(
                order, trackingNumber, carrierId, carrierServiceLevelId
        );
        return saveOrUpdate(orderParcelTracking);

    }
}

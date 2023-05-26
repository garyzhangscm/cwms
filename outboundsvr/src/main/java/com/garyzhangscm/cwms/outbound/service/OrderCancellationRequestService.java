/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.OrderCancellationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class OrderCancellationRequestService {
    private static final Logger logger = LoggerFactory.getLogger(OrderCancellationRequestService.class);


    @Autowired
    private OrderCancellationRequestRepository orderCancellationRequestRepository;

    public OrderCancellationRequest save(OrderCancellationRequest orderCancellationRequest) {
        return orderCancellationRequestRepository.save(orderCancellationRequest);
    }

    public OrderCancellationRequest createOrderCancellationRequest(Order order,
                                                                   String cancelRequestedUsername,
                                                                   OrderCancellationRequestResult result,
                                                                   String message) {
        OrderCancellationRequest orderCancellationRequest =
                new OrderCancellationRequest(order, cancelRequestedUsername, result, message);

        return save(orderCancellationRequest);
    }
    public OrderCancellationRequest createFailedOrderCancellationRequest(Order order,
                                                                         String cancelRequestedUsername,
                                                                         String message) {
        return createOrderCancellationRequest(
                order, cancelRequestedUsername, OrderCancellationRequestResult.FAIL, message
        );
    }
}

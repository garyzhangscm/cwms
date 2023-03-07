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

package com.garyzhangscm.cwms.inbound.service;

import com.garyzhangscm.cwms.inbound.clients.KafkaSender;
import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillableActivityService {
    private static final Logger logger = LoggerFactory.getLogger(BillableActivityService.class);

    @Autowired
    private KafkaSender kafkaSender;

    public void sendBillableActivity(Long companyId,
                                                   Long warehouseId,
                                                   Long clientId,
                                                   Long amount,
                                                   String documentNumber,
                                                   String itemNumber,
                                                   BillableCategory billableCategory) {
        BillableActivity billableActivity = createBillableActivity(companyId,
                warehouseId, clientId,
                amount, documentNumber, itemNumber, billableCategory);

        logger.debug("start to send billable activity:\n{}",
                billableActivity);
        kafkaSender.send(billableActivity);
    }
    public BillableActivity createBillableActivity(Long companyId,
                                                   Long warehouseId,
                                                   Long clientId,
                                                   Long amount,
                                                   String documentNumber,
                                                   String itemNumber,
                                                   BillableCategory billableCategory) {
        return createBillableActivity(companyId,
                warehouseId, clientId,
                amount, documentNumber, itemNumber, billableCategory, 0.0);
    }

    public BillableActivity createBillableActivity(Long companyId,
                                                   Long warehouseId,
                                                   Long clientId,
                                                   Long amount,
                                                   String documentNumber,
                                                   String itemNumber,
                                                   BillableCategory billableCategory,
                                                   Double rate) {
        return createBillableActivity(companyId,
                warehouseId, clientId,
                amount, documentNumber, itemNumber, billableCategory, rate,
                amount * rate);
    }

    public BillableActivity createBillableActivity(Long companyId,
                                                   Long warehouseId,
                                                   Long clientId,
                                                   Long amount,
                                                   String documentNumber,
                                                   String itemNumber,
                                                   BillableCategory billableCategory,
                                                   Double rate,
                                                   Double totalCharge) {
        BillableActivity billableActivity = new BillableActivity();
        billableActivity.setCompanyId(companyId);
        billableActivity.setWarehouseId(warehouseId);
        billableActivity.setClientId(clientId);
        billableActivity.setAmount(amount.doubleValue());
        billableActivity.setDocumentNumber(documentNumber);
        billableActivity.setItemNumber(itemNumber);

        billableActivity.setBillableCategory(billableCategory);
        billableActivity.setRate(rate);
        billableActivity.setTotalCharge(totalCharge);

        return billableActivity;
    }
}

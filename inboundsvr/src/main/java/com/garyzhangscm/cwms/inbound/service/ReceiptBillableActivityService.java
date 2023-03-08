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
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.model.BillableActivity;
import com.garyzhangscm.cwms.inbound.model.BillableCategory;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptBillableActivity;
import com.garyzhangscm.cwms.inbound.repository.ReceiptBillableActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ReceiptBillableActivityService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptBillableActivityService.class);

    @Autowired
    private ReceiptBillableActivityRepository receiptBillableActivityRepository;

    @Autowired
    private ReceiptService receiptService;


    public ReceiptBillableActivity save(ReceiptBillableActivity receiptBillableActivity) {
        return receiptBillableActivityRepository.save(receiptBillableActivity);
    }

    public ReceiptBillableActivity findByType(Long receiptId, Long billableActivityTypeId) {
        return receiptBillableActivityRepository.findByType(receiptId, billableActivityTypeId);
    }

    public ReceiptBillableActivity saveOrUpdate(ReceiptBillableActivity receiptBillableActivity) {
        if (Objects.isNull(receiptBillableActivity.getId()) &&
                Objects.nonNull(findByType(receiptBillableActivity.getReceipt().getId(), receiptBillableActivity.getBillableActivityTypeId()))) {
            receiptBillableActivity.setId(
                    findByType(
                            receiptBillableActivity.getReceipt().getId(),
                            receiptBillableActivity.getBillableActivityTypeId()
                    ).getId()
            );
        }

        return save(receiptBillableActivity);
    }

    public ReceiptBillableActivity addReceiptBillableActivity(Long receiptId, ReceiptBillableActivity receiptBillableActivity) {
        ReceiptBillableActivity existingReceiptBillableActivity = findByType(
                receiptId, receiptBillableActivity.getBillableActivityTypeId()
        );
        if (Objects.nonNull(existingReceiptBillableActivity)) {
            // we already have the billable activity with same type, let's just change it instead of
            // create a new activity record with same type in the same receipt
            existingReceiptBillableActivity.setRate(receiptBillableActivity.getRate());
            existingReceiptBillableActivity.setAmount(receiptBillableActivity.getAmount());
            existingReceiptBillableActivity.setTotalCharge(receiptBillableActivity.getTotalCharge());
            return saveOrUpdate(existingReceiptBillableActivity);
        }
        // we don't have any existing billable activity with same type in this receipt, let's create one
        Receipt receipt = receiptService.findById(receiptId);
        receiptBillableActivity.setReceipt(receipt);

        return saveOrUpdate(receiptBillableActivity);

    }

    public ReceiptBillableActivity changeReceiptBillableActivity(ReceiptBillableActivity receiptBillableActivity) {
        if (Objects.isNull(receiptBillableActivity.getId())) {
            throw ReceiptOperationException.raiseException("Can't change an non exists billable activity");
        }
        return saveOrUpdate(receiptBillableActivity);

    }
    public void removeReceiptBillableActivity(ReceiptBillableActivity receiptBillableActivity) {
        if (Objects.isNull(receiptBillableActivity.getId())) {
            throw ReceiptOperationException.raiseException("Can't remove an non exists billable activity");
        }
        removeReceiptBillableActivity(receiptBillableActivity.getId());

    }
    public void removeReceiptBillableActivity(Long id) {
        receiptBillableActivityRepository.deleteById(id);

    }
}

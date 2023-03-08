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

import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptBillableActivity;
import com.garyzhangscm.cwms.inbound.model.ReceiptLine;
import com.garyzhangscm.cwms.inbound.model.ReceiptLineBillableActivity;
import com.garyzhangscm.cwms.inbound.repository.ReceiptBillableActivityRepository;
import com.garyzhangscm.cwms.inbound.repository.ReceiptLineBillableActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ReceiptLineBillableActivityService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptLineBillableActivityService.class);

    @Autowired
    private ReceiptLineBillableActivityRepository receiptLineBillableActivityRepository;

    @Autowired
    private ReceiptLineService receiptLineService;


    public ReceiptLineBillableActivity save(ReceiptLineBillableActivity receiptLineBillableActivity) {
        return receiptLineBillableActivityRepository.save(receiptLineBillableActivity);
    }

    public ReceiptLineBillableActivity findByType(Long receiptLineId, Long billableActivityTypeId) {
        return receiptLineBillableActivityRepository.findByType(receiptLineId, billableActivityTypeId);
    }

    public ReceiptLineBillableActivity saveOrUpdate(ReceiptLineBillableActivity receiptLineBillableActivity) {
        if (Objects.isNull(receiptLineBillableActivity.getId()) &&
                Objects.nonNull(findByType(receiptLineBillableActivity.getReceiptLine().getId(), receiptLineBillableActivity.getBillableActivityTypeId()))) {
            receiptLineBillableActivity.setId(
                    findByType(
                            receiptLineBillableActivity.getReceiptLine().getId(),
                            receiptLineBillableActivity.getBillableActivityTypeId()
                    ).getId()
            );
        }

        return save(receiptLineBillableActivity);
    }

    public ReceiptLineBillableActivity addReceiptLineBillableActivity(Long receiptLineId, ReceiptLineBillableActivity receiptLineBillableActivity) {
        ReceiptLineBillableActivity existingReceiptLineBillableActivity = findByType(
                receiptLineId, receiptLineBillableActivity.getBillableActivityTypeId()
        );
        if (Objects.nonNull(existingReceiptLineBillableActivity)) {
            // we already have the billable activity with same type, let's just change it instead of
            // create a new activity record with same type in the same receipt
            existingReceiptLineBillableActivity.setRate(receiptLineBillableActivity.getRate());
            existingReceiptLineBillableActivity.setAmount(receiptLineBillableActivity.getAmount());
            existingReceiptLineBillableActivity.setTotalCharge(receiptLineBillableActivity.getTotalCharge());
            return saveOrUpdate(existingReceiptLineBillableActivity);
        }
        // we don't have any existing billable activity with same type in this receipt, let's create one
        ReceiptLine receiptLine = receiptLineService.findById(receiptLineId);
        receiptLineBillableActivity.setReceiptLine(receiptLine);

        return saveOrUpdate(receiptLineBillableActivity);

    }

    public ReceiptLineBillableActivity changeReceiptLineBillableActivity(ReceiptLineBillableActivity receiptLineBillableActivity) {
        if (Objects.isNull(receiptLineBillableActivity.getId())) {
            throw ReceiptOperationException.raiseException("Can't change an non exists billable activity");
        }
        return saveOrUpdate(receiptLineBillableActivity);

    }
    public void removeReceiptLineBillableActivity(ReceiptLineBillableActivity receiptLineBillableActivity) {
        if (Objects.isNull(receiptLineBillableActivity.getId())) {
            throw ReceiptOperationException.raiseException("Can't remove an non exists billable activity");
        }
        removeReceiptLineBillableActivity(receiptLineBillableActivity.getId());

    }
    public void removeReceiptLineBillableActivity(Long id) {
        receiptLineBillableActivityRepository.deleteById(id);

    }
}

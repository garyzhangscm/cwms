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

import com.garyzhangscm.cwms.inbound.model.Inventory;
import com.garyzhangscm.cwms.inbound.model.ReceiptLine;
import com.garyzhangscm.cwms.inbound.model.ReceivingTransaction;
import com.garyzhangscm.cwms.inbound.repository.ReceivingTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ReceivingTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(ReceivingTransactionService.class);


    @Autowired
    private ReceivingTransactionRepository receivingTransactionRepository;


    public ReceivingTransaction createReceivingTransaction(ReceiptLine receiptLine,
                                                           Inventory inventory) {

    }

}

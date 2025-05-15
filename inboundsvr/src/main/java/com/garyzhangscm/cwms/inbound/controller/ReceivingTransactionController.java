/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.inbound.controller;

import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReceivingTransactionController {
    private static final Logger logger = LoggerFactory.getLogger(ReceivingTransactionController.class);
    @Autowired
    ReceivingTransactionService receivingTransactionService;

    @RequestMapping(value="/receiving-transactions", method = RequestMethod.GET)
    public List<ReceivingTransaction> findAllReceivingTransactions(@RequestParam Long warehouseId,
                                         @RequestParam(name="receiptId", required = false, defaultValue = "") Long receiptId,
                                         @RequestParam(name="receiptLineId", required = false, defaultValue = "") Long receiptLineId,
                                                                   @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {
        return receivingTransactionService.findAll(warehouseId, receiptId, receiptLineId, loadDetails);
    }
}

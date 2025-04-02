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

import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickConfirmTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Objects;
import java.util.UUID;


@Service
public class PickConfirmTransactionService   {
    private static final Logger logger = LoggerFactory.getLogger(PickConfirmTransactionService.class);

    @Autowired
    private PickConfirmTransactionRepository pickConfirmTransactionRepository;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private HttpSession httpSession;

    public PickConfirmTransaction addRequest(Long companyId, Long warehouseId,
                                      Pick pick, String username,
                                      Long quantity,
                                             String sessionId) {
        return add(companyId, warehouseId, pick, username,
                PickConfirmTransactionType.REQUEST,
                quantity, "", sessionId);

    }
    public PickConfirmTransaction addConfirmation(Long companyId, Long warehouseId,
                                             Pick pick, String username,
                                             Long quantity, String lpn,
                                                  String sessionId) {
        return add(companyId, warehouseId, pick, username,
                PickConfirmTransactionType.CONFIRM,
                quantity, lpn, sessionId);

    }
    private PickConfirmTransaction add(Long companyId, Long warehouseId,
                                      Pick pick, String username,
                                      PickConfirmTransactionType type,
                                      Long quantity, String lpn,
                                       String sessionId) {
        if (Objects.isNull(companyId)) {
            companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId)
                    .getCompanyId();
        }


        PickConfirmTransaction pickConfirmTransaction = new PickConfirmTransaction(
                companyId, warehouseId,
                sessionId,
                pick, username, type,
                quantity, lpn
        );
        return pickConfirmTransactionRepository.save(pickConfirmTransaction);

    }
}

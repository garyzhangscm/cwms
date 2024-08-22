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

import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.ReceivingTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ReceivingTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(ReceivingTransactionService.class);


    @Autowired
    private ReceivingTransactionRepository receivingTransactionRepository;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public List<ReceivingTransaction> findAll(Long warehouseId, Long receiptId, Long receiptLineId, Boolean loadDetails) {
        List<ReceivingTransaction> receivingTransactions =
                receivingTransactionRepository.findAll(
                (Root<ReceivingTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(receiptId)) {
                        Join<ReceivingTransaction, Receipt> joinReceipt= root.join("receipt", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinReceipt.get("id"), receiptId));
                    }
                    if (Objects.nonNull(receiptLineId)) {
                        Join<ReceivingTransaction, ReceiptLine> joinReceipt= root.join("receiptLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinReceipt.get("id"), receiptLineId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "lastModifiedTime")
        );

        if (loadDetails) {
            loadAttribute(receivingTransactions);

        }
        return receivingTransactions;
    }

    public void loadAttribute(List<ReceivingTransaction> receivingTransactions) {
        for(ReceivingTransaction receivingTransaction : receivingTransactions) {
            loadAttribute(receivingTransaction);
        }
    }
    public void loadAttribute(ReceivingTransaction receivingTransaction) {
        if (Objects.nonNull(receivingTransaction.getItemId()) &&
              Objects.isNull(receivingTransaction.getItem())) {
            receivingTransaction.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            receivingTransaction.getItemId()
                    )
            );
        }

        if (Objects.nonNull(receivingTransaction.getItemPackageTypeId()) &&
                Objects.isNull(receivingTransaction.getItemPackageType())) {
            receivingTransaction.setItemPackageType(
                    inventoryServiceRestemplateClient.getItemPackageTypeById(
                            receivingTransaction.getItemPackageTypeId()
                    )
            );
        }

        if (Objects.nonNull(receivingTransaction.getInventoryStatusId()) &&
                Objects.isNull(receivingTransaction.getInventoryStatus())) {
            receivingTransaction.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            receivingTransaction.getInventoryStatusId()
                    )
            );
        }

    }


    public ReceivingTransaction createReceivingTransaction(ReceiptLine receiptLine,
                                                           Inventory inventory,
                                                           String username,
                                                           String rfCode) {

        return receivingTransactionRepository.save(
                new ReceivingTransaction(receiptLine, inventory,
                        ReceivingTransactionType.RECEIVING, username, rfCode));
    }

}

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

package com.garyzhangscm.cwms.inbound.repository;

import com.garyzhangscm.cwms.inbound.model.ReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ReceiptLineRepository extends JpaRepository<ReceiptLine, Long>, JpaSpecificationExecutor<ReceiptLine> {

    @Query("select rl from ReceiptLine rl where receipt.id = :receiptId and number = :number and warehouseId = :warehouseId")
    ReceiptLine findByNaturalKey(Long warehouseId, Long receiptId, String number);


    @Query("select rl from ReceiptLine rl inner join rl.receipt r where rl.itemId = :itemId " +
            " and r.receiptStatus != com.garyzhangscm.cwms.inbound.model.ReceiptStatus.CLOSED")
    List<ReceiptLine> findOpenReceiptLinesByItem(Long itemId);

    /**
     * Override a item in the warehouse level. We will change the receipt line's item id to the new warehouse level
     * item. We will only change the receipt line in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update receipt_line set item_id = :newItemId where item_id = :oldItemId  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long oldItemId, Long newItemId, Long warehouseId);
}

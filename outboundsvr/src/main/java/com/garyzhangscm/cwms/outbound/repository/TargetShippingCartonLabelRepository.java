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

package com.garyzhangscm.cwms.outbound.repository;


import com.garyzhangscm.cwms.outbound.model.TargetShippingCartonLabel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TargetShippingCartonLabelRepository extends JpaRepository<TargetShippingCartonLabel, Long>, JpaSpecificationExecutor<TargetShippingCartonLabel> {


    TargetShippingCartonLabel findBySSCC18(String SSCC18);

    List<TargetShippingCartonLabel> findByWarehouseIdAndPoNumber(Long warehouseId, String poNumber, Pageable pageable);

    @Query(value = "SELECT * FROM  target_shipping_carton_label WHERE pallet_pick_label_content_id = :palletPickLabelContentId" ,
            nativeQuery = true )
    List<TargetShippingCartonLabel> findByPalletPickLabelContentId(Long palletPickLabelContentId);

    @Query(value = "SELECT max(t.pieceCarton) FROM  TargetShippingCartonLabel t" +
            " WHERE t.warehouseId = :warehouseId " +
            "  and t.poNumber = :poNumber " +
            "  and t.itemNumber = :itemNumber " +
            "  and t.pieceCarton is not null and t.pieceCarton > 0 ")
    String getPieceCartonFromShippingCartonLabel(Long warehouseId, String poNumber, String itemNumber);
}

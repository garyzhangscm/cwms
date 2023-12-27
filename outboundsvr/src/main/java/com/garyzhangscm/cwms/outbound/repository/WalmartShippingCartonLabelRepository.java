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


import com.garyzhangscm.cwms.outbound.model.WalmartShippingCartonLabel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WalmartShippingCartonLabelRepository extends JpaRepository<WalmartShippingCartonLabel, Long>, JpaSpecificationExecutor<WalmartShippingCartonLabel> {


    WalmartShippingCartonLabel findBySSCC18(String SSCC18);

    List<WalmartShippingCartonLabel> findByWarehouseIdAndPoNumber(Long warehouseId, String poNumber, Pageable pageable);
/**
    @Query("select carton from WalmartShippingCartonLabel carton inner join carton.palletPickLabelContent pallet " +
            " where pallet.id = :palletPickLabelContentId ")
**/
    @Query(value = "SELECT * FROM  walmart_shipping_carton_label WHERE pallet_pick_label_content_id = :palletPickLabelContentId" ,
            nativeQuery = true )
    List<WalmartShippingCartonLabel> findByPalletPickLabelContentId(Long palletPickLabelContentId);
}

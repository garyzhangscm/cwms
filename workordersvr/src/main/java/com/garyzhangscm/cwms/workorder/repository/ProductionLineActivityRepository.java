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

package com.garyzhangscm.cwms.workorder.repository;

import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineActivity;
import com.garyzhangscm.cwms.workorder.model.ProductionLineActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionLineActivityRepository extends JpaRepository<ProductionLineActivity, Long>, JpaSpecificationExecutor<ProductionLineActivity> {

    /***
    @Query(value= "SELECT * FROM production_line_activity " +
            " where activity.productionLine.id = :productionLineId and type = :type" +
            " ORDER BY activity.transactionDate desc LIMIT 1")
    **/
    ProductionLineActivity findTop1ByProductionLineAndTypeOrderByTransactionTimeDesc(ProductionLine productionLine, ProductionLineActivityType type);


    ProductionLineActivity findTop1ByProductionLineAndUsernameAndTypeOrderByTransactionTimeDesc(
            ProductionLine productionLine, String username, ProductionLineActivityType type);
}

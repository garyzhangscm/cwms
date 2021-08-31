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

package com.garyzhangscm.cwms.dblink.controller;


import com.garyzhangscm.cwms.dblink.ResponseBodyWrapper;
import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.service.RecordCopyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/integration-data")
public class IntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(IntegrationDataController.class);

    @Autowired
    private RecordCopyService recordCopyService;



    @RequestMapping(value="/inventory-adjustment-confirmation", method = RequestMethod.PUT)
    public ResponseBodyWrapper addInventoryAdjustConfirmationData(
            @RequestBody DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {

        logger.debug("Get inventory adjust confirmation \n {}",
                dbBasedInventoryAdjustmentConfirmation);
        recordCopyService.saveIntegration(dbBasedInventoryAdjustmentConfirmation);
        return ResponseBodyWrapper.success("success");
    }


    @RequestMapping(value="/order-confirmation", method = RequestMethod.PUT)
    public ResponseBodyWrapper addOrderConfirmationData(
            @RequestBody DBBasedOrderConfirmation dbBasedOrderConfirmation) {

        logger.debug("Get order confirmation \n {}",
                dbBasedOrderConfirmation);
        recordCopyService.saveIntegration(dbBasedOrderConfirmation);
        return ResponseBodyWrapper.success("success");
    }
}

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


import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.service.DBBasedCustomerService;
import com.garyzhangscm.cwms.dblink.service.DBBasedItemService;
import com.garyzhangscm.cwms.dblink.service.DBBasedWorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;


@RestController
@RequestMapping(value = "/")
public class Welcome {


    private static final Logger logger = LoggerFactory.getLogger(Welcome.class);


    @Autowired
    DBBasedItemService dbBasedItemService;
    @Autowired
    DBBasedWorkOrderService dbBasedWorkOrderService;
    @Autowired
    DBBasedCustomerService dbBasedCustomerService;


    //
    // Client Related
    //
    @RequestMapping( method = RequestMethod.GET)
    public String welcome() {

        return "welcome";
    }


    @RequestMapping(value = "/items", method = RequestMethod.GET)
    public List<DBBasedItem> getDBBasedItem() {
        List<DBBasedItem> items = dbBasedItemService.findAll();
        logger.debug("Get {} items", items.size());
        return items;
    }

    @RequestMapping(value = "/work-orders", method = RequestMethod.GET)
    public int getDBBasedWorkOrders() {
        List<DBBasedWorkOrder> dbBasedWorkOrders =
                dbBasedWorkOrderService.findAll(null,
                        null, null, null, null,
                        null, null, 2);
        logger.debug("Get {} items", dbBasedWorkOrders.size());
        return dbBasedWorkOrders.size();
    }

    @RequestMapping(value = "/live-probe", method = RequestMethod.GET)
    public String testLive() {

        logger.debug("start test live by dbBasedItemService");
        dbBasedItemService.testLive();
        logger.debug("dbBasedItemService is live");

        logger.debug("start test live by dbBasedWorkOrderService");
        dbBasedWorkOrderService.testLive();
        logger.debug("dbBasedWorkOrderService is live");

        // logger.debug("start test live by dbBasedCustomerService");
        // dbBasedCustomerService.testLive();
        // logger.debug("dbBasedCustomerService is live");



        logger.debug("=====>>>>>   Everything is live");
        return "live";
    }

}

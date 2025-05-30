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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.exception.SystemFatalException;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/probe")
public class ProbeController {
    private static final Logger logger = LoggerFactory.getLogger(ProbeController.class);

    @Autowired
    InventoryService inventoryService;

    @RequestMapping(value = "/live", method = RequestMethod.GET)
    public ResponseBodyWrapper<String> liveProbe() {

        try {
            inventoryService.findById(-1L);
            return ResponseBodyWrapper.success("live probe success");

        }
        catch (ResourceNotFoundException ex) {

            return ResponseBodyWrapper.success("live probe success");
        }
        catch(Exception ex) {
            ex.printStackTrace();

            logger.debug("live probe fail due to some error: {}", ex.getMessage());


            throw SystemFatalException.raiseException("live probe fail");
        }

    }


}

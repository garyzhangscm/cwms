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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.QCConfiguration;
import com.garyzhangscm.cwms.inventory.model.QCRuleConfiguration;
import com.garyzhangscm.cwms.inventory.service.QCConfigurationService;
import com.garyzhangscm.cwms.inventory.service.QCRuleConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QCConfigurationController {
    @Autowired
    QCConfigurationService qcConfigurationService;

    @RequestMapping(value="/qc-configuration", method = RequestMethod.GET)
    public QCConfiguration findQCConfigurations(
            @RequestParam Long warehouseId) {
        return qcConfigurationService.getQCConfiguration(warehouseId);
    }

    @RequestMapping(value="/qc-configuration/{id}", method = RequestMethod.GET)
    public QCConfiguration findQCConfiguration(@PathVariable Long id) {
        return qcConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> deleteQCConfiguration(@PathVariable Long id) {

        qcConfigurationService.delete(id);
        return ResponseBodyWrapper.success("qc configuration with id " + id + " is deleted");
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/qc-configuration")
    public QCConfiguration addQCConfiguration(
            @RequestBody QCConfiguration qcConfiguration) {
        return qcConfigurationService.addQCConfiguration(qcConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-configuration/{id}", method = RequestMethod.POST)
    public QCConfiguration changeQCConfiguration(
            @PathVariable Long id, @RequestBody QCConfiguration qcConfiguration) {

        return qcConfigurationService.changeQCConfiguration(id, qcConfiguration);
    }

}

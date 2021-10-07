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
import com.garyzhangscm.cwms.inventory.model.QCRule;
import com.garyzhangscm.cwms.inventory.model.QCRuleConfiguration;
import com.garyzhangscm.cwms.inventory.service.QCRuleConfigurationService;
import com.garyzhangscm.cwms.inventory.service.QCRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QCRuleConfigurationController {
    @Autowired
    QCRuleConfigurationService qcRuleConfigurationService;

    @RequestMapping(value="/qc-rule-configuration", method = RequestMethod.GET)
    public List<QCRuleConfiguration> findAllQCRuleConfigurations(
            @RequestParam Long warehouseId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
            @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId) {
        return qcRuleConfigurationService.findAll(warehouseId, itemId, inventoryStatusId, supplierId);
    }

    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.GET)
    public QCRuleConfiguration findQCRuleConfiguration(@PathVariable Long id) {
        return qcRuleConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> deleteQCRuleConfiguration(@PathVariable Long id) {

        qcRuleConfigurationService.delete(id);
        return ResponseBodyWrapper.success("qc rule configuration with id " + id + " is deleted");
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/qc-rule-configuration")
    public QCRuleConfiguration addQCRuleConfiguration(
            @RequestBody QCRuleConfiguration qcRuleConfiguration) {
        return qcRuleConfigurationService.addQCRuleConfiguration(qcRuleConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.POST)
    public QCRuleConfiguration changeQCRuleConfiguration(
            @PathVariable Long id, @RequestBody QCRuleConfiguration qcRuleConfiguration) {

        return qcRuleConfigurationService.changeQCRuleConfiguration(id, qcRuleConfiguration);
    }

}

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

package com.garyzhangscm.cwms.workorder.controller;


import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCResult;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCRuleConfiguration;
import com.garyzhangscm.cwms.workorder.service.WorkOrderQCResultService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderQCRuleConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderQCRuleConfigurationController {
    @Autowired
    WorkOrderQCRuleConfigurationService workOrderQCRuleConfigurationService;

    @RequestMapping(value="/qc-rule-configuration", method = RequestMethod.GET)
    public List<WorkOrderQCRuleConfiguration> findAllQCRuleConfigurations(
            @RequestParam Long companyId,
            @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId ,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
            @RequestParam(name="btoOutboundOrderId", required = false, defaultValue = "") Long btoOutboundOrderId,
            @RequestParam(name="btoCustomerId", required = false, defaultValue = "") Long btoCustomerId,
            @RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="fromInventoryStatusId", required = false, defaultValue = "") Long fromInventoryStatusId,
            @RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name="loadDetail", required = false, defaultValue = "true") Boolean loadDetail) {
        return workOrderQCRuleConfigurationService.findAll(warehouseId, workOrderId, workOrderNumber, productionLineId,
                btoOutboundOrderId, btoCustomerId, itemFamilyId, itemId, fromInventoryStatusId, companyId,
                loadDetail);
    }

    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.GET)
    public WorkOrderQCRuleConfiguration findQCRuleConfiguration(@PathVariable Long id) {
        return workOrderQCRuleConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> deleteQCRuleConfiguration(@PathVariable Long id) {

        workOrderQCRuleConfigurationService.delete(id);
        return ResponseBodyWrapper.success("qc rule configuration with id " + id + " is deleted");
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/qc-rule-configuration")
    public WorkOrderQCRuleConfiguration addQCRuleConfiguration(
            @RequestBody WorkOrderQCRuleConfiguration qcRuleConfiguration) {
        return workOrderQCRuleConfigurationService.addQCRuleConfiguration(qcRuleConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rule-configuration/{id}", method = RequestMethod.POST)
    public WorkOrderQCRuleConfiguration changeQCRuleConfiguration(
            @PathVariable Long id, @RequestBody WorkOrderQCRuleConfiguration qcRuleConfiguration) {

        return workOrderQCRuleConfigurationService.changeQCRuleConfiguration(id, qcRuleConfiguration);
    }

    @RequestMapping(value="/qc-rule-configuration/qc-samples/{qcSampleId}/matched", method = RequestMethod.GET)
    public List<WorkOrderQCRuleConfiguration> findMatchedConfigurationForQCSample(
            @PathVariable Long qcSampleId
    ) {

        return workOrderQCRuleConfigurationService.findMatchedConfigurationForQCSample(qcSampleId);
    }


}

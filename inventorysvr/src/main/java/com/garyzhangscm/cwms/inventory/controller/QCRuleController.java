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
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.QCRule;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.ItemService;
import com.garyzhangscm.cwms.inventory.service.QCRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class QCRuleController {
    @Autowired
    QCRuleService qcRuleService;

    @RequestMapping(value="/qc-rules", method = RequestMethod.GET)
    public List<QCRule> findAllQCRules(@RequestParam Long warehouseId,
                                       @RequestParam(name="name", required = false, defaultValue = "") String name) {
        return qcRuleService.findAll(warehouseId, name);
    }

    @RequestMapping(value="/qc-rules/{id}", method = RequestMethod.GET)
    public QCRule findQCRule(@PathVariable Long id) {
        return qcRuleService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rules/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> deleteQCRule(@PathVariable Long id) {

        qcRuleService.delete(id);
        return ResponseBodyWrapper.success("qc rule with id " + id + " is deleted");
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/qc-rules")
    public QCRule addQCRule(@RequestBody QCRule qcRule) {
        return qcRuleService.addQCRule(qcRule);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-rules/{id}", method = RequestMethod.POST)
    public QCRule changeQCRule(@PathVariable Long id, @RequestBody QCRule qcRule) {

        return qcRuleService.changeQCRule(id, qcRule);
    }

}

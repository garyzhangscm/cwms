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

package com.garyzhangscm.cwms.common.controller;

import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Policy;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.PolicyService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PolicyController {
    @Autowired
    PolicyService policyService;

    @RequestMapping(value="/policies", method = RequestMethod.GET)
    public List<Policy> findAllPolicies(@RequestParam(name="key", required = false, value = "") String key) {
            return policyService.findAll(key);
    }

    @RequestMapping(value="/policies", method = RequestMethod.POST)
    public Policy addPolicy(@RequestBody Policy policy) {
        return policyService.save(policy);
    }

    @RequestMapping(value="/policies/{id}", method = RequestMethod.GET)
    public Policy findPolicy(@PathVariable Long id) {
        return policyService.findById(id);
    }

    @RequestMapping(value="/policies/{id}", method = RequestMethod.PUT)
    public Policy changePolicy(@PathVariable Long id,
                             @RequestBody Policy policy) {
        if (policy.getId() != null && policy.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; policy.getId(): " + policy.getId());
        }
        policy.setId(id);
        return policyService.saveOrUpdate(policy);
    }

    @RequestMapping(value="/policies/{id}", method = RequestMethod.DELETE)
    public Policy removePolicy(@PathVariable Long id) {
        Policy policy = policyService.findById(id);
        policyService.delete(id);
        return policy;
    }

}

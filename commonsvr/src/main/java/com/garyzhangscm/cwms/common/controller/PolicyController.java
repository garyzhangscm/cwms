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

import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Policy;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.PolicyService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class PolicyController {
    @Autowired
    PolicyService policyService;

    @RequestMapping(value="/policies", method = RequestMethod.GET)
    public List<Policy> findAllPolicies(@RequestParam Long warehouseId,
                                        @RequestParam(name="key", required = false, value = "") String key) {
            return policyService.findAll(warehouseId, key);
    }

    @BillableEndpoint
    @RequestMapping(value="/policies", method = RequestMethod.POST)
    public Policy addPolicy(@RequestBody Policy policy) {
        return policyService.saveOrUpdate(policy);
    }

    @RequestMapping(value="/policies/{id}", method = RequestMethod.GET)
    public Policy findPolicy(@PathVariable Long id) {
        return policyService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/policies/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InboundService_Policy", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Policy", allEntries = true),
            }
    )
    public Policy changePolicy(@PathVariable Long id,
                             @RequestBody Policy policy) {
        if (Objects.nonNull(policy.getId()) && !Objects.equals(policy.getId(),  id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; policy.getId(): " + policy.getId());
        }
        policy.setId(id);
        return policyService.saveOrUpdate(policy);
    }

    @BillableEndpoint
    @RequestMapping(value="/policies/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InboundService_Policy", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Policy", allEntries = true),
            }
    )
    public Policy removePolicy(@PathVariable Long id) {
        Policy policy = policyService.findById(id);
        policyService.delete(id);
        return policy;
    }

    @BillableEndpoint
    @RequestMapping(value="/policies", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InboundService_Policy", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Policy", allEntries = true),
            }
    )
    public ResponseBodyWrapper<String> removePolicy(@RequestParam Long warehouseId,
                                            @RequestParam(name="key", required = false, value = "") String key) {
        policyService.removePolicy(warehouseId, key);
        return ResponseBodyWrapper.success("policy returned for warehouse id: " + warehouseId + ", key: " + key);
    }

}

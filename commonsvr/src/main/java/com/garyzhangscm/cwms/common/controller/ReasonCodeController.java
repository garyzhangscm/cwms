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
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.ReasonCode;
import com.garyzhangscm.cwms.common.service.ReasonCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class ReasonCodeController {
    @Autowired
    ReasonCodeService reasonCodeService;

    @RequestMapping(value="/reason-codes", method = RequestMethod.GET)
    public List<ReasonCode> findAllReasonCodes(@RequestParam Long warehouseId,
                                               @RequestParam(value = "type", required = false, defaultValue = "") String type) {

        return reasonCodeService.findAll(warehouseId, type);

    }



    @RequestMapping(value="/reason-codes/{id}", method = RequestMethod.GET)
    public ReasonCode findReasonCode(@PathVariable Long id) {
        return reasonCodeService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/reason-codes", method = RequestMethod.POST)
    public ReasonCode addReasonCode(@RequestParam Long warehouseId,
                                    @RequestBody ReasonCode reasonCode) {
        return reasonCodeService.addReasonCode(reasonCode);
    }

    @BillableEndpoint
    @RequestMapping(value="/reason-codes/{id}", method = RequestMethod.PUT)
    public ReasonCode changeReasonCode(@RequestParam Long warehouseId,
                                       @PathVariable Long id, @RequestBody ReasonCode reasonCode) {
        if (Objects.nonNull(reasonCode.getId()) && !Objects.equals(reasonCode.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; reasonCode.getId(): " + reasonCode.getId());
        }
        return reasonCodeService.save(reasonCode);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/reason-codes/{id}")
    public ResponseBodyWrapper<String> deleteReasonCode(@RequestParam Long warehouseId,
                                                        @PathVariable Long id) {
        reasonCodeService.removeReasonCode(id);
        return ResponseBodyWrapper.success("reason code " + id + " is removed");
    }
}

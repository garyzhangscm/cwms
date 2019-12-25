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
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.ReasonCode;
import com.garyzhangscm.cwms.common.service.ClientService;
import com.garyzhangscm.cwms.common.service.ReasonCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReasonCodeController {
    @Autowired
    ReasonCodeService reasonCodeService;

    @RequestMapping(value="/reason-codes", method = RequestMethod.GET)
    public List<ReasonCode> findAllReasonCodes(@RequestParam(value = "type", required = false, defaultValue = "") String type) {
        if (type.isEmpty()) {
            return reasonCodeService.findAll();
        }
        else {
            return reasonCodeService.findByType(type);
        }

    }



    @RequestMapping(value="/reason-code/{id}", method = RequestMethod.GET)
    public ReasonCode findReasonCode(@PathVariable Long id) {
        return reasonCodeService.findById(id);
    }

    @RequestMapping(value="/reason-code", method = RequestMethod.POST)
    public ReasonCode addReasonCode(@RequestBody ReasonCode reasonCode) {
        return reasonCodeService.save(reasonCode);
    }

    @RequestMapping(value="/reason-code/{id}", method = RequestMethod.PUT)
    public ReasonCode changeReasonCode(@PathVariable Long id, @RequestBody ReasonCode reasonCode) {
        if (reasonCode.getId() != null && reasonCode.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return reasonCodeService.save(reasonCode);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/reason-code")
    public void deleteReasonCodes(@RequestParam(name = "client_ids", required = false, defaultValue = "") String reasonCodeIds) {
        reasonCodeService.delete(reasonCodeIds);
    }
}

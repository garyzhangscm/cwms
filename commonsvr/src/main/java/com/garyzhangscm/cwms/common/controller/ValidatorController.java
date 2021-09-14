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
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.SystemControlledNumber;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import com.garyzhangscm.cwms.common.service.SystemControlledNumberService;
import com.garyzhangscm.cwms.common.service.ValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class ValidatorController {
    @Autowired
    ValidateService validateService;

    @BillableEndpoint
    @RequestMapping(value="/validator/validate-new-number/{variable}/{value}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateNewNumber(@PathVariable String variable,
                                                         @PathVariable String value,
                                                         @RequestParam Long warehouseId,
                                                         @RequestParam Long companyId) {
        return ResponseBodyWrapper.success(
                validateService.validate(
                        companyId, warehouseId, variable,
                        ValidatorType.VALIDATE_VALUE_NON_EXISTS, value));

    }

    @BillableEndpoint
    @RequestMapping(value="/validator/validate-existing-number/{variable}/{value}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateExistingNumber(@PathVariable String variable,
                                                         @PathVariable String value,
                                                         @RequestParam Long warehouseId,
                                                         @RequestParam Long companyId) {

        return ResponseBodyWrapper.success(
                validateService.validate(
                        companyId, warehouseId, variable,
                        ValidatorType.VALIDATE_VALUE_EXISTS, value));

    }
}

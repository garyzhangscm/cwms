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

import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.Unit;
import com.garyzhangscm.cwms.common.service.CarrierService;
import com.garyzhangscm.cwms.common.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class UnitController {
    @Autowired
    private UnitService unitService;

    @RequestMapping(value="/units", method = RequestMethod.GET)
    public List<Unit> findAllUnits(@RequestParam Long warehouseId,
                                   @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                   @RequestParam(name = "type", required = false, defaultValue = "") String type) {
        return unitService.findAll(name, type);
    }


}

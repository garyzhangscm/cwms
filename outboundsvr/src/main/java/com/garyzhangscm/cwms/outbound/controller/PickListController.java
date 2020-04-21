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

package com.garyzhangscm.cwms.outbound.controller;


import com.garyzhangscm.cwms.outbound.model.PickList;
import com.garyzhangscm.cwms.outbound.service.PickListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PickListController {
    @Autowired
    PickListService pickListService;

    @RequestMapping(value="/pick-lists", method = RequestMethod.GET)
    public List<PickList> findAllPickLists(@RequestParam Long warehouseId,
                                           @RequestParam(name="number", required = false, defaultValue = "") String number) {
        return pickListService.findAll(warehouseId, number);
    }


    @RequestMapping(value="/pick-lists/{id}", method = RequestMethod.GET)
    public PickList findPickList(@PathVariable Long id) {
        return pickListService.findById(id);
    }

}

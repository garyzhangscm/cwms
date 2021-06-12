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
import com.garyzhangscm.cwms.workorder.model.BillOfMaterial;
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.service.BillOfMaterialService;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MouldController {
    @Autowired
    MouldService mouldService;

    @RequestMapping(value="/moulds", method = RequestMethod.GET)
    public List<Mould> findAllMoulds(@RequestParam Long warehouseId,
                                     @RequestParam(name="name", required = false, defaultValue = "") String name,
                                     @RequestParam(name="description", required = false, defaultValue = "") String description) {
        return mouldService.findAll(warehouseId, name, description);
    }



    @RequestMapping(value="/moulds", method = RequestMethod.POST)
    public Mould addMoulds(@RequestBody Mould mould) {
        return mouldService.addMould(mould);
    }


    @RequestMapping(value="/moulds/{id}", method = RequestMethod.GET)
    public Mould findMould(@PathVariable Long id) {

        return mouldService.findById(id);
    }

    @RequestMapping(value="/moulds/{id}", method = RequestMethod.PUT)
    public Mould changeMould(@PathVariable Long id,
                                               @RequestBody Mould mould){
        return mouldService.changeMould(id, mould);
    }

    @RequestMapping(value="/moulds/{id}", method = RequestMethod.DELETE)
    public void removeMould(@PathVariable Long id) {
        mouldService.delete(id);
    }


}

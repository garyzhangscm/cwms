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


import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.MaterialRequirementsPlanning;
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.service.MaterialRequirementsPlanningService;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MaterialRequirementsPlanningController {
    @Autowired
    MaterialRequirementsPlanningService materialRequirementsPlanningService;

    @RequestMapping(value="/material-requirements-planning", method = RequestMethod.GET)
    public List<MaterialRequirementsPlanning> findAllMRP(
            @RequestParam Long warehouseId,
            @RequestParam(name="name", required = false, defaultValue = "") String name,
            @RequestParam(name="description", required = false, defaultValue = "") String description,
            @RequestParam(name="mpsNumber", required = false, defaultValue = "") String mpsNumber) {
        return materialRequirementsPlanningService.findAll(warehouseId, name, mpsNumber, description);
    }



    @BillableEndpoint
    @RequestMapping(value="/material-requirements-planning", method = RequestMethod.POST)
    public MaterialRequirementsPlanning addMRP(@RequestBody MaterialRequirementsPlanning materialRequirementsPlanning) {
        return materialRequirementsPlanningService.addMRP(materialRequirementsPlanning);
    }


    @RequestMapping(value="/material-requirements-planning/{id}", method = RequestMethod.GET)
    public MaterialRequirementsPlanning findMRP(@PathVariable Long id) {

        return materialRequirementsPlanningService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/material-requirements-planning/{id}", method = RequestMethod.PUT)
    public MaterialRequirementsPlanning changeMRP(@PathVariable Long id,
                                               @RequestBody MaterialRequirementsPlanning materialRequirementsPlanning){
        return materialRequirementsPlanningService.changeMRP(id, materialRequirementsPlanning);
    }

    @BillableEndpoint
    @RequestMapping(value="/material-requirements-planning/{id}", method = RequestMethod.DELETE)
    public void removeMRP(@PathVariable Long id) {
        materialRequirementsPlanningService.delete(id);
    }


}

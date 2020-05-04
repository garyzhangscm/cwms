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


import com.garyzhangscm.cwms.outbound.model.GridDistributionWork;
import com.garyzhangscm.cwms.outbound.service.GridDistributionWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GridDistributionWorkController {
    @Autowired
    GridDistributionWorkService gridDistributionWorkService;

    @RequestMapping(value="/grid-distribution-work", method = RequestMethod.GET)
    public List<GridDistributionWork> findAllGridDistributionWorks(@RequestParam Long warehouseId,
                                                                   @RequestParam Long locationGroupId,
                                                                   @RequestParam String id) {
        return gridDistributionWorkService.getGridDistributionWork(warehouseId, locationGroupId, id);
    }

    @RequestMapping(value="/grid-distribution-work/confirm", method = RequestMethod.POST)
    public void confirmGridDistributionWork(@RequestParam Long warehouseId,
                                            @RequestParam String id,
                                            @RequestParam Long gridLocationConfigurationId) {
        gridDistributionWorkService.confirmGridDistributionWork(warehouseId, id, gridLocationConfigurationId);
    }

}

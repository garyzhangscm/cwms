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

import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.Stop;
import com.garyzhangscm.cwms.outbound.service.StopService;
import com.garyzhangscm.cwms.outbound.service.TrailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StopController {
    @Autowired
    StopService stopService;

    @RequestMapping(value="/stops", method = RequestMethod.GET)
    public List<Stop> findAllStops() {
        return stopService.findAll();
    }

    @BillableEndpoint
    @RequestMapping(value="/stops", method = RequestMethod.POST)
    public Stop addStop(@RequestBody Stop stop) {
        return stopService.save(stop);
    }


    @RequestMapping(value="/stops/{id}", method = RequestMethod.GET)
    public Stop findStop(@PathVariable Long id) {
        return stopService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/stops/{id}", method = RequestMethod.PUT)
    public Stop changeStop(@RequestBody Stop stop){
        return stopService.save(stop);
    }

    @BillableEndpoint
    @RequestMapping(value="/stops", method = RequestMethod.DELETE)
    public void removeStops(@RequestParam(name = "stop_ids", required = false, defaultValue = "") String stopIds) {
        stopService.delete(stopIds);
    }

}

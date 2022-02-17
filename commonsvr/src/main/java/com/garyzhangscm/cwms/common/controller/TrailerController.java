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

import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Location;
import com.garyzhangscm.cwms.common.model.Trailer;
import com.garyzhangscm.cwms.common.service.TrailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TrailerController {
    @Autowired
    TrailerService trailerService;


    @RequestMapping(value="/trailers", method = RequestMethod.GET)
    public List<Trailer> findAllTrailers(@RequestParam(name="number", required = false, defaultValue = "") String number) {
        return trailerService.findAll(number);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers", method = RequestMethod.POST)
    public Trailer addTrailer(@RequestBody Trailer trailer) {
        return trailerService.save(trailer);
    }


    @RequestMapping(value="/trailers/{id}", method = RequestMethod.GET)
    public Trailer findTrailer(@PathVariable Long id) {
        return trailerService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers/{id}", method = RequestMethod.PUT)
    public Trailer changeTrailer(@RequestBody Trailer trailer){
        return trailerService.save(trailer);
    }


    @BillableEndpoint
    @RequestMapping(value="/trailers/{id}/checkin", method = RequestMethod.POST)
    public Trailer checkinTrailer(@PathVariable Long id,
                                  @RequestBody Location dockLocation) {
        return trailerService.checkInTrailer(id, dockLocation);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers/{id}/dispatch", method = RequestMethod.POST)
    public Trailer checkinTrailer(@PathVariable Long id) {
        return trailerService.dispatchTrailer(id);
    }

}

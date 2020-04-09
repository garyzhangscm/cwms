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

package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.model.IntegrationClientData;
import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/integration-data")
public class IntegrationDataController {


    @Autowired
    private IntegrationDataService integrationDataService;


    @RequestMapping(value="/clients", method = RequestMethod.GET)
    public List<? extends IntegrationClientData> getIntegrationClientData() {

        return integrationDataService.getClientData();
    }

}

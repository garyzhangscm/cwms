/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.service.CompanyMenuService;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CompanyMenuController {



    @Autowired
    CompanyMenuService companyMenuService;

    @RequestMapping(value="/company-menus", method = RequestMethod.GET)
    public List<CompanyMenu> listAllCompanyMenus(@RequestParam Long companyId) {
        return companyMenuService.findAll(companyId);
    }


    @BillableEndpoint
    @RequestMapping(value="/company-menus/assign/{companyId}", method = RequestMethod.POST)
    public ResponseBodyWrapper processCompanyMenus(@PathVariable Long companyId,
                                                   @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedMenuIds,
                                                   @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedMenuIds) {

        companyMenuService.processCompanyMenus(companyId, assignedMenuIds, deassignedMenuIds);
        return ResponseBodyWrapper.success("success");
    }


}

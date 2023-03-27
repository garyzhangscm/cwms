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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.Permission;
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.service.PermissionService;
import com.garyzhangscm.cwms.resources.service.RFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PermissionController {

    @Autowired
    PermissionService permissionService;

    @RequestMapping(value="/permissions", method = RequestMethod.GET)
    public List<Permission> findAllPermissions(@RequestParam Long warehouseId,
                                               @RequestParam(name="menuIds", required = false, defaultValue = "") String menuIds) {
        return permissionService.findAll(null, null, menuIds);
    }


}

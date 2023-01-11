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

import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.service.CompanyMenuService;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuController {

    @Autowired
    MenuGroupService menuGroupService;
    @Autowired
    MenuService menuService;

    @Autowired
    CompanyMenuService companyMenuService;

    @RequestMapping(value="/menus", method = RequestMethod.GET)
    public List<MenuGroup> listAllMenus(@RequestParam Long companyId,
                                        @RequestParam(name = "username", required = false, defaultValue = "") String username) {
        return menuGroupService.getAccessibleMenus(companyId, username);
    }

    @RequestMapping(value="/menus/web", method = RequestMethod.GET)
    public List<MenuGroup> listAllWebMenus(@RequestParam Long companyId,
                                           @RequestParam(name = "username", required = false, defaultValue = "") String username) {
        return menuGroupService.getAccessibleMenus(companyId, username, MenuType.WEB);
    }

    @RequestMapping(value="/menus/mobile", method = RequestMethod.GET)
    public List<MenuGroup> listAllMobileMenus(@RequestParam Long companyId,
                                              @RequestParam(name = "username", required = false, defaultValue = "") String username) {
        return menuGroupService.getAccessibleMenus(companyId, username, MenuType.MOBILE);
    }


    @RequestMapping(value="/menu/{id}/enable", method = RequestMethod.POST)
    public Menu enableDisableMenu(@PathVariable Long id,
                                  @RequestParam Boolean enabled) {
        return menuService.enableDisableMenu(id, enabled);
    }

    @RequestMapping(value="/menus/company-accessible", method = RequestMethod.GET)
    public List<MenuGroup> getCompanyAccessibleMenu(@RequestParam Long companyId) {
        return menuGroupService.getCompanyAccessibleMenu(companyId);
    }






}

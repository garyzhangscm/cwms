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

package com.garyzhangscm.cwms.resources;

import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.MenuService;
import com.garyzhangscm.cwms.resources.service.MenuSubGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Component
public class MyApplicationRunner implements ApplicationRunner {
    // WE will init the menu when start the application

    @Autowired
    MenuGroupService menuGroupService;
    @Autowired
    MenuSubGroupService menuSubGroupService;
    @Autowired
    MenuService menuService;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Start to init the menus");
        menuGroupService.initTestData( null, "");
        menuSubGroupService.initTestData(null, "");
        menuService.initTestData(null, "");
    }
}

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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.*;
import com.garyzhangscm.cwms.resources.model.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InitTestDataService {


    private static final Logger logger = LoggerFactory.getLogger(InitTestDataService.class);
    UserService userService;
    RoleService roleService;
    UserRoleService userRoleService;
    RoleMenuService roleMenuService;
    WorkingTeamService workingTeamService;
    WorkingTeamUserService workingTeamUserService;
    ReportService reportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;



    /***
     * Menus will be init when the server is started
     * it is not based on any warehouse
     * and necessary when the user login, before
     * the user can ever load the test data
    MenuGroupService menuGroupService;
    MenuSubGroupService menuSubGroupService;
    MenuService menuService;
    ***/

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();



    CommonServiceRestemplateClient commonServiceRestemplateClient;

    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    InboundServiceRestemplateClient inboundServiceRestemplateClient;

    OutboundServiceRestemplateClient outboundServiceRestemplateClient;

    WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;

    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;




    List<InitiableServiceRestemplateClient> initiableServiceRestemplateClients = new LinkedList<>();
    @Autowired
    public InitTestDataService(CommonServiceRestemplateClient commonServiceRestemplateClient,
                               InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
                               LayoutServiceRestemplateClient layoutServiceRestemplateClient,
                               InboundServiceRestemplateClient inboundServiceRestemplateClient,
                               OutboundServiceRestemplateClient outboundServiceRestemplateClient,
                               WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient,
                               IntegrationServiceRestemplateClient integrationServiceRestemplateClient,
                               UserService userService,
                               RoleService roleService,
                               // MenuGroupService menuGroupService,
                               // MenuSubGroupService menuSubGroupService,
                               // MenuService menuService,
                               UserRoleService userRoleService,
                               RoleMenuService roleMenuService,
                               WorkingTeamService workingTeamService,
                               WorkingTeamUserService workingTeamUserService,
                               ReportService reportService) {

        // Add service from current server
        this.userService = userService;
        this.roleService = roleService;
        // this.menuGroupService = menuGroupService;
        // this.menuSubGroupService = menuSubGroupService;
        // this.menuService = menuService;
        this.userRoleService = userRoleService;
        this.roleMenuService = roleMenuService;
        this.workingTeamService = workingTeamService;
        this.workingTeamUserService = workingTeamUserService;
        this.reportService = reportService;


        initiableServices.put("User", userService);
        serviceNames.add("User");
        initiableServices.put("Role", roleService);
        serviceNames.add("Role");
        /****
        initiableServices.put("Menu Group", menuGroupService);
        serviceNames.add("Menu Group");
        initiableServices.put("Menu Sub Group", menuSubGroupService);
        serviceNames.add("Menu Sub Group");
        initiableServices.put("Menu", menuService);
        serviceNames.add("Menu");
         ****/
        initiableServices.put("User_Role", userRoleService);
        serviceNames.add("User_Role");
        initiableServices.put("Role_Menu_Access", roleMenuService);
        serviceNames.add("Role_Menu_Access");


        initiableServices.put("working_team", workingTeamService);
        serviceNames.add("working_team");
        initiableServices.put("working_team_user", workingTeamUserService);
        serviceNames.add("working_team_user");
        initiableServices.put("report", reportService);
        serviceNames.add("report");



        // Add service from other servers
        this.layoutServiceRestemplateClient  = layoutServiceRestemplateClient;
        this.commonServiceRestemplateClient = commonServiceRestemplateClient;
        this.inventoryServiceRestemplateClient = inventoryServiceRestemplateClient;
        this.inboundServiceRestemplateClient = inboundServiceRestemplateClient;
        this.outboundServiceRestemplateClient = outboundServiceRestemplateClient;
        this.workOrderServiceRestemplateClient = workOrderServiceRestemplateClient;
        this.integrationServiceRestemplateClient = integrationServiceRestemplateClient;

        initiableServiceRestemplateClients.add(layoutServiceRestemplateClient);
        initiableServiceRestemplateClients.add(commonServiceRestemplateClient);
        initiableServiceRestemplateClients.add(inventoryServiceRestemplateClient);
        initiableServiceRestemplateClients.add(inboundServiceRestemplateClient);
        initiableServiceRestemplateClients.add(outboundServiceRestemplateClient);
        initiableServiceRestemplateClients.add(workOrderServiceRestemplateClient);
        initiableServiceRestemplateClients.add(integrationServiceRestemplateClient);

    }

    public String[] getTestDataNames() {
        List<String> testDataNames = new ArrayList<>();
        testDataNames.addAll(serviceNames);

        for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {
            testDataNames.addAll(Arrays.asList(initiableServiceRestemplateClient.getTestDataNames()));
        }
        return testDataNames.toArray(new String[0]);
    }

    public void init(Long companyId, String warehouseName) {
        initAll(companyId, warehouseName);
    }
    public void init(Long companyId, String name, String warehouseName) {
        if (name.isEmpty() || name.equals("ALL")) {
            initAll(companyId, warehouseName);
        }
        else {
            if (initiableServices.containsKey(name)) {
                logger.debug("### Start to initial {}, with company ID: {}, warehouse Name: {}",
                        name, companyId, warehouseName);
                initiableServices.get(name).initTestData(companyId, warehouseName);
            }
            else {
                for (InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {
                    if (initiableServiceRestemplateClient.contains(name)) {
                        initiableServiceRestemplateClient.initTestData(companyId, name, warehouseName);
                    }
                }
            }
        }

    }
    private void initAll(Long companyId, String warehouseName) {

        serviceNames.forEach(serviceName -> init(companyId, serviceName,  warehouseName));

        for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {

            initiableServiceRestemplateClient.initTestData(companyId, warehouseName);
        }
    }

    public void clear(Long warehouseId) {

        // clear all data from resource service
        // which including menus / users / roles

        // We should remove from the last table , by taking the dependence into
        // consideration



        jdbcTemplate.execute("delete from user_role");
        logger.debug("user_role records removed!");


        jdbcTemplate.execute("delete from role_menu");
        logger.debug("role_menu records removed!");

        jdbcTemplate.execute("delete from working_team_user");
        logger.debug("working_team_user records removed!");
        jdbcTemplate.execute("delete from working_team");
        logger.debug("working_team records removed!");

        jdbcTemplate.execute("delete from role_menu");
        logger.debug("role_menu records removed!");

        // we will keep user!
        // jdbcTemplate.execute("delete from user_info");
        // logger.debug("user_info records removed!");

        jdbcTemplate.execute("delete from role");
        logger.debug("role records removed!");


        Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseById(
                warehouseId
        );
        if (Objects.nonNull(warehouse)) {

            jdbcTemplate.update("delete from report where warehouse_id = ?", new Object[] { warehouseId });
            logger.debug("report records for warehouse {}!", warehouse.getName());
            jdbcTemplate.update("delete from report where company_id = ?", new Object[] { warehouse.getCompany().getId() });
            logger.debug("report records for company {}!", warehouse.getCompany().getCode());

        }


        // we will keep menu info
        // jdbcTemplate.execute("delete from menu");
        // logger.debug("menu records removed!");
        // jdbcTemplate.execute("delete from menu_sub_group");
        // logger.debug("menu_sub_group records removed!");
        // jdbcTemplate.execute("delete from menu_group");
        // logger.debug("menu_group records removed!");
        /***
        for(int i = serviceNames.size() - 1; i >= 0; i--) {

            logger.debug("Start to clear {}", serviceNames.get(i));
            initiableServices.get(serviceNames.get(i)).deleteAll(warehouseName);
        }
         **/

        for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {

            logger.debug("=== Start to remove data by {} ===", initiableServiceRestemplateClient.getClass());
            initiableServiceRestemplateClient.clearData(warehouseId);
            logger.debug("=== End of remove data by {} ===", initiableServiceRestemplateClient.getClass());
        }

    }
}

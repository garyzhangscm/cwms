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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.MenuGroup;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.model.WorkingTeam;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.RoleService;
import com.garyzhangscm.cwms.resources.service.WorkingTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkingTeamController {

    @Autowired
    WorkingTeamService workingTeamService;

    @RequestMapping(value="/working-teams", method = RequestMethod.GET)
    public List<WorkingTeam> findAllWorkingTeams(@RequestParam Long companyId,
                                                 @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                 @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled) {
        return workingTeamService.findAll(companyId, name, enabled);
    }

    @RequestMapping(value="/working-teams", method = RequestMethod.PUT)
    public WorkingTeam addWorkingTeam(@RequestBody WorkingTeam workingTeam) {
        return workingTeamService.addWorkingTeam(workingTeam);
    }

    @RequestMapping(value="/working-teams/{id}", method = RequestMethod.GET)
    public WorkingTeam findWorkingTeam(@PathVariable Long id) {
        return workingTeamService.findById(id);
    }


    @RequestMapping(value="/working-teams/{id}/disable", method = RequestMethod.POST)
    public WorkingTeam disableWorkingTeam(@PathVariable Long id) {
        return workingTeamService.disableWorkingTeam(id);
    }
    @RequestMapping(value="/working-teams/{id}/enable", method = RequestMethod.POST)
    public WorkingTeam enableWorkingTeam(@PathVariable Long id) {
        return workingTeamService.enableWorkingTeam(id);
    }




    @RequestMapping(value="/working-teams/{id}/users", method = RequestMethod.POST)
    public ResponseBodyWrapper processUsers(@PathVariable Long id,
                                        @RequestParam(name = "assigned", required = false, defaultValue = "") String assignedUserIds,
                                        @RequestParam(name = "deassigned", required = false, defaultValue = "") String deassignedUserIds) {

        workingTeamService.processUsers(id, assignedUserIds, deassignedUserIds);
        return ResponseBodyWrapper.success("success");
    }




}

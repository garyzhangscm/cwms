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
import com.garyzhangscm.cwms.resources.model.Department;
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.service.DepartmentService;
import com.garyzhangscm.cwms.resources.service.RFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DepartmentController {

    @Autowired
    DepartmentService departmentService;

    @RequestMapping(value="/departments", method = RequestMethod.GET)
    public List<Department> findAllDepartments(@RequestParam Long companyId,
                                       @RequestParam(name="name", required = false, defaultValue = "") String name) {
        return departmentService.findAll(companyId, name);
    }


    @RequestMapping(value="/departments/{id}", method = RequestMethod.GET)
    public Department findDepartment(@PathVariable Long id) {
        return departmentService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/departments", method = RequestMethod.PUT)
    public Department addDepartment(@RequestBody Department department) {
        return departmentService.addDepartment(department);
    }

    @BillableEndpoint
    @RequestMapping(value="/departments/{id}", method = RequestMethod.POST)
    public Department changeDepartment(@PathVariable Long id,
                                       @RequestBody Department department) {
        return departmentService.changeDepartment(id, department);
    }

    @BillableEndpoint
    @RequestMapping(value="/departments/{id}", method = RequestMethod.DELETE)
    public Boolean removeDepartment(@PathVariable Long id) {
        departmentService.delete(id);
        return true;
    }

}
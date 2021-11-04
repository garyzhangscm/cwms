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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.Department;
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.model.RFCSVWrapper;
import com.garyzhangscm.cwms.resources.model.Warehouse;
import com.garyzhangscm.cwms.resources.repository.DepartmentRepository;
import com.garyzhangscm.cwms.resources.repository.RFRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DepartmentService  {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);
    @Autowired
    private DepartmentRepository departmentRepository;


    public Department findById(Long id) {
        Department department =  departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("department not found by id: " + id));
        return department;
    }

    public Department findByName(Long companyId, String name) {
        return departmentRepository.findByCompanyIdAndName(companyId, name);

    }

    public List<Department> findAll(Long companyId,
                                    String name) {

        return departmentRepository.findAll(
                (Root<Department> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (!StringUtils.isBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public Department save(Department department) {



        return departmentRepository.save(department);
    }

    public Department saveOrUpdate(Department department) {
        if (Objects.isNull(department.getId()) &&
                !Objects.isNull(findByName(department.getCompanyId(), department.getName()))) {
            department.setId(findByName(department.getCompanyId(), department.getName()).getId());
        }
        return save(department);
    }
    public Department addDepartment(Department department) {
        return saveOrUpdate(department);

    }

    public Department changeDepartment(Long id, Department department) {
        department.setId(id);
        return saveOrUpdate(department);

    }

    public void delete(Long id) {
        departmentRepository.deleteById(id);

    }
}

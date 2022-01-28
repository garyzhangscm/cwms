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

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.MaterialRequirementsPlanningRepository;
import com.garyzhangscm.cwms.workorder.repository.MouldRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Service
public class MaterialRequirementsPlanningService   {
    private static final Logger logger = LoggerFactory.getLogger(MaterialRequirementsPlanningService.class);

    @Autowired
    private MaterialRequirementsPlanningRepository materialRequirementsPlanningRepository;



    public MaterialRequirementsPlanning findById(Long id) {
        return materialRequirementsPlanningRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("MRP not found by id: " + id));
    }



    public List<MaterialRequirementsPlanning> findAll(Long warehouseId,
                                                      String number,
                                                      String mpsNumber,
                                                      String description) {
        return materialRequirementsPlanningRepository.findAll(
                (Root<MaterialRequirementsPlanning> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }

                    }

                    if (StringUtils.isNotBlank(description)) {
                        if (description.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("description"), description));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }

                    }
                    if (StringUtils.isNotBlank(mpsNumber)) {

                        Join<MaterialRequirementsPlanning, MasterProductionSchedule> joinMasterProductionSchedule
                                = root.join("masterProductionSchedule", JoinType.INNER);
                        if (mpsNumber.contains("%")) {
                            predicates.add(criteriaBuilder.like(joinMasterProductionSchedule.get("number"), mpsNumber));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinMasterProductionSchedule.get("number"), mpsNumber));
                        }

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "number")
        );

    }

    public MaterialRequirementsPlanning findByNumber(Long warehouseId, String number) {

        return materialRequirementsPlanningRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public MaterialRequirementsPlanning save(MaterialRequirementsPlanning materialRequirementsPlanning) {
        return materialRequirementsPlanningRepository.save(materialRequirementsPlanning);
    }

    public MaterialRequirementsPlanning saveOrUpdate(MaterialRequirementsPlanning materialRequirementsPlanning) {
        if (materialRequirementsPlanning.getId() == null &&
                findByNumber(materialRequirementsPlanning.getWarehouseId(), materialRequirementsPlanning.getNumber()) != null) {
            materialRequirementsPlanning.setId(
                    findByNumber(materialRequirementsPlanning.getWarehouseId(), materialRequirementsPlanning.getNumber()).getId());
        }
        return save(materialRequirementsPlanning);
    }


    public void delete(MaterialRequirementsPlanning materialRequirementsPlanning) {
        materialRequirementsPlanningRepository.delete(materialRequirementsPlanning);
    }

    public void delete(Long id) {
        materialRequirementsPlanningRepository.deleteById(id);
    }


    public MaterialRequirementsPlanning addMRP(MaterialRequirementsPlanning materialRequirementsPlanning) {
        return saveOrUpdate(materialRequirementsPlanning);
    }

    public MaterialRequirementsPlanning changeMRP(Long id, MaterialRequirementsPlanning materialRequirementsPlanning) {
        return saveOrUpdate(materialRequirementsPlanning);
    }
}

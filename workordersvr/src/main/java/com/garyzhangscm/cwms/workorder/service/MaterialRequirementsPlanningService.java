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

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.MaterialRequirementsPlanningRepository;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class MaterialRequirementsPlanningService   {
    private static final Logger logger = LoggerFactory.getLogger(MaterialRequirementsPlanningService.class);

    @Autowired
    private MaterialRequirementsPlanningRepository materialRequirementsPlanningRepository;

    @Autowired
    private ProductionLineService productionLineService;


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
                        if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }

                    }

                    if (StringUtils.isNotBlank(description)) {
                        if (description.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("description"), description.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }

                    }
                    if (StringUtils.isNotBlank(mpsNumber)) {

                        Join<MaterialRequirementsPlanning, MasterProductionSchedule> joinMasterProductionSchedule
                                = root.join("masterProductionSchedule", JoinType.INNER);
                        if (mpsNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinMasterProductionSchedule.get("number"), mpsNumber.replaceAll("\\*", "%")));
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
        //  make sure the number is passed in and not used before
        if (Strings.isBlank(materialRequirementsPlanning.getNumber())) {
            throw WorkOrderException.raiseException("Number is not passed in for the new MRP");
        }
        if (Objects.nonNull(
                findByNumber(
                        materialRequirementsPlanning.getWarehouseId(), materialRequirementsPlanning.getNumber()))) {

            throw WorkOrderException.raiseException("Number is not already used by existing MRP");
        }
        materialRequirementsPlanning.getMaterialRequirementsPlanningLines().forEach(
                materialRequirementsPlanningLine ->
                        materialRequirementsPlanningLine.setMaterialRequirementsPlanning(
                                materialRequirementsPlanning
                        )
        );

        // Save the MRP without production line first
        // since we are adding new MRP, the MRP doesn't have an ID yet.
        // MRP / Production line are many to many relationship so we need both
        // MRP and Production line having the ID so that the relationship can be
        // persist in the mrp_production_line table
        // So we will save the MRP without Production line,
        // then attach all the Production line to the saved MRP and persist the
        // relationship again
        List<ProductionLine> productionLines = materialRequirementsPlanning.getProductionLines();
        materialRequirementsPlanning.setProductionLines(new ArrayList<>());
        MaterialRequirementsPlanning newMRP = saveOrUpdate(materialRequirementsPlanning);

        productionLines.forEach(productionLine -> {
            ProductionLine newProductionLine = productionLineService.findById(productionLine.getId());
            newMRP.addProductionLine(newProductionLine);
        });

        return saveOrUpdate(newMRP);
    }

    public MaterialRequirementsPlanning changeMRP(Long id, MaterialRequirementsPlanning materialRequirementsPlanning) {
        materialRequirementsPlanning.getMaterialRequirementsPlanningLines().forEach(
                materialRequirementsPlanningLine ->
                        materialRequirementsPlanningLine.setMaterialRequirementsPlanning(
                                materialRequirementsPlanning
                        )
        );
        return saveOrUpdate(materialRequirementsPlanning);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        materialRequirementsPlanningRepository.processItemOverride(
                warehouseId, oldItemId, newItemId
        );
        materialRequirementsPlanningRepository.processItemOverrideFroLine(
                warehouseId, oldItemId, newItemId
        );
    }
}

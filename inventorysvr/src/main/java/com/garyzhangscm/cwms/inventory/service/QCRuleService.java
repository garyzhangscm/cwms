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

package com.garyzhangscm.cwms.inventory.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ItemException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemRepository;
import com.garyzhangscm.cwms.inventory.repository.QCRuleRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QCRuleService{
    private static final Logger logger = LoggerFactory.getLogger(QCRuleService.class);

    @Autowired
    private QCRuleRepository qcRuleRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public QCRule findById(Long id) {
        return qcRuleRepository.findById(id)
                 .orElseThrow(() -> ResourceNotFoundException.raiseException("QC Rule not found by id: " + id));

    }


    public List<QCRule> findAll(Long warehouseId,
                              String name ) {

        return qcRuleRepository.findAll(
            (Root<QCRule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (StringUtils.isNotBlank(name)) {
                    if (name.contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                }



                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

    }



    public QCRule save(QCRule qcRule) {
        return qcRuleRepository.save(qcRule);
    }


    public QCRule saveOrUpdate(QCRule qcRule) {
        if (qcRule.getId() == null && findByName(qcRule.getWarehouseId(), qcRule.getName()) != null) {
            qcRule.setId(findByName(qcRule.getWarehouseId(), qcRule.getName()).getId());
        }
        return save(qcRule);
    }

    private QCRule findByName(Long warehouseId, String name) {
        List<QCRule> qcRules = findAll(warehouseId, name);
        if (qcRules.size() == 1) {
            return qcRules.get(0);
        }
        return null;
    }

    public void delete(QCRule qcRule) {
        qcRuleRepository.delete(qcRule);
    }
    public void delete(Long id) {
        qcRuleRepository.deleteById(id);
    }


    public QCRule addQCRule(QCRule qcRule) {
        qcRule.getQcRuleItems().forEach(
                qcRuleItem -> qcRuleItem.setQcRule(qcRule)
        );
        return saveOrUpdate(qcRule);
    }

    public QCRule changeQCRule(Long id, QCRule qcRule) {
        qcRule.setId(id);
        return saveOrUpdate(qcRule);
    }
}

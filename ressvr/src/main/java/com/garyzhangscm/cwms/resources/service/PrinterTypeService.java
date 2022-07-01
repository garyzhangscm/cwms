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
import com.garyzhangscm.cwms.resources.model.PrinterType;
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.model.RFCSVWrapper;
import com.garyzhangscm.cwms.resources.model.Warehouse;
import com.garyzhangscm.cwms.resources.repository.PrinterTypeRepository;
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
public class PrinterTypeService  {
    private static final Logger logger = LoggerFactory.getLogger(PrinterTypeService.class);
    @Autowired
    private PrinterTypeRepository printerTypeRepository;


    public PrinterType findById(Long id) {
        PrinterType printerType =  printerTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("printer type not found by id: " + id));
        return printerType;
    }

    public List<PrinterType> findAll(Long companyId,
                                    String name) {

        return printerTypeRepository.findAll(
                (Root<PrinterType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (!StringUtils.isBlank(name)) {
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

    public PrinterType findByName(Long companyId, String name) {
        return printerTypeRepository.findByCompanyIdAndName(companyId, name);
    }

    public PrinterType save(PrinterType printerType) {
        return printerTypeRepository.save(printerType);
    }

    public PrinterType saveOrUpdate(PrinterType printerType) {
        if (Objects.isNull(printerType.getId()) &&
                !Objects.isNull(findByName(printerType.getCompanyId(), printerType.getName()))) {
            printerType.setId(findByName(printerType.getCompanyId(), printerType.getName()).getId());
        }
        return save(printerType);
    }




    public PrinterType addPrinterType(PrinterType printerType) {
        return saveOrUpdate(printerType);

    }

    public void delete(Long id) {
        printerTypeRepository.deleteById(id);

    }
}

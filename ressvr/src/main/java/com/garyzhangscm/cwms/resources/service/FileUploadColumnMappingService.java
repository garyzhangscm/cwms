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

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.FileUploadColumnMappingRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class FileUploadColumnMappingService{
    private static final Logger logger = LoggerFactory.getLogger(FileUploadColumnMappingService.class);
    @Autowired
    private FileUploadColumnMappingRepository fileUploadColumnMappingRepository;



    public FileUploadColumnMapping findById(Long id) {
        return fileUploadColumnMappingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("File upload column mapping not found by id: " + id));
    }

    public FileUploadColumnMapping save(FileUploadColumnMapping fileUploadColumnMapping) {
        return fileUploadColumnMappingRepository.save(fileUploadColumnMapping);
    }
    public FileUploadColumnMapping saveOrUpdate(FileUploadColumnMapping fileUploadColumnMapping) {
        if (Objects.isNull(fileUploadColumnMapping.getId()) &&
            Objects.nonNull(findByFileUploadTypeAndColumn(
                    fileUploadColumnMapping.getCompanyId(),
                    fileUploadColumnMapping.getWarehouseId(),
                    fileUploadColumnMapping.getType(),
                    fileUploadColumnMapping.getColumnName()
            ))) {
            fileUploadColumnMapping.setId(
                    findByFileUploadTypeAndColumn(
                            fileUploadColumnMapping.getCompanyId(),
                            fileUploadColumnMapping.getWarehouseId(),
                            fileUploadColumnMapping.getType(),
                            fileUploadColumnMapping.getColumnName()
                    ).getId()
            );
        }
        return save(fileUploadColumnMapping);
    }
    public FileUploadColumnMapping findByFileUploadTypeAndColumn(Long companyId, Long warehouseId, String type, String columnName) {

        return fileUploadColumnMappingRepository.findByCompanyIdAndWarehouseIdAndTypeAndColumnName(
                companyId, warehouseId, type, columnName
        );
    }

    public List<FileUploadColumnMapping> findAll(Long companyId, Long warehouseId, String type, String columnName) {
        return fileUploadColumnMappingRepository.findAll(
                (Root<FileUploadColumnMapping> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

                    if (Strings.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), type));

                    }
                    if (Strings.isNotBlank(columnName)) {

                        predicates.add(criteriaBuilder.equal(root.get("columnName"), columnName));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public FileUploadColumnMapping addFileUploadColumnMapping(FileUploadColumnMapping fileUploadColumnMapping) {
        return saveOrUpdate(fileUploadColumnMapping);
    }
    public FileUploadColumnMapping changeFileUploadColumnMapping(Long id, FileUploadColumnMapping fileUploadColumnMapping) {
        fileUploadColumnMapping.setId(id);
        return saveOrUpdate(fileUploadColumnMapping);
    }
}

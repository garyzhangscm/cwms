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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.MouldRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderQCSampleRepository;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderQCSampleService   {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderQCSampleService.class);

    @Autowired
    private WorkOrderQCSampleRepository workOrderQCSampleRepository;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;
    @Autowired
    private FileService fileService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.mould:mould}")
    String testDataFile;

    @Value("${workorder.qc.sampleImageFolder}")
    private String qcSampleImageFolder;

    public WorkOrderQCSample findById(Long id) {
        return findById(id, true);
    }

    public WorkOrderQCSample findById(Long id, boolean loadDetail) {
        WorkOrderQCSample workOrderQCSample =
                workOrderQCSampleRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("work order qc sample not found by id: " + id));
        if (loadDetail) {
            loadAttribute(workOrderQCSample);
        }
        return workOrderQCSample;
    }


    private void loadAttribute(List<WorkOrderQCSample> workOrderQCSamples) {

        workOrderQCSamples.forEach(
                workOrderQCSample -> loadAttribute(workOrderQCSample)
        );
    }

    private void loadAttribute(WorkOrderQCSample workOrderQCSample) {
        workOrderQCSample.getProductionLineAssignment().setWorkOrderId(
                workOrderQCSample.getProductionLineAssignment().getWorkOrder().getId()
        );
    }


    public List<WorkOrderQCSample> findAll(Long warehouseId, String number,
                                           Long productionLineAssignmentId) {

        return findAll(warehouseId, number, productionLineAssignmentId, true);
    }

    public List<WorkOrderQCSample> findAll(Long warehouseId, String number,
                                           Long productionLineAssignmentId,
                                           boolean loadDetail) {
        List<WorkOrderQCSample> workOrderQCSamples =
                workOrderQCSampleRepository.findAll(
                (Root<WorkOrderQCSample> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (Objects.nonNull(productionLineAssignmentId)) {

                        Join<WorkOrderQCSample, ProductionLineAssignment> joinProductionLineAssignment
                                = root.join("productionLineAssignment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLineAssignment.get("id"), productionLineAssignmentId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "number")
        );
        if (workOrderQCSamples.size() > 0 && loadDetail) {
            loadAttribute(workOrderQCSamples);
        }
        return  workOrderQCSamples;

    }

    
    public WorkOrderQCSample save(WorkOrderQCSample workOrderQCSample) {
        return workOrderQCSampleRepository.save(workOrderQCSample);
    }

    public WorkOrderQCSample saveOrUpdate(WorkOrderQCSample workOrderQCSample) {
        if (workOrderQCSample.getId() == null &&
                findByNumber(workOrderQCSample.getWarehouseId(), workOrderQCSample.getNumber()) != null) {
            workOrderQCSample.setId(
                    findByNumber(workOrderQCSample.getWarehouseId(), workOrderQCSample.getNumber()).getId());
        }
        return save(workOrderQCSample);
    }

    private WorkOrderQCSample findByNumber(Long warehouseId, String number) {
        return workOrderQCSampleRepository.findByWarehouseIdAndNumber(
                warehouseId, number
        );
    }


    public void delete(WorkOrderQCSample workOrderQCSample) {
        workOrderQCSampleRepository.delete(workOrderQCSample);
    }

    public void delete(Long id) {
        workOrderQCSampleRepository.deleteById(id);
    }




    public WorkOrderQCSample addWorkOrderQCSample(WorkOrderQCSample workOrderQCSample) {
        return saveOrUpdate(workOrderQCSample);
    }

    public WorkOrderQCSample changeWorkOrderQCSample(Long id, WorkOrderQCSample workOrderQCSample) {
        return saveOrUpdate(workOrderQCSample);
    }

    public File getWorkOrderQCSampleImage(Long warehouseId,
                                          Long productionLineAssignmentId, String fileName) {

        // make sure the user is calling the function with the right data.

        ProductionLineAssignment productionLineAssignment =
                productionLineAssignmentService.findById(productionLineAssignmentId);
        if (!warehouseId.equals(productionLineAssignment.getWorkOrder().getWarehouseId())) {
            throw WorkOrderException.raiseException("Can't get image for the work order with incorrect parameters");
        }

        String fileUrl = getWorkOrderQCSampleImageFolder(productionLineAssignmentId) + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    /**
     * Get the QC sample image folder. it will be in the predefined folder
     * and a subfolder defined by the production line assignment id
     * @param productionLineAssignmentId
     * @return
     */
    private String getWorkOrderQCSampleImageFolder(Long productionLineAssignmentId) {
        return qcSampleImageFolder + "/" + productionLineAssignmentId + "/";
    }

    public String uploadQCSampleImage(Long productionLineAssignmentId, MultipartFile file) throws IOException {


        String filePath = getWorkOrderQCSampleImageFolder(productionLineAssignmentId);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();

    }

    @Transactional
    public void removeQCSamples(ProductionLineAssignment productionLineAssignment) {
        // let's see if we have any qc samples for this production line assignment
        List<WorkOrderQCSample> workOrderQCSamples = findAll(
                productionLineAssignment.getWorkOrder().getWarehouseId(),
                null,
                productionLineAssignment.getId(),
        false);
        workOrderQCSamples.forEach(
                workOrderQCSample -> removeQCSample(workOrderQCSample)
        );
    }

    @Transactional
    private void removeQCSample(WorkOrderQCSample workOrderQCSample) {
        // remove all files

        String filePath = getWorkOrderQCSampleImageFolder(workOrderQCSample.getProductionLineAssignment().getId());
        logger.debug("start to remove qc samples from folder {}",
                filePath);
        fileService.deleteDirectory(new File(filePath));

        // remove the qc sample record
        delete(workOrderQCSample);

    }

    public void removeQCSample(Long id) {
        WorkOrderQCSample workOrderQCSample = findById(id);
        removeQCSample(workOrderQCSample);
    }
}

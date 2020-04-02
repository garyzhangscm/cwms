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

package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import com.garyzhangscm.cwms.outbound.repository.TrailerTemplateRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class TrailerTemplateService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerTemplateService.class);

    @Autowired
    private TrailerTemplateRepository trailerTemplateRepository;

    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.trailer-templates:trailer-templates}")
    String testDataFile;

    public TrailerTemplate findById(Long id) {
        return trailerTemplateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer template not found by id: " + id));
    }


    public List<TrailerTemplate> findAll(String number, Boolean enabled) {
        return trailerTemplateRepository.findAll(
                (Root<TrailerTemplate> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (!StringUtils.isBlank(number)) {

                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }
                    if (enabled != null) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }




    public TrailerTemplate save(TrailerTemplate trailerTemplate) {
        return trailerTemplateRepository.save(trailerTemplate);
    }

    public TrailerTemplate saveOrUpdate(TrailerTemplate trailerTemplate) {
        // Make sure we only have one enabled trailer template for each trailer number
        if (trailerTemplate.getId() == null) {
            List<TrailerTemplate> enabledTrailerTemplates = findAll(trailerTemplate.getNumber(), true);
            if (enabledTrailerTemplates.size() > 0) {
                trailerTemplate.setId(enabledTrailerTemplates.get(0).getId());
            }
        }
        return save(trailerTemplate);
    }


    public void delete(TrailerTemplate trailerTemplate) {
        trailerTemplateRepository.delete(trailerTemplate);
    }

    public void delete(Long id) {
        trailerTemplateRepository.deleteById(id);
    }

    public void delete(String trailerTemplateIds) {
        if (!trailerTemplateIds.isEmpty()) {
            long[] trailerTemplateIdArray = Arrays.asList(trailerTemplateIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : trailerTemplateIdArray) {
                delete(id);
            }
        }
    }


    public List<TrailerTemplate> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("number").
                addColumn("licensePlateNumber").
                addColumn("size").
                addColumn("type").
                addColumn("driverFirstName").
                addColumn("driverLastName").
                addColumn("driverPhone").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, TrailerTemplate.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<TrailerTemplate> trailerTemplates = loadData(inputStream);
            trailerTemplates.stream().forEach(trailerTemplate -> saveOrUpdate(trailerTemplate));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private TrailerTemplate convertFromWrapper(TrailerTemplateCSVWrapper trailerTemplateCSVWrapper) {

        TrailerTemplate trailerTemplate = new TrailerTemplate();
        trailerTemplate.setDriverFirstName(trailerTemplateCSVWrapper.getDriverFirstName());
        trailerTemplate.setDriverLastName(trailerTemplateCSVWrapper.getDriverLastName());
        trailerTemplate.setDriverPhone(trailerTemplateCSVWrapper.getDriverPhone());
        trailerTemplate.setEnabled(trailerTemplateCSVWrapper.getEnabled());
        trailerTemplate.setLicensePlateNumber(trailerTemplateCSVWrapper.getLicensePlateNumber());
        trailerTemplate.setNumber(trailerTemplateCSVWrapper.getNumber());
        trailerTemplate.setSize(trailerTemplateCSVWrapper.getSize());
        trailerTemplate.setType(TrailerType.valueOf(trailerTemplateCSVWrapper.getType()));

        if (!StringUtils.isBlank(trailerTemplateCSVWrapper.getWarehouse())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(trailerTemplateCSVWrapper.getWarehouse());
            if (warehouse != null) {
                trailerTemplate.setWarehouseId(warehouse.getId());
            }
        }


        return trailerTemplate;
    }


}

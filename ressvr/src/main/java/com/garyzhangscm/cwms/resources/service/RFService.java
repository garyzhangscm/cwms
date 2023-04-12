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
import com.garyzhangscm.cwms.resources.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.GenericException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RFRepository;
import com.garyzhangscm.cwms.resources.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RFService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(RFService.class);
    @Autowired
    private RFRepository rfRepository;

    @Autowired
    private FileService fileService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    @Value("${fileupload.test-data.rfs:rfs}")
    String testDataFile;




    public RF findById(Long id) {
        RF rf =  rfRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("rf not found by id: " + id));
        return rf;
    }

    public RF findByRFCode(Long warehouseId, String rfCode) {
        return rfRepository.findByWarehouseIdAndRfCode(warehouseId, rfCode);

    }

    public List<RF> findAll(Long warehouseId) {

        return findAll(warehouseId, null);
    }

    public List<RF> findAll(Long warehouseId,
                              String rfCode) {

        return rfRepository.findAll(
                (Root<RF> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(rfCode)) {
                        predicates.add(criteriaBuilder.equal(root.get("rfCode"), rfCode));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public RF save(RF rf) {
        // before we save the RF, let's create a location for it
        logger.debug("Will create the location for the rf {} / {}",
                rf.getWarehouseId(), rf.getRfCode());
        layoutServiceRestemplateClient.createRFLocation(
                rf.getWarehouseId(),
                rf.getRfCode()
        );



        return rfRepository.save(rf);
    }

    public RF saveOrUpdate(RF rf) {
        if (Objects.isNull(rf.getId()) &&
                !Objects.isNull(findByRFCode(rf.getWarehouseId(), rf.getRfCode()))) {
            rf.setId(findByRFCode(rf.getWarehouseId(), rf.getRfCode()).getId());
        }
        return save(rf);
    }


    public List<RFCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("rfCode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, RFCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<RFCSVWrapper> rfCSVWrappers = loadData(inputStream);
            rfCSVWrappers.stream().forEach(rfCSVWrapper -> saveOrUpdate(convertFromWrapper(rfCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }



    private RF convertFromWrapper(RFCSVWrapper rfCSVWrapper) {

        RF rf = new RF();



        Warehouse warehouse =
                layoutServiceRestemplateClient.getWarehouseByName(
                        rfCSVWrapper.getCompany(),
                        rfCSVWrapper.getWarehouse());
        rf.setWarehouseId(warehouse.getId());

        rf.setRfCode(rfCSVWrapper.getRfCode());


        return rf;
    }


    public Boolean validateRFCode(Long warehouseId, String rfCode) {
        return Objects.nonNull(findByRFCode(warehouseId, rfCode));
    }

    public RF addRF(RF rf) {
        RF newRF = saveOrUpdate(rf);
        // let's check if we already have a location for this RF
        // we will always assume there's only one RF location group
        // as there's no reason to have 2 RF location groups by current
        // infrastructure
        layoutServiceRestemplateClient.createRFLocation(
                rf.getWarehouseId(),
                rf.getRfCode()
        );
        return newRF;

    }

    public void delete(Long id) {
        RF rf = findById(id);
        rfRepository.deleteById(id);

        layoutServiceRestemplateClient.removeRFLocation(
                rf.getWarehouseId(),
                rf.getRfCode()
        );

    }

    public void resetCurrentLocation(Long warehouseId, String rfCode, Long locationId) {
        RF rf = findByRFCode(warehouseId, rfCode);
        rf.setCurrentLocationId(locationId);

        saveOrUpdate(rf);
    }
}

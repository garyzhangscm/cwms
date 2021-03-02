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

package com.garyzhangscm.cwms.common.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.CarrierRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class CarrierService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(CarrierService.class);

    @Autowired
    private CarrierRepository carrierRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.carriers:carriers}")
    String testDataFile;

    public Carrier findById(Long id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carrier not found by id: " + id));
    }

    public List<Carrier> findAll(Long warehouseId,
                                 String name) {

        return carrierRepository.findAll(
                (Root<Carrier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public Carrier findByName(Long warehouseId, String name){
        return carrierRepository.findByName( warehouseId, name);
    }

    @Transactional
    public Carrier save(Carrier carrier) {
        return carrierRepository.save(carrier);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Carrier saveOrUpdate(Carrier carrier) {
        if (carrier.getId() == null &&
                findByName( carrier.getWarehouseId(),
                        carrier.getName()) != null) {
            carrier.setId(findByName( carrier.getWarehouseId(),
                    carrier.getName()).getId());
        }
        return save(carrier);
    }

    @Transactional
    public void delete(Carrier carrier) {
        carrierRepository.delete(carrier);
    }
    @Transactional
    public void delete(Long id) {
        carrierRepository.deleteById(id);
    }

    @Transactional
    public void delete(String carrierIds) {
        // remove a list of location groups based upon the id passed in
        if (!carrierIds.isEmpty()) {
            long[] carrierIdArray = Arrays.asList(carrierIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : carrierIdArray) {
                delete(id);
            }
        }
    }

    @Transactional
    public List<CarrierCSVWrapper> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    @Transactional
    public List<CarrierCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(file, schema, CarrierCSVWrapper.class);
    }

    @Transactional
    public List<CarrierCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, CarrierCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";


            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<CarrierCSVWrapper> carrierCSVWrappers = loadData(inputStream);
            carrierCSVWrappers.stream().forEach(carrierCSVWrapper -> saveOrUpdate(convertFromWrapper(carrierCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Carrier convertFromWrapper(CarrierCSVWrapper carrierCSVWrapper) {
        Carrier carrier = new Carrier();

        BeanUtils.copyProperties(carrierCSVWrapper, carrier);

        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                carrierCSVWrapper.getCompany(), carrierCSVWrapper.getWarehouse()
        );
        carrier.setWarehouseId(warehouse.getId());
        return carrier;

    }


}

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
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialRepository;
import com.garyzhangscm.cwms.workorder.repository.MouldRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
public class MouldService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(MouldService.class);

    @Autowired
    private MouldRepository mouldRepository;
    @Autowired
    private FileService fileService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.mould:mould}")
    String testDataFile;

    public Mould findById(Long id) {
        return mouldRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("mould not found by id: " + id));
    }



    public List<Mould> findAll(Long warehouseId, String name, String description) {
        return mouldRepository.findAll(
                (Root<Mould> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name));

                    }

                    if (StringUtils.isNotBlank(description)) {
                        predicates.add(criteriaBuilder.like(root.get("description"), description));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public Mould findByName(Long warehouseId, String name) {

        return mouldRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public Mould save(Mould mould) {
        return mouldRepository.save(mould);
    }

    public Mould saveOrUpdate(Mould mould) {
        if (mould.getId() == null &&
                findByName(mould.getWarehouseId(), mould.getName()) != null) {
            mould.setId(
                    findByName(mould.getWarehouseId(), mould.getName()).getId());
        }
        return save(mould);
    }


    public void delete(Mould mould) {
        mouldRepository.delete(mould);
    }

    public void delete(Long id) {
        mouldRepository.deleteById(id);
    }


    public List<MouldCVSWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MouldCVSWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MouldCVSWrapper> mouldCVSWrappers = loadData(inputStream);
            mouldCVSWrappers.stream().forEach(mouldCVSWrapper -> saveOrUpdate(convertFromWrapper(mouldCVSWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Mould convertFromWrapper(MouldCVSWrapper mouldCVSWrapper) {

        Mould mould = new Mould();
        mould.setName(mouldCVSWrapper.getName());
        mould.setDescription(mouldCVSWrapper.getDescription());

        logger.debug("Start to get warehouse: {}", mouldCVSWrapper.getWarehouse());
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                mouldCVSWrapper.getCompany(),
                mouldCVSWrapper.getWarehouse()
        );

        mould.setWarehouseId(warehouse.getId());

        return mould;
    }


    public Mould addMould(Mould mould) {
        return saveOrUpdate(mould);
    }

    public Mould changeMould(Long id, Mould mould) {
        return saveOrUpdate(mould);
    }
}

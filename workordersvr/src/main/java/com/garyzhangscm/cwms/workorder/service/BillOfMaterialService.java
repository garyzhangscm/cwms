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
public class BillOfMaterialService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(BillOfMaterialService.class);

    @Autowired
    private BillOfMaterialRepository billOfMaterialRepository;
    @Autowired
    private BillOfMaterialLineService billOfMaterialLineService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.bill-of-material:bill-of-material}")
    String testDataFile;

    public BillOfMaterial findById(Long id, boolean loadDetails) {
        BillOfMaterial billOfMaterial = billOfMaterialRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bill of material not found by id: " + id));
        if (loadDetails) {
            loadAttribute(billOfMaterial);
        }
        return billOfMaterial;
    }

    public BillOfMaterial findById(Long id) {
        return findById(id, true);
    }


    public List<BillOfMaterial> findAll(Long warehouseId, String number, String itemName, boolean loadDetails) {
        List<BillOfMaterial> billOfMaterials =  billOfMaterialRepository.findAll(
                (Root<BillOfMaterial> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);
                        if (item != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                        }
                        else {
                            // The client passed in an invalid item name, let's return nothing
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        if (billOfMaterials.size() > 0 && loadDetails) {
            loadAttribute(billOfMaterials);
        }
        return billOfMaterials;
    }

    public List<BillOfMaterial> findAll(Long warehouseId, String number, String itemName) {
        return findAll(warehouseId, number, itemName,true);
    }


    public BillOfMaterial findByNumber(Long warehouseId, String number, boolean loadDetails) {
        BillOfMaterial billOfMaterial = billOfMaterialRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (billOfMaterial != null && loadDetails) {
            loadAttribute(billOfMaterial);
        }
        return billOfMaterial;
    }

    public BillOfMaterial findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }


    public void loadAttribute(List<BillOfMaterial> billOfMaterials) {
        for (BillOfMaterial billOfMaterial : billOfMaterials) {
            loadAttribute(billOfMaterial);
        }
    }

    public void loadAttribute(BillOfMaterial billOfMaterial) {
        // Load the details for client and supplier informaiton
        if (billOfMaterial.getItemId() != null && billOfMaterial.getItem() == null) {
            billOfMaterial.setItem(inventoryServiceRestemplateClient.getItemById(billOfMaterial.getItemId()));
        }

        // Load the item and inventory status information for each lines
        billOfMaterial.getBillOfMaterialLines()
                .forEach(billOfMaterialLine -> billOfMaterialLineService.loadAttribute(billOfMaterialLine));

    }


    public BillOfMaterial save(BillOfMaterial billOfMaterial) {
        BillOfMaterial newBillOfMaterial = billOfMaterialRepository.save(billOfMaterial);
        loadAttribute(newBillOfMaterial);
        return newBillOfMaterial;
    }

    public BillOfMaterial saveOrUpdate(BillOfMaterial billOfMaterial) {
        if (billOfMaterial.getId() == null &&
                findByNumber(billOfMaterial.getWarehouseId(), billOfMaterial.getNumber()) != null) {
            billOfMaterial.setId(
                    findByNumber(billOfMaterial.getWarehouseId(), billOfMaterial.getNumber()).getId());
        }
        return save(billOfMaterial);
    }


    public void delete(BillOfMaterial billOfMaterial) {
        billOfMaterialRepository.delete(billOfMaterial);
    }

    public void delete(Long id) {
        billOfMaterialRepository.deleteById(id);
    }

    public void delete(String billOfMaterialIds) {
        if (!billOfMaterialIds.isEmpty()) {
            long[] billOfMaterialIdArray = Arrays.asList(billOfMaterialIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : billOfMaterialIdArray) {
                delete(id);
            }
        }
    }

    public List<BillOfMaterialCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("number").
                addColumn("warehouse").
                addColumn("item").
                addColumn("expectedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, BillOfMaterialCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName  = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<BillOfMaterialCSVWrapper> billOfMaterialCSVWrappers = loadData(inputStream);
            billOfMaterialCSVWrappers.stream().forEach(billOfMaterialCSVWrapper -> saveOrUpdate(convertFromWrapper(billOfMaterialCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private BillOfMaterial convertFromWrapper(BillOfMaterialCSVWrapper billOfMaterialCSVWrapper) {

        BillOfMaterial billOfMaterial = new BillOfMaterial();
        billOfMaterial.setNumber(billOfMaterialCSVWrapper.getNumber());
        billOfMaterial.setExpectedQuantity(billOfMaterialCSVWrapper.getExpectedQuantity());

        logger.debug("Start to get warehouse: {}", billOfMaterialCSVWrapper.getWarehouse());
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                billOfMaterialCSVWrapper.getWarehouse()
        );
        logger.debug("warehouse is null? {}", (warehouse == null));
        billOfMaterial.setWarehouseId(warehouse.getId());
        logger.debug("Start to get item: {}", billOfMaterialCSVWrapper.getItem());
        billOfMaterial.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), billOfMaterialCSVWrapper.getItem()).getId()
        );

        return billOfMaterial;
    }


}

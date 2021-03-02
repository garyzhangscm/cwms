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
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialByProductRepository;
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


@Service
public class BillOfMaterialByProductService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(BillOfMaterialByProductService.class);

    @Autowired
    private BillOfMaterialByProductRepository billOfMaterialByProductRepository;
    @Autowired
    private BillOfMaterialService billOfMaterialService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.bill-of-material-by-product:bill-of-material-by-product}")
    String testDataFile;

    public BillOfMaterialByProduct findById(Long id, boolean loadDetails) {
        BillOfMaterialByProduct billOfMaterialByProduct = billOfMaterialByProductRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bill of material line not found by id: " + id));
        if (loadDetails) {
            loadAttribute(billOfMaterialByProduct);
        }
        return billOfMaterialByProduct;
    }

    public BillOfMaterialByProduct findById(Long id) {
        return findById(id, true);
    }


    public List<BillOfMaterialByProduct> findAll(boolean loadDetails) {
        List<BillOfMaterialByProduct> billOfMaterialByProducts = billOfMaterialByProductRepository.findAll();

        if (billOfMaterialByProducts.size() > 0 && loadDetails) {
            loadAttribute(billOfMaterialByProducts);
        }
        return billOfMaterialByProducts;
    }

    public List<BillOfMaterialByProduct> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<BillOfMaterialByProduct> billOfMaterialByProducts) {
        for (BillOfMaterialByProduct billOfMaterialByProduct : billOfMaterialByProducts) {
            loadAttribute(billOfMaterialByProduct);
        }
    }

    public void loadAttribute(BillOfMaterialByProduct billOfMaterialByProduct) {

        if (billOfMaterialByProduct.getItemId() != null && billOfMaterialByProduct.getItem() == null) {
            billOfMaterialByProduct.setItem(inventoryServiceRestemplateClient.getItemById(billOfMaterialByProduct.getItemId()));
        }
        if (billOfMaterialByProduct.getInventoryStatusId() != null && billOfMaterialByProduct.getInventoryStatus() == null) {
            billOfMaterialByProduct.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(billOfMaterialByProduct.getInventoryStatusId()));
        }

    }



    public BillOfMaterialByProduct save(BillOfMaterialByProduct billOfMaterialByProduct) {
        BillOfMaterialByProduct newBillOfMaterialByProduct = billOfMaterialByProductRepository.save(billOfMaterialByProduct);
        loadAttribute(billOfMaterialByProduct);
        return newBillOfMaterialByProduct;
    }

    public void delete(BillOfMaterialByProduct billOfMaterialByProduct) {
        billOfMaterialByProductRepository.delete(billOfMaterialByProduct);
    }

    public void delete(Long id) {
        billOfMaterialByProductRepository.deleteById(id);
    }



    public List<BillOfMaterialByProductCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("billOfMaterial").
                addColumn("item").
                addColumn("inventoryStatus").
                addColumn("expectedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, BillOfMaterialByProductCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<BillOfMaterialByProductCSVWrapper> billOfMaterialByProductCSVWrappers = loadData(inputStream);
            billOfMaterialByProductCSVWrappers.stream().forEach(billOfMaterialByProductCSVWrapper -> save(convertFromWrapper(billOfMaterialByProductCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private BillOfMaterialByProduct convertFromWrapper(BillOfMaterialByProductCSVWrapper billOfMaterialByProductCSVWrapper) {

        BillOfMaterialByProduct billOfMaterialByProduct = new BillOfMaterialByProduct();
        billOfMaterialByProduct.setExpectedQuantity(billOfMaterialByProductCSVWrapper.getExpectedQuantity());

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                billOfMaterialByProductCSVWrapper.getCompany(),
                billOfMaterialByProductCSVWrapper.getWarehouse()
        );

        billOfMaterialByProduct.setBillOfMaterial(
                billOfMaterialService.findByNumber(warehouse.getId(), billOfMaterialByProductCSVWrapper.getBillOfMaterial()));

        billOfMaterialByProduct.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), billOfMaterialByProductCSVWrapper.getItem()).getId()
        );

        billOfMaterialByProduct.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), billOfMaterialByProductCSVWrapper.getInventoryStatus()).getId()
        );


        return billOfMaterialByProduct;
    }


}

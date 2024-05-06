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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class BillOfMaterialLineService  implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(BillOfMaterialLineService.class);

    @Autowired
    private BillOfMaterialLineRepository billOfMaterialLineRepository;
    @Autowired
    private BillOfMaterialService billOfMaterialService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    private final static int BOM_FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> bomFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> bomFileUploadResult = new ConcurrentHashMap<>();

    @Value("${fileupload.test-data.bill-of-material-line:bill-of-material-line}")
    String testDataFile;

    public BillOfMaterialLine findById(Long id, boolean loadDetails) {
        BillOfMaterialLine billOfMaterialLine = billOfMaterialLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bill of material line not found by id: " + id));
        if (loadDetails) {
            loadAttribute(billOfMaterialLine);
        }
        return billOfMaterialLine;
    }

    public BillOfMaterialLine findById(Long id) {
        return findById(id, true);
    }


    public List<BillOfMaterialLine> findAll(boolean loadDetails) {
        List<BillOfMaterialLine> billOfMaterialLines = billOfMaterialLineRepository.findAll();

        if (billOfMaterialLines.size() > 0 && loadDetails) {
            loadAttribute(billOfMaterialLines);
        }
        return billOfMaterialLines;
    }

    public List<BillOfMaterialLine> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<BillOfMaterialLine> billOfMaterialLines) {
        for (BillOfMaterialLine billOfMaterialLine : billOfMaterialLines) {
            loadAttribute(billOfMaterialLine);
        }
    }

    public void loadAttribute(BillOfMaterialLine billOfMaterialLine) {

        if (billOfMaterialLine.getItemId() != null && billOfMaterialLine.getItem() == null) {
            billOfMaterialLine.setItem(inventoryServiceRestemplateClient.getItemById(billOfMaterialLine.getItemId()));
        }
        if (billOfMaterialLine.getInventoryStatusId() != null && billOfMaterialLine.getInventoryStatus() == null) {
            billOfMaterialLine.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(billOfMaterialLine.getInventoryStatusId()));
        }

    }

    public BillOfMaterialLine findByNumber(String billOfMaterialNumber, String number) {
        return findByNumber(billOfMaterialNumber, number, true);

    }

    public BillOfMaterialLine findByNumber(String billOfMaterialNumber, String number, boolean loadDetails) {

        BillOfMaterialLine billOfMaterialLine
                = billOfMaterialLineRepository.findByNumber(
                           billOfMaterialNumber,number);
        if (billOfMaterialLine != null && loadDetails) {
            loadAttribute(billOfMaterialLine);
        }
        return billOfMaterialLine;
    }




    public BillOfMaterialLine save(BillOfMaterialLine billOfMaterialLine) {
        BillOfMaterialLine newOrder = billOfMaterialLineRepository.save(billOfMaterialLine);
        loadAttribute(billOfMaterialLine);
        return newOrder;
    }

    public BillOfMaterialLine saveOrUpdate(BillOfMaterialLine billOfMaterialLine) {
        if (billOfMaterialLine.getId() == null
                && findByNumber(
                        billOfMaterialLine.getBillOfMaterial().getNumber(), billOfMaterialLine.getNumber()) != null) {
            billOfMaterialLine.setId(
                    findByNumber(billOfMaterialLine.getBillOfMaterial().getNumber(), billOfMaterialLine.getNumber()).getId());
        }
        return save(billOfMaterialLine);
    }


    public void delete(BillOfMaterialLine billOfMaterialLine) {
        billOfMaterialLineRepository.delete(billOfMaterialLine);
    }

    public void delete(Long id) {
        billOfMaterialLineRepository.deleteById(id);
    }

    public void delete(String billOfMaterialLineIds) {
        if (!billOfMaterialLineIds.isEmpty()) {
            long[] billOfMaterialLineIdArray = Arrays.asList(billOfMaterialLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : billOfMaterialLineIdArray) {
                delete(id);
            }
        }
    }


    private CsvSchema getCsvSchema() {
        return  CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("billOfMaterial").
                addColumn("bomItem").
                addColumn("bomExpectedQuantity").
                addColumn("number").
                addColumn("item").
                addColumn("inventoryStatus").
                addColumn("expectedQuantity").
                build().withHeader();
    }
    public <T> List<T> loadData(File file, Class<T> tClass)  {
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema bootstrapSchema = CsvSchema.emptySchema() //
                .withHeader() //
                .withColumnSeparator(',');

        ObjectReader reader = csvMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
                .readerFor(tClass) //
                .with(bootstrapSchema);

        MappingIterator<T> iterator;
        try {
            iterator = reader.readValues(new FileInputStream(file));
        } catch (IOException e) {
            throw new IllegalStateException(String.format("could not access file " + file.getName()), e);
        }
        List<T> results = new ArrayList<>();
        iterator.forEachRemaining(results::add);
        return results;
    }


    public List<BillOfMaterialLineCSVWrapper> loadData(File file) throws IOException {


        return fileService.loadData(file, getCsvSchema(), BillOfMaterialLineCSVWrapper.class);
    }

    public List<BillOfMaterialLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        return fileService.loadData(inputStream, getCsvSchema(), BillOfMaterialLineCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<BillOfMaterialLineCSVWrapper> billOfMaterialLineCSVWrappers = loadData(inputStream);
            billOfMaterialLineCSVWrappers.stream().forEach(billOfMaterialLineCSVWrapper -> saveOrUpdate(convertFromWrapper(billOfMaterialLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private BillOfMaterialLine convertFromWrapper(BillOfMaterialLineCSVWrapper billOfMaterialLineCSVWrapper) {

        BillOfMaterialLine billOfMaterialLine = new BillOfMaterialLine();

        billOfMaterialLine.setNumber(billOfMaterialLineCSVWrapper.getNumber());
        billOfMaterialLine.setExpectedQuantity(billOfMaterialLineCSVWrapper.getExpectedQuantity());
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                billOfMaterialLineCSVWrapper.getCompany(),
                billOfMaterialLineCSVWrapper.getWarehouse()
        );

        // let's check if already have the BOM header
        BillOfMaterial billOfMaterial =
                billOfMaterialService.findByNumber(warehouse.getId(), billOfMaterialLineCSVWrapper.getBillOfMaterial());
        if (Objects.isNull(billOfMaterial)) {
            // BOM is not created yet, let's create it on the fly
            billOfMaterial = createBillOfMaterial(billOfMaterialLineCSVWrapper);
        }

        billOfMaterialLine.setBillOfMaterial(billOfMaterial);

        billOfMaterialLine.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), billOfMaterialLineCSVWrapper.getItem()).getId()
        );

        billOfMaterialLine.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), billOfMaterialLineCSVWrapper.getInventoryStatus()).getId()
        );


        return billOfMaterialLine;
    }

    private BillOfMaterial createBillOfMaterial(BillOfMaterialLineCSVWrapper billOfMaterialLineCSVWrapper) {
        BillOfMaterialCSVWrapper billOfMaterialCSVWrapper = new BillOfMaterialCSVWrapper();
        billOfMaterialCSVWrapper.setCompany(billOfMaterialLineCSVWrapper.getCompany());
        billOfMaterialCSVWrapper.setExpectedQuantity(billOfMaterialLineCSVWrapper.getBomExpectedQuantity());
        billOfMaterialCSVWrapper.setNumber(billOfMaterialLineCSVWrapper.getBillOfMaterial());
        billOfMaterialCSVWrapper.setItem(billOfMaterialLineCSVWrapper.getBomItem());
        billOfMaterialCSVWrapper.setWarehouse(billOfMaterialLineCSVWrapper.getWarehouse());

        return billOfMaterialService.saveOrUpdate(
                billOfMaterialService.convertFromWrapper(billOfMaterialCSVWrapper)
        );
    }


    public boolean match(BillOfMaterialLine billOfMaterialLine, WorkOrderLine workOrderLine) {

        if (billOfMaterialLine.getItemId().equals(workOrderLine.getItemId())) {
            return true;
        }
        else {
            return false;
        }
    }
    public String saveBOMLineData(Long warehouseId, File localFile)   {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearBOMFileUploadMap();

        bomFileUploadProgress.put(fileUploadProgressKey, 0.0);
        bomFileUploadResult.put(fileUploadProgressKey, new ArrayList<>());

        List<BillOfMaterialLineCSVWrapper> billOfMaterialLineCSVWrappers
                = fileService.loadData(localFile, BillOfMaterialLineCSVWrapper.class);

        billOfMaterialLineCSVWrappers.forEach(
                billOfMaterialLineCSVWrapper ->
                        billOfMaterialLineCSVWrapper.trim()
        );
        logger.debug("start to save {} BOM ", billOfMaterialLineCSVWrappers.size());

        bomFileUploadProgress.put(fileUploadProgressKey, 10.0);


        new Thread(() -> {

            int totalBOMLineCount = billOfMaterialLineCSVWrappers.size();
            int index = 0;
            // see if we need to create order
            for (BillOfMaterialLineCSVWrapper billOfMaterialLineCSVWrapper : billOfMaterialLineCSVWrappers) {
                try {

                    bomFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalBOMLineCount) * (index));

                    BillOfMaterialLine billOfMaterialLine =
                            convertFromWrapper(billOfMaterialLineCSVWrapper);


                    saveOrUpdate(billOfMaterialLine);


                    bomFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalBOMLineCount) * (index + 1));


                    List<FileUploadResult> fileUploadResults = bomFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            billOfMaterialLineCSVWrapper.toString(),
                            "success", ""
                    ));
                    bomFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process BOM line upload file record: {}, \n error message: {}",
                            billOfMaterialLineCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = bomFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            billOfMaterialLineCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    bomFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
                // after we process all inventory, mark the progress to 100%
                bomFileUploadProgress.put(fileUploadProgressKey, 100.0);
            }

        }).start();
        return fileUploadProgressKey;
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        billOfMaterialLineRepository.processItemOverride(
                warehouseId, oldItemId, newItemId
        );
    }

    public Object getBillOfMaterialsFileUploadProgress(String key) {

        return bomFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getBillOfMaterialsFileUploadResult(Long warehouseId, String key) {
        return bomFileUploadResult.getOrDefault(key, new ArrayList<>());
    }

    private void clearBOMFileUploadMap() {

        if (bomFileUploadProgress.size() > BOM_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = bomFileUploadProgress.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (bomFileUploadResult.size() > BOM_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = bomFileUploadResult.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }

}

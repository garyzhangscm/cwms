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
import com.garyzhangscm.cwms.workorder.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class BillOfMaterialService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(BillOfMaterialService.class);

    @Autowired
    private BillOfMaterialRepository billOfMaterialRepository;
    @Autowired
    private BillOfMaterialLineService billOfMaterialLineService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderInstructionTemplateService workOrderInstructionTemplateService;
    @Autowired
    private BillOfMaterialByProductService billOfMaterialByProductService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

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


    public List<BillOfMaterial> findAll(Long warehouseId, String number,
                                        String numbers,
                                        String itemName, boolean genericMatch, boolean loadDetails,
                                        ClientRestriction clientRestriction) {
        List<BillOfMaterial> billOfMaterials =  billOfMaterialRepository.findAll(
                (Root<BillOfMaterial> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        if (genericMatch || number.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }

                    }
                    if (!StringUtils.isBlank(numbers)) {

                        CriteriaBuilder.In<String> inNumbers = criteriaBuilder.in(root.get("number"));
                        for(String bomNumber : numbers.split(",")) {
                            inNumbers.value(bomNumber);
                        }
                        predicates.add(criteriaBuilder.and(inNumbers));

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

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    // return addClientRestriction(predicate, clientRestriction,
                    //        root, criteriaBuilder);
                    return Objects.isNull(clientRestriction) ?
                            predicate :
                            clientRestriction.addClientRestriction(predicate,
                                    root, criteriaBuilder);
                }
                ,
                Sort.by(Sort.Direction.ASC, "number")
        );


        if (billOfMaterials.size() > 0 && loadDetails) {
            loadAttribute(billOfMaterials);
        }
        return billOfMaterials;
    }

    public List<BillOfMaterial> findAll(Long warehouseId, String number,
                                        String numbers,String itemName,
                                        boolean genericMatch,
                                        ClientRestriction clientRestriction) {
        return findAll(warehouseId, number, numbers,  itemName, genericMatch, true, clientRestriction);
    }


    public BillOfMaterial findByNumber(Long warehouseId, Long clientId, String number, boolean loadDetails) {

        BillOfMaterial billOfMaterial =
                Objects.isNull(clientId) ?
                        billOfMaterialRepository.findByWarehouseIdAndNumber(warehouseId, number) :
                        billOfMaterialRepository.findByWarehouseIdAndClientIdAndNumber(warehouseId, clientId, number);

        if (billOfMaterial != null && loadDetails) {
            loadAttribute(billOfMaterial);
        }
        return billOfMaterial;
    }

    public BillOfMaterial findByNumber(Long warehouseId, Long clientId, String number) {
        return findByNumber(warehouseId, clientId, number, true);
    }
    public BillOfMaterial findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, null, number);
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

        billOfMaterial.getBillOfMaterialByProducts()
                .forEach(billOfMaterialByProduct -> billOfMaterialByProductService.loadAttribute(billOfMaterialByProduct));

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
                addColumn("company").
                addColumn("number").
                addColumn("warehouse").
                addColumn("item").
                addColumn("expectedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, BillOfMaterialCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<BillOfMaterialCSVWrapper> billOfMaterialCSVWrappers = loadData(inputStream);
            billOfMaterialCSVWrappers.stream().forEach(billOfMaterialCSVWrapper -> saveOrUpdate(convertFromWrapper(billOfMaterialCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    public BillOfMaterial convertFromWrapper(BillOfMaterialCSVWrapper billOfMaterialCSVWrapper) {

        BillOfMaterial billOfMaterial = new BillOfMaterial();
        billOfMaterial.setNumber(billOfMaterialCSVWrapper.getNumber());
        billOfMaterial.setExpectedQuantity(billOfMaterialCSVWrapper.getExpectedQuantity());

        logger.debug("Start to get warehouse: {}", billOfMaterialCSVWrapper.getWarehouse());
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                billOfMaterialCSVWrapper.getCompany(),
                billOfMaterialCSVWrapper.getWarehouse()
        );
        logger.debug("warehouse is null? {}", (warehouse == null));
        billOfMaterial.setWarehouseId(warehouse.getId());
        logger.debug("Start to get item: {}", billOfMaterialCSVWrapper.getItem());

        Client client = null;
        if (Strings.isNotBlank(billOfMaterialCSVWrapper.getClient())) {
            client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(),billOfMaterialCSVWrapper.getClient());
            if (Objects.isNull(client)) {
                throw ResourceNotFoundException.raiseException("Can not create Bill Of Measure due to not able to " +
                        " find client with name " + billOfMaterialCSVWrapper.getClient());
            }
            billOfMaterial.setClientId(client.getId());
        }

        Item item = inventoryServiceRestemplateClient.getItemByName(
                warehouse.getId(), Objects.isNull(client) ? null : client.getId(),
                billOfMaterialCSVWrapper.getItem());


        // raise error if we still can't find the item here
        if (Objects.isNull(item)) {
            throw ResourceNotFoundException.raiseException("Can not create Bill Of Measure due to " +
                    " not able to find item with name " + billOfMaterialCSVWrapper.getItem());
        }

        billOfMaterial.setItemId( item.getId());

        return billOfMaterial;
    }

    public BillOfMaterial getMatchedBillOfMaterial(Long workOrderid) {
        WorkOrder workOrder = workOrderService.findById(workOrderid);
        if (Objects.isNull(workOrder)) {
            return null;
        }
        return getMatchedBillOfMaterial(workOrder);
    }

    /**
     * Find the matched bill of material
     * 1. if the work order is created from the BOM, then the BOM is the 'matched BOM'
     * 2. Otherwise, the BOM needs to have the same master item and details as the work order
     * 2.1 the same master item
     * 2.2 the same number of line
     * 2.3 the ame quantity for each line
     * @param workOrder
     * @return
     */
    public BillOfMaterial getMatchedBillOfMaterial(WorkOrder workOrder) {
        if (Objects.nonNull(workOrder.getBillOfMaterial())) {
            BillOfMaterial billOfMaterial = workOrder.getBillOfMaterial();
            loadAttribute(billOfMaterial);
            return billOfMaterial;
        }

        // find by the item
        logger.debug("start to find bill of material by warehouse / item {}, {}",
                workOrder.getWarehouseId(), workOrder.getItem().getName());
        List<BillOfMaterial> billOfMaterials =
                findAll(workOrder.getWarehouseId(), "", "", workOrder.getItem().getName(), false, null);

        BillOfMaterial matchedBillOfMaterial = billOfMaterials.stream()
                .filter(billOfMaterial -> match(billOfMaterial, workOrder)).findFirst().orElse(null);
        if (matchedBillOfMaterial != null) {

            loadAttribute(matchedBillOfMaterial);
            return matchedBillOfMaterial;
        }
        else {
            return null;
        }
    }
    public List<BillOfMaterial>  findMatchedBillOfMaterialByItemName(Long warehouseId,
                                                               String itemName) {
        return findAll(warehouseId, "","",  itemName, true, null);

    }
    /**
     * @param billOfMaterial
     * @param workOrder
     * @return
     */
    private boolean match(BillOfMaterial billOfMaterial, WorkOrder workOrder) {

        if (!billOfMaterial.getItemId().equals(workOrder.getItemId())) {
            return false;
        }

        if (billOfMaterial.getBillOfMaterialLines().size() != workOrder.getWorkOrderLines().size()) {
            return false;
        }

        for (BillOfMaterialLine billOfMaterialLine : billOfMaterial.getBillOfMaterialLines()) {
            boolean findMatchedLine = false;
            for (WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {
                if (billOfMaterialLineService.match(billOfMaterialLine, workOrderLine)) {
                    findMatchedLine = true;
                }
            }
            if (!findMatchedLine) {
                return false;
            }

        }
        return true;
    }


    public BillOfMaterial addBillOfMaterials(BillOfMaterial billOfMaterial) {
        billOfMaterial.getBillOfMaterialLines().forEach(
                billOfMaterialLine -> billOfMaterialLine.setBillOfMaterial(billOfMaterial)
        );

        billOfMaterial.getWorkOrderInstructionTemplates().forEach(
                workOrderInstructionTemplate -> workOrderInstructionTemplate.setBillOfMaterial(billOfMaterial)
        );

        billOfMaterial.getBillOfMaterialByProducts().forEach(
                billOfMaterialByProduct -> billOfMaterialByProduct.setBillOfMaterial(billOfMaterial)
        );
        return save(billOfMaterial);
    }

    public BillOfMaterial changeBillOfMaterial( Long id,
                                                BillOfMaterial billOfMaterial){
        BillOfMaterial existingBillOfMaterial = findById(id);

        existingBillOfMaterial.setNumber(billOfMaterial.getNumber());
        existingBillOfMaterial.setDescription(billOfMaterial.getDescription());

        existingBillOfMaterial.setItemId(billOfMaterial.getItemId());
        existingBillOfMaterial.setExpectedQuantity(billOfMaterial.getExpectedQuantity());

        existingBillOfMaterial = saveOrUpdate(existingBillOfMaterial);

        // process lines
        changeBillOfMaterialLines(existingBillOfMaterial, billOfMaterial);

        // process work instruction
        changeWorkingInstruction(existingBillOfMaterial, billOfMaterial);
        // process by product
        changeByProduct(existingBillOfMaterial, billOfMaterial);

        return existingBillOfMaterial;
    }

    private void changeByProduct(BillOfMaterial existingBillOfMaterial, BillOfMaterial billOfMaterial) {
        // Remove the by product if it doesn't exists any more
        existingBillOfMaterial.getBillOfMaterialByProducts().stream()
                .filter(byProduct ->
                        // check if this instruction still exists in the new BOM structure
                        // If not, then return true as we will remove this line
                        billOfMaterial.getBillOfMaterialByProducts().stream()
                                .filter(newByPorduct -> Objects.equals(byProduct.getId(), newByPorduct.getId()))
                                .count() == 0
                ).forEach(byProduct -> billOfMaterialByProductService.delete(byProduct));

        // For each new by product in the new BOM structure(instruction id is null)
        // add it
        billOfMaterial.getBillOfMaterialByProducts().stream()
                .filter(byProduct -> Objects.isNull(byProduct.getId()))
                .forEach(byProduct -> {
                    byProduct.setBillOfMaterial(existingBillOfMaterial);
                    billOfMaterialByProductService.save(byProduct);
                });

        // For each by product that exists in both old BOM and new BOM structure,
        // let's change the by product
        // The only thing we allow the user to change is the quantity.
        // If the user want to change the item, they can only do so by
        // removing the existing by product information and add a new by product

        existingBillOfMaterial.getBillOfMaterialByProducts().stream()
                .forEach(byProduct -> {
                    billOfMaterial.getBillOfMaterialByProducts().stream()
                            .filter(newByPorduct ->
                                    // only return when the lines have the same ID
                                    // but have different quantities
                                    Objects.equals(byProduct.getId(), newByPorduct.getId()) &&
                                            !byProduct.getExpectedQuantity().equals(newByPorduct.getExpectedQuantity()))
                            .forEach(newByPorduct -> {
                                byProduct.setExpectedQuantity(newByPorduct.getExpectedQuantity());
                                billOfMaterialByProductService.save(byProduct);

                            });
                });
    }

    private void changeWorkingInstruction(BillOfMaterial existingBillOfMaterial, BillOfMaterial billOfMaterial) {
        // Remove the instruction if it doesn't exists any more
        existingBillOfMaterial.getWorkOrderInstructionTemplates().stream()
                .filter(workOrderInstructionTemplate ->
                        // check if this instruction still exists in the new BOM structure
                        // If not, then return true as we will remove this line
                        billOfMaterial.getWorkOrderInstructionTemplates().stream()
                                .filter(newWorkOrderInstructionTemplate -> Objects.equals(workOrderInstructionTemplate.getId(), newWorkOrderInstructionTemplate.getId()))
                                .count() == 0
                ).forEach(workOrderInstructionTemplate -> workOrderInstructionTemplateService.delete(workOrderInstructionTemplate));

        // For each new instruction in the new BOM structure(instruction id is null)
        // add it
        billOfMaterial.getWorkOrderInstructionTemplates().stream()
                .filter(workOrderInstructionTemplate -> Objects.isNull(workOrderInstructionTemplate.getId()))
                .forEach(workOrderInstructionTemplate -> {
                    workOrderInstructionTemplate.setBillOfMaterial(existingBillOfMaterial);
                    workOrderInstructionTemplateService.save(workOrderInstructionTemplate);
                });

        // For each instruction that exists in both old BOM and new BOM structure,
        // let's change the instruction
        existingBillOfMaterial.getWorkOrderInstructionTemplates().stream()
                .forEach(workOrderInstructionTemplate -> {
                    billOfMaterial.getWorkOrderInstructionTemplates().stream()
                            .filter(newWorkOrderInstructionTemplate ->
                                    // only return when the lines have the same ID
                                    // but have different quantities
                                    Objects.equals(workOrderInstructionTemplate.getId(), newWorkOrderInstructionTemplate.getId()) &&
                                            !workOrderInstructionTemplate.getInstruction().equals(newWorkOrderInstructionTemplate.getInstruction()))
                            .forEach(newWorkOrderInstructionTemplate -> {
                                workOrderInstructionTemplate.setInstruction(newWorkOrderInstructionTemplate.getInstruction());
                                workOrderInstructionTemplateService.save(workOrderInstructionTemplate);

                            });
                });
    }

    private void changeBillOfMaterialLines(BillOfMaterial existingBillOfMaterial, BillOfMaterial billOfMaterial) {
        // Remove the lines if it doesn't exists any more
        existingBillOfMaterial.getBillOfMaterialLines().stream()
                .filter(billOfMaterialLine ->
                    // check if this line still exists in the new BOM structure
                    // If not, then return true as we will remove this line
                    billOfMaterial.getBillOfMaterialLines().stream()
                            .filter(newBillOfMaterialLine -> Objects.equals(billOfMaterialLine.getId(), newBillOfMaterialLine.getId()))
                            .count() == 0
                 ).forEach(billOfMaterialLine -> billOfMaterialLineService.delete(billOfMaterialLine));

        // For each new line in the new BOM structure(line id is null)
        // add it
        billOfMaterial.getBillOfMaterialLines().stream()
                .filter(billOfMaterialLine -> Objects.isNull(billOfMaterial.getId()))
                .forEach(billOfMaterialLine -> {
                    billOfMaterialLine.setBillOfMaterial(existingBillOfMaterial);
                    billOfMaterialLineService.save(billOfMaterialLine);
                });

        // For each line that exists in both old BOM and new BOM structure,
        // let's change the quantity
        // The only thing we allow the user to change is the quantity.
        // If the user want to change the item, they can only do so by
        // removing the existing line and add a new line
        existingBillOfMaterial.getBillOfMaterialLines().stream()
                .forEach(billOfMaterialLine -> {
                    billOfMaterial.getBillOfMaterialLines().stream()
                            .filter(newBillOfMaterialLine ->
                                    // only return when the lines have the same ID
                                    // but have different quantities
                                    Objects.equals(billOfMaterialLine.getId(), newBillOfMaterialLine.getId()) &&
                                    !billOfMaterialLine.getExpectedQuantity().equals(newBillOfMaterialLine.getExpectedQuantity()))
                            .forEach(newBillOfMaterialLine -> {
                                billOfMaterialLine.setExpectedQuantity(newBillOfMaterialLine.getExpectedQuantity());
                                billOfMaterialLineService.save(billOfMaterialLine);

                            });
                });
    }

    /**
     * Make sure the number is new and not exists in the warehouse yet
     * @param warehouseId
     * @param number
     * @return
     */
    public String validateNewBOMNumber(Long warehouseId, Long clientId, String number) {
        BillOfMaterial billOfMaterial =
                findByNumber(warehouseId, clientId, number, false);

        return Objects.isNull(billOfMaterial) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();

    }


    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        billOfMaterialRepository.processItemOverride(warehouseId,oldItemId, newItemId);

        billOfMaterialLineService.handleItemOverride(
                warehouseId, oldItemId, newItemId);
        billOfMaterialByProductService.handleItemOverride(
                warehouseId, oldItemId, newItemId);

    }
}

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

import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.TargetShippingCartonLabelRepository;
import com.garyzhangscm.cwms.outbound.repository.TargetShippingCartonLabelRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class TargetShippingCartonLabelService {
    private static final Logger logger = LoggerFactory.getLogger(TargetShippingCartonLabelService.class);


    @Autowired
    private TargetShippingCartonLabelRepository targetShippingCartonLabelRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultMap = new ConcurrentHashMap<>();



    public TargetShippingCartonLabel findById(Long id) {
        return targetShippingCartonLabelRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("target shipping carton label not found by id: " + id));

    }

    public List<TargetShippingCartonLabel> findAll(Long warehouseId, String SSCC18,
                                                    String SSCC18s,
                                                    String poNumber,
                                                    String itemNumber,
                                                    Long palletPickLabelContentId,
                                                    Boolean notPrinted,
                                                    Boolean notAssignedToPalletPickLabel,
                                                    Integer count) {

        if (Objects.isNull(count) || count <= 0) {
            count = Integer.MAX_VALUE;
        }
        Pageable limit = PageRequest.of(0,count);

        return  targetShippingCartonLabelRepository.findAll(
                (Root<TargetShippingCartonLabel> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(SSCC18)) {

                        predicates.add(criteriaBuilder.equal(root.get("SSCC18"), SSCC18));
                    }
                    if (Strings.isNotBlank(SSCC18s)) {

                        CriteriaBuilder.In<String> in = criteriaBuilder.in(root.get("SSCC18"));
                        for(String sscc18 : SSCC18s.split(",")) {
                            in.value(sscc18);
                        }
                        predicates.add(criteriaBuilder.and(in));
                    }


                    if (Strings.isNotBlank(poNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("poNumber"), poNumber));
                    }

                    if (Strings.isNotBlank(itemNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemNumber"), itemNumber));
                    }
                    if (Objects.nonNull(palletPickLabelContentId)) {

                        Join<TargetShippingCartonLabel, PalletPickLabelContent> joinPalletPickLabelContent =
                                root.join("palletPickLabelContent", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinPalletPickLabelContent.get("id"), palletPickLabelContentId));
                    }
                    if (Boolean.TRUE.equals(notPrinted)) {

                        predicates.add(criteriaBuilder.isNull(root.get("lastPrintTime")));
                    }
                    if (Boolean.TRUE.equals(notAssignedToPalletPickLabel)) {

                        predicates.add(criteriaBuilder.isNull(root.get("palletPickLabelContent")));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        ).getContent();
    }


    public TargetShippingCartonLabel findBySSCC18(String SSCC18) {
        return targetShippingCartonLabelRepository.findBySSCC18(SSCC18);
    }

    public List<TargetShippingCartonLabel> findByPoNumberAndItem(
            Long warehouseId, String poNumber, String itemName,
            boolean nonAssignedOnly, boolean nonPrintedOnly) {
        return findByPoNumberAndItem(warehouseId, poNumber, itemName,
                nonAssignedOnly, nonPrintedOnly, Integer.MAX_VALUE);
    }

    private Long getPieceCartonFromShippingCartonLabel(Long warehouseId, String poNumber, String itemName) {
        String pieceCarton = targetShippingCartonLabelRepository.getPieceCartonFromShippingCartonLabel(
                warehouseId, poNumber, itemName
        );
        if (Strings.isNotBlank(pieceCarton)) {
            return Long.parseLong(pieceCarton);
        }
        else {
            return null;
        }
    }
    public List<TargetShippingCartonLabel> findByPoNumberAndItem(Long warehouseId, String poNumber, String itemName,
                                                                  boolean nonAssignedOnly, boolean nonPrintedOnly, int labelCount) {

        return findAll(warehouseId,
                null,
                null,
                poNumber,
                itemName,
                null,
                nonPrintedOnly, nonAssignedOnly, labelCount);
    }


    @Transactional
    public TargetShippingCartonLabel save(TargetShippingCartonLabel TargetShippingCartonLabel) {
        return targetShippingCartonLabelRepository.save(TargetShippingCartonLabel);
    }


    @Transactional
    public TargetShippingCartonLabel saveOrUpdate(TargetShippingCartonLabel targetShippingCartonLabel ) {
        if (targetShippingCartonLabel.getId() == null && findBySSCC18(targetShippingCartonLabel.getSSCC18()) != null) {
            targetShippingCartonLabel.setId(
                    findBySSCC18(targetShippingCartonLabel.getSSCC18()).getId());
        }
        return save(targetShippingCartonLabel);
    }

    @Transactional
    public void delete(TargetShippingCartonLabel targetShippingCartonLabel) {
        targetShippingCartonLabelRepository.delete(targetShippingCartonLabel);
    }

    public void delete(Long id) {
        targetShippingCartonLabelRepository.deleteById(id);
    }

    public String updateTargetShippingCartonLabels(Long warehouseId,
                                File localFile) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();

        fileUploadProgress.put(fileUploadProgressKey, 0.0);
        fileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());

        List<TargetShippingCartonLabel> targetShippingCartonLabels =
                fileService.loadData(localFile, TargetShippingCartonLabel.class);

        logger.debug("start to save {} target shipping carton labels", targetShippingCartonLabels.size());

        fileUploadProgress.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {
            int totalCount = targetShippingCartonLabels.size();
            int index = 0;
            for (TargetShippingCartonLabel targetShippingCartonLabel : targetShippingCartonLabels) {

                try {
                    targetShippingCartonLabel.setWarehouseId(warehouseId);
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.5));
                    logger.debug("start to save TargetShippingCartonLabel: {}",
                            targetShippingCartonLabel);
                    saveOrUpdate(targetShippingCartonLabel);

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            targetShippingCartonLabel.toString(),
                            "success", ""
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receiving walmart shipping carton label upload file record: {}, \n error message: {}",
                            targetShippingCartonLabel,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            targetShippingCartonLabel.toString(),
                            "fail", ex.getMessage()
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));
                }
                finally {

                    index++;
                }
            }
        }).start();

        return fileUploadProgressKey;

    }

    private void clearFileUploadMap() {

        if (fileUploadProgress.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadProgress.keySet().iterator();
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

        if (fileUploadResultMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadResultMap.keySet().iterator();
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


    public double getTargetShippingCartonLabelsFileUploadProgress(String key) {
        return fileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getTargetShippingCartonLabelsFileUploadResult(Long warehouseId, String key) {
        return fileUploadResultMap.getOrDefault(key, new ArrayList<>());
    }


    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId, String SSCC18s, int copies, String locale) {
        Report reportData = new Report();
        setupTargetShippingCartonLabelData(
                warehouseId,
                reportData, SSCC18s,  copies
        );


        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("Will print {} labels", reportData.getData().size());
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.TARGET_SHIPPING_CARTON_LABEL, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    public List<TargetShippingCartonLabel> findByPalletPickLabel(PalletPickLabelContent palletPickLabelContent) {
        /**
        return findAll(
                palletPickLabelContent.getWarehouseId(),
                null,
                null,
                null,
                null,
                null,
                null,
                palletPickLabelContent.getId(),
                null, null, null
        );
         **/
        return targetShippingCartonLabelRepository.findByPalletPickLabelContentId(palletPickLabelContent.getId());
    }
    private void setupTargetShippingCartonLabelData(Long warehouseId,
                                                     Report reportData,
                                                     String SSCC18s,
                                                     int copies) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();
        if (Strings.isNotBlank(SSCC18s)) {
            List<TargetShippingCartonLabel> targetShippingCartonLabels =
                    findAll(warehouseId, null, SSCC18s,  null,
                            null, null, null, null, null);

            targetShippingCartonLabels.forEach(
                    targetShippingCartonLabel -> {
                        Map<String, Object> lpnLabelContent =   getTargetShippingCartonLabelContent(
                                targetShippingCartonLabel
                        );
                        for (int i = 0; i < copies; i++) {

                            lpnLabelContents.add(lpnLabelContent);
                        }

                    }
            );
        }
        reportData.setData(lpnLabelContents);

    }

    private Map<String, Object> getTargetShippingCartonLabelContent(TargetShippingCartonLabel targetShippingCartonLabel) {
        Map<String, Object> lpnLabelContent = new HashMap<>();

        lpnLabelContent.put("shipTo", targetShippingCartonLabel.getShipToName());
        lpnLabelContent.put("address1", targetShippingCartonLabel.getAddress1());
        lpnLabelContent.put("cityStateZip", targetShippingCartonLabel.getCityStateZip());

        lpnLabelContent.put("zip420", targetShippingCartonLabel.getZip420());

        lpnLabelContent.put("poNumber", targetShippingCartonLabel.getPoNumber());
        lpnLabelContent.put("dpci", targetShippingCartonLabel.getDpci());
        lpnLabelContent.put("casepack", targetShippingCartonLabel.getPieceCarton());

        lpnLabelContent.put("style", targetShippingCartonLabel.getItemNumber());

        lpnLabelContent.put("SSCC18", targetShippingCartonLabel.getSSCC18());


        return lpnLabelContent;
    }

    /**
     * Assign walmart carton label to pallet pick label so that we know we already assigned to certain pallet pick label
     * @param palletPickLabelContent
     * @return
     */
    public List<TargetShippingCartonLabel> assignShippingCartonLabel(Order order,
                                                                      PalletPickLabelContent palletPickLabelContent) {
        // let's find all the available shipping carton labels that belongs to the order(BY PO number)
        // and not printed yet
        if (Strings.isBlank(order.getPoNumber())) {
            throw OrderOperationException.raiseException("The order " + order.getNumber() + " doesn't have a PO number");
        }
        List<TargetShippingCartonLabel> result = new ArrayList<>();
        // for each picks in this pallet picking label, let's get the item and its quantity
        // and then we can get available shipping carton label for each box of the item
        for (PalletPickLabelPickDetail palletPickLabelPickDetail : palletPickLabelContent.getPalletPickLabelPickDetails()) {
            Item item = palletPickLabelPickDetail.getPick().getItem();
            if (Objects.isNull(item)) {
                item = inventoryServiceRestemplateClient.getItemById(
                        palletPickLabelPickDetail.getPick().getItemId()
                );
            }
            // only continue when there's no
            if (Objects.isNull(item)) {

                throw OrderOperationException.raiseException("fail to assign walmart shipping carton label to the pallet picking label," +
                        " can't load the item information");
            }

            // see how many cases of the item will be picked onto this pallet
            // let's get the piece / carton from the shipping label first
            Long quantityPerCase = getPieceCartonFromShippingCartonLabel(
                    item.getWarehouseId(), order.getPoNumber(), item.getName()
            );
            logger.debug("Get piece per carton from the shipping label: {}",
                    Objects.isNull(quantityPerCase) ? "N/A" : quantityPerCase);

            if (Objects.isNull(quantityPerCase)) {
                logger.debug("Piece per carton is not defined by the shipping label, let's get from the item");
                quantityPerCase = getPieceCartonFromItem(item, palletPickLabelPickDetail.getPick());
            }
            logger.debug("final quantityPerCase: {}", quantityPerCase);

            int caseQuantity = (int)Math.ceil(palletPickLabelPickDetail.getPickQuantity() / quantityPerCase);
            logger.debug("case quantity from this pick {}: {}",
                    palletPickLabelPickDetail.getPick().getNumber(),
                    caseQuantity);
            List<TargetShippingCartonLabel> availableTargetShippingCartonLabels =
                    findByPoNumberAndItem(order.getWarehouseId(),
                            order.getPoNumber(),  item.getName(),
                            true, true, caseQuantity);

            logger.debug("get {} carton labels for this item {} in the order {}, for {} cases",
                    availableTargetShippingCartonLabels.size(),
                    item.getName(),
                    order.getNumber(),
                    caseQuantity);
            result.addAll(availableTargetShippingCartonLabels);
            // start to assign the labels to the pallet label
            availableTargetShippingCartonLabels.forEach(
                    targetShippingCartonLabel -> assignTargetShippingCartonLabel(targetShippingCartonLabel, palletPickLabelContent)
            );

        }
        return result;
    }

    private Long getPieceCartonFromItem(Item item, Pick pick) {
        Long quantityPerCase = 1l;
        ItemPackageType itemPackageType = pick.getItemPackageType();
        if (Objects.isNull(itemPackageType) && Objects.nonNull(pick.getItemPackageTypeId())) {
            itemPackageType = inventoryServiceRestemplateClient.getItemPackageTypeById(
                    pick.getItemPackageTypeId()
            );
        }
        if (Objects.isNull(itemPackageType)) {
            itemPackageType = item.getDefaultItemPackageType();
        }
        if (Objects.isNull(itemPackageType.getCaseItemUnitOfMeasure())) {
            logger.debug("There's no case UOM defined for the item package type {} of item {}, let's use the stock UOM {}'s quantity = {}",
                    itemPackageType.getName(),
                    item.getName(),
                    itemPackageType.getStockItemUnitOfMeasure().getUnitOfMeasure().getName(),
                    itemPackageType.getStockItemUnitOfMeasure().getQuantity());
            quantityPerCase = itemPackageType.getStockItemUnitOfMeasure().getQuantity();
        }
        else {
            logger.debug("Case UOM {} is defined for the item package type {} of item {}, let's use the its quantity = {}",
                    itemPackageType.getCaseItemUnitOfMeasure().getUnitOfMeasure().getName(),
                    itemPackageType.getName(),
                    item.getName(),
                    itemPackageType.getCaseItemUnitOfMeasure().getQuantity());
            quantityPerCase = itemPackageType.getCaseItemUnitOfMeasure().getQuantity();
        }
        return quantityPerCase;
    }

    private void assignTargetShippingCartonLabel(TargetShippingCartonLabel targetShippingCartonLabel,
                                                  PalletPickLabelContent palletPickLabelContent) {
        targetShippingCartonLabel.setPalletPickLabelContent( palletPickLabelContent);
        saveOrUpdate(targetShippingCartonLabel);
    }

    /**
     * Once we remove a pallet pallet, we may need to release all the shipping carton on it as well so
     * that those shipping cartons can be group into a new pallet
     * @param palletPickLabelContent
     */
    public void releaseShippingCartonLabel(PalletPickLabelContent palletPickLabelContent) {
        logger.debug("start to remove target shipping carton labels " +
                " for pallet with id and number {} / {}",
                palletPickLabelContent.getId(), palletPickLabelContent.getNumber());
        List<TargetShippingCartonLabel> targetShippingCartonLabels
                = findByPalletPickLabel(palletPickLabelContent);
        logger.debug("we get {} target shipping carton labels from the pallet with id {}",
                targetShippingCartonLabels.size(),
                palletPickLabelContent.getId());

        targetShippingCartonLabels.forEach(
                targetShippingCartonLabel -> {
                    targetShippingCartonLabel.setPalletPickLabelContent(null);
                    targetShippingCartonLabel.setLastPrintTime(null);
                    saveOrUpdate(targetShippingCartonLabel);
                }
        );




        
    }

    @Transactional
    public void removeTargetShippingCartonLabel(Long id) {
        targetShippingCartonLabelRepository.deleteById(id);
    }

    @Transactional
    public void removeTargetShippingCartonLabels(String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
        targetShippingCartonLabelRepository.deleteByIdIn(idList);
    }
}

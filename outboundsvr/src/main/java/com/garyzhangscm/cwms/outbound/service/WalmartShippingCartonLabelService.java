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

import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.WalmartShippingCartonLabelRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class WalmartShippingCartonLabelService {
    private static final Logger logger = LoggerFactory.getLogger(WalmartShippingCartonLabelService.class);


    @Autowired
    private WalmartShippingCartonLabelRepository walmartShippingCartonLabelRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultMap = new ConcurrentHashMap<>();



    public WalmartShippingCartonLabel findById(Long id) {
        return walmartShippingCartonLabelRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("walmart shipping carton label not found by id: " + id));

    }

    public List<WalmartShippingCartonLabel> findAll(Long warehouseId, String SSCC18,
                                                    String SSCC18s,
                                                    String poNumber, String type,
                                                    String dept,
                                                    String itemNumber,
                                                    Long palletPickLabelContentId) {

        return  walmartShippingCartonLabelRepository.findAll(
                (Root<WalmartShippingCartonLabel> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

                    if (Strings.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), type));
                    }

                    if (Strings.isNotBlank(dept)) {

                        predicates.add(criteriaBuilder.equal(root.get("dept"), dept));
                    }

                    if (Strings.isNotBlank(itemNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemNumber"), itemNumber));
                    }
                    if (Objects.nonNull(palletPickLabelContentId)) {

                        Join<WalmartShippingCartonLabel, PalletPickLabelContent> joinPalletPickLabelContent =
                                root.join("palletPickLabelContent", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinPalletPickLabelContent.get("id"), palletPickLabelContentId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public WalmartShippingCartonLabel findBySSCC18(String SSCC18) {
        return walmartShippingCartonLabelRepository.findBySSCC18(SSCC18);
    }

    public List<WalmartShippingCartonLabel> findByPoNumber(Long warehouseId, String poNumber) {
        return walmartShippingCartonLabelRepository.findByWarehouseIdAndPoNumber(warehouseId, poNumber);
    }


    @Transactional
    public WalmartShippingCartonLabel save(WalmartShippingCartonLabel walmartShippingCartonLabel) {
        return walmartShippingCartonLabelRepository.save(walmartShippingCartonLabel);
    }


    @Transactional
    public WalmartShippingCartonLabel saveOrUpdate(WalmartShippingCartonLabel walmartShippingCartonLabel ) {
        if (walmartShippingCartonLabel.getId() == null && findBySSCC18(walmartShippingCartonLabel.getSSCC18()) != null) {
            walmartShippingCartonLabel.setId(
                    findBySSCC18(walmartShippingCartonLabel.getSSCC18()).getId());
        }
        return save(walmartShippingCartonLabel);
    }

    @Transactional
    public void delete(WalmartShippingCartonLabel walmartShippingCartonLabel) {
        walmartShippingCartonLabelRepository.delete(walmartShippingCartonLabel);
    }

    public void delete(Long id) {
        walmartShippingCartonLabelRepository.deleteById(id);
    }

    public String updateWalmartShippingCartonLabels(Long warehouseId,
                                File localFile) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();

        fileUploadProgress.put(fileUploadProgressKey, 0.0);
        fileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());

        List<WalmartShippingCartonLabel> walmartShippingCartonLabels =
                fileService.loadData(localFile, WalmartShippingCartonLabel.class);

        logger.debug("start to save {} walmart shipping carton labels", walmartShippingCartonLabels.size());

        fileUploadProgress.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {
            int totalCount = walmartShippingCartonLabels.size();
            int index = 0;
            for (WalmartShippingCartonLabel walmartShippingCartonLabel : walmartShippingCartonLabels) {

                try {
                    walmartShippingCartonLabel.setWarehouseId(warehouseId);
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.5));
                    logger.debug("start to save walmartShippingCartonLabel: {}",
                            walmartShippingCartonLabel);
                    saveOrUpdate(walmartShippingCartonLabel);

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            walmartShippingCartonLabel.toString(),
                            "success", ""
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receiving walmart shipping carton label upload file record: {}, \n error message: {}",
                            walmartShippingCartonLabel,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            walmartShippingCartonLabel.toString(),
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


    public double getWalmartShippingCartonLabelsFileUploadProgress(String key) {
        return fileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getWalmartShippingCartonLabelsFileUploadResult(Long warehouseId, String key) {
        return fileUploadResultMap.getOrDefault(key, new ArrayList<>());
    }


    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId, String SSCC18s, int copies, String locale) {
        Report reportData = new Report();
        setupWalmartShippingCartonLabelData(
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
                        warehouseId, ReportType.WALMART_SHIPPING_CARTON_LABEL, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    public List<WalmartShippingCartonLabel> findByPalletPickLabel(PalletPickLabelContent palletPickLabelContent) {
        return findAll(
                palletPickLabelContent.getWarehouseId(),
                null,
                null,
                null,
                null,
                null,
                null,
                palletPickLabelContent.getId()
        );
    }
    private void setupWalmartShippingCartonLabelData(Long warehouseId,
                                                     Report reportData,
                                                     String SSCC18s,
                                                     int copies) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();
        if (Strings.isNotBlank(SSCC18s)) {
            List<WalmartShippingCartonLabel> walmartShippingCartonLabels =
                    findAll(warehouseId, null, SSCC18s, null, null, null, null, null);

            walmartShippingCartonLabels.forEach(
                    walmartShippingCartonLabel -> {
                        Map<String, Object> lpnLabelContent =   getWalmartShippingCartonLabelContent(
                                walmartShippingCartonLabel
                        );
                        for (int i = 0; i < copies; i++) {

                            lpnLabelContents.add(lpnLabelContent);
                        }

                    }
            );
        }
        reportData.setData(lpnLabelContents);

    }

    private Map<String, Object> getWalmartShippingCartonLabelContent(WalmartShippingCartonLabel walmartShippingCartonLabel) {
        Map<String, Object> lpnLabelContent = new HashMap<>();

        lpnLabelContent.put("address1", walmartShippingCartonLabel.getAddress1());
        lpnLabelContent.put("BOL", walmartShippingCartonLabel.getBOL());
        lpnLabelContent.put("carrierNumber", walmartShippingCartonLabel.getCarrierNumber());
        lpnLabelContent.put("cityStateZip", walmartShippingCartonLabel.getCityStateZip());
        lpnLabelContent.put("DC", walmartShippingCartonLabel.getDC());
        lpnLabelContent.put("dept", walmartShippingCartonLabel.getDept());
        lpnLabelContent.put("poNumber", walmartShippingCartonLabel.getPoNumber());
        lpnLabelContent.put("shipTo", walmartShippingCartonLabel.getShipTo());
        lpnLabelContent.put("SSCC18", walmartShippingCartonLabel.getSSCC18());
        lpnLabelContent.put("type", walmartShippingCartonLabel.getType());
        lpnLabelContent.put("WMIT", walmartShippingCartonLabel.getWMIT());

        return lpnLabelContent;
    }

    public List<WalmartShippingCartonLabel> assignShippingCartonLabel(PalletPickLabelContent palletPickLabelContent) {
    }
}

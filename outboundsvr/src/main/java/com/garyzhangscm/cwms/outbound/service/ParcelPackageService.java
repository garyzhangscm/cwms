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

import com.easypost.model.Shipment;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiConfiguration;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiShippingLabelFormatByProduct;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiTrackResponseData;
import com.garyzhangscm.cwms.outbound.repository.ParcelPackageRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class ParcelPackageService  {
    private static final Logger logger = LoggerFactory.getLogger(ParcelPackageService.class);

    @Autowired
    private ParcelPackageRepository parcelPackageRepository;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private HualeiConfigurationService hualeiConfigurationService;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private HualeiShippingService hualeiShippingService;


    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultMap = new ConcurrentHashMap<>();



    public ParcelPackage findById(Long id) {
        return parcelPackageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("parcel package not found by id: " + id));
    }

    public ParcelPackage save(ParcelPackage parcelPackage) {
        return parcelPackageRepository.save(parcelPackage);
    }

    public ParcelPackage saveOrUpdate(ParcelPackage parcelPackage) {
        if (Objects.isNull(parcelPackage.getId()) &&
                Objects.nonNull(findByShipmentId(parcelPackage.getWarehouseId(),parcelPackage.getShipmentId()))) {
            parcelPackage.setId(findByShipmentId(parcelPackage.getWarehouseId(),parcelPackage.getShipmentId()).getId());
        }
        return save(parcelPackage);
    }

    public ParcelPackage findByShipmentId(Long warehouseId, String shipmentId) {
        return parcelPackageRepository.findByWarehouseIdAndShipmentId(warehouseId, shipmentId);
    }

    public ParcelPackage findByTrackingCode(Long warehouseId, String trackingCode) {
        return parcelPackageRepository.findByWarehouseIdAndTrackingCode(warehouseId, trackingCode);
    }

    public ParcelPackage findByTrackingCode(String trackingCode) {
        return parcelPackageRepository.findByTrackingCode(trackingCode);
    }


    public ParcelPackage addParcelPackage(Long warehouseId,
                                          Long orderId,
                                          ParcelPackage parcelPackage) {
        if (Objects.isNull(parcelPackage.getWarehouseId())) {
            parcelPackage.setWarehouseId(warehouseId);
        }
        if (Objects.isNull(parcelPackage.getOrder())) {
            Order order = orderService.findById(orderId, false);
            parcelPackage.setOrder(order);
        }
        return saveOrUpdate(parcelPackage);
    }
    public ParcelPackage addParcelPackage(Long warehouseId,
                                           ParcelPackage parcelPackage) {
        if (Objects.isNull(parcelPackage.getWarehouseId())) {
            parcelPackage.setWarehouseId(warehouseId);
        }
        return saveOrUpdate(parcelPackage);
    }

    public ParcelPackage addParcelPackage(Long warehouseId,
                                           Order order,
                                           Shipment easyPostShipment) {
        return addParcelPackage(warehouseId, new ParcelPackage(warehouseId, order, easyPostShipment));
    }


    public List<ParcelPackage> findAll(Long warehouseId,
                                       Long orderId,
                                       String orderNumber,
                                       String trackingCode,
                                       Boolean undeliveredPackageOnly,
                                       ParcelPackageRequestSystem requestSystem) {

        List<ParcelPackage> cartons =  parcelPackageRepository.findAll(
                (Root<ParcelPackage> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(orderId)) {
                        Join<ParcelPackage, Order> joinOrder = root.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }
                    if (Strings.isNotBlank(orderNumber)) {
                        Join<ParcelPackage, Order> joinOrder = root.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));

                    }
                    if (Strings.isNotBlank(trackingCode)) {
                        predicates.add(criteriaBuilder.equal(root.get("trackingCode"), trackingCode));

                    }
                    if (Boolean.TRUE.equals(undeliveredPackageOnly)) {
                        predicates.add(criteriaBuilder.notEqual(root.get("status"), ParcelPackageStatus.DELIVERED));
                    }
                    if (Objects.nonNull(requestSystem)) {
                        predicates.add(criteriaBuilder.equal(root.get("requestSystem"), requestSystem));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "warehouseId", "createdTime")
        );

        return cartons;
    }

    public void updateTracker(String trackingCode, String statusDescription) {
        ParcelPackage parcelPackage = findByTrackingCode(trackingCode);
        if (Objects.nonNull(parcelPackage)) {
            logger.debug("we found a package with tracking code {}, let's update its status to {}",
                    trackingCode, statusDescription);
            parcelPackage.setStatusDescription(statusDescription);
            saveOrUpdate(parcelPackage);
        }
        else {
            logger.debug("can't find any package with tracking code {}",
                    trackingCode);
        }
    }

    /**
     * Add tracking information from hualei system
     * @param warehouseId
     * @param order
     * @param productId
     * @param carrierId
     * @param carrierServiceLevelId
     * @param trackingCode
     * @param hualeiOrderId
     * @param length
     * @param width
     * @param height
     * @param weight
     * @return
     */
    public ParcelPackage addTracking(Long warehouseId,
                                     Order order,
                                     String productId,
                                     Long carrierId,
                                     Long carrierServiceLevelId,
                                     String trackingCode,
                                     String hualeiOrderId,
                                     double length,
                                     double width,
                                     double height,
                                     double weight) {
        Carrier carrier = Objects.isNull(carrierId) ? null :
                commonServiceRestemplateClient.getCarrierById(carrierId);
        CarrierServiceLevel carrierServiceLevel = Objects.isNull(carrierServiceLevelId) ? null :
                commonServiceRestemplateClient.getCarrierServiceLevelById(carrierServiceLevelId);
        HualeiConfiguration hualeiConfiguration =
                hualeiConfigurationService.findByWarehouse(warehouseId);
        HualeiShippingLabelFormatByProduct hualeiShippingLabelFormatByProduct =
                hualeiConfiguration.getHualeiShippingLabelFormatByProducts().stream().filter(
                        existingHualeiShippingLabelFormatByProduct ->
                                existingHualeiShippingLabelFormatByProduct.getProductId().equalsIgnoreCase(productId)
                ).findFirst().orElse(null);


        ParcelPackage parcelPackage = new ParcelPackage(
                warehouseId, order, carrier, carrierServiceLevel,
                hualeiConfiguration, hualeiShippingLabelFormatByProduct,
                trackingCode, hualeiOrderId,
                length, width, height, weight,
                ParcelPackageRequestSystem.HUALEI
        );

        return saveOrUpdate(parcelPackage);

    }


    public ParcelPackage addTracking(Long warehouseId,
                                     Order order,
                                     ParcelPackageCSVWrapper parcelPackageCSVWrapper,
                                     Long carrierId,
                                     Long carrierServiceLevelId) {
        Carrier carrier = Objects.isNull(carrierId) ? null :
                commonServiceRestemplateClient.getCarrierById(carrierId);
        CarrierServiceLevel carrierServiceLevel = Objects.isNull(carrierServiceLevelId) ? null :
                commonServiceRestemplateClient.getCarrierServiceLevelById(carrierServiceLevelId);

        ParcelPackage parcelPackage = new ParcelPackage(
                warehouseId, order, carrier, carrierServiceLevel,
                parcelPackageCSVWrapper.getTrackingCode(),
                parcelPackageCSVWrapper.getLength(),
                parcelPackageCSVWrapper.getWidth(),
                parcelPackageCSVWrapper.getHeight(),
                parcelPackageCSVWrapper.getWeight(),
                parcelPackageCSVWrapper.getDeliveryDays(),
                parcelPackageCSVWrapper.getRate(),
                ParcelPackageRequestSystem.MANUAL
        );

        return saveOrUpdate(parcelPackage);

    }

    private List<ParcelPackageCSVWrapper> loadParcelPackageData(File file) throws IOException {

        return fileService.loadData(file, ParcelPackageCSVWrapper.class);
    }
    public String saveParcelPackageData(Long warehouseId,
                                        File localFile) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearFileUploadMap();

        fileUploadProgress.put(fileUploadProgressKey, 0.0);
        fileUploadResultMap.put(fileUploadProgressKey, new ArrayList<>());

        List<ParcelPackageCSVWrapper> parcelPackageCSVWrappers = loadParcelPackageData(localFile);

        logger.debug("start to save {} order parcel package", parcelPackageCSVWrappers.size());

        fileUploadProgress.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {
            int totalCount = parcelPackageCSVWrappers.size();
            int index = 0;
            for (ParcelPackageCSVWrapper parcelPackageCSVWrapper : parcelPackageCSVWrappers) {

                try {
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));
                    Client client = Strings.isNotBlank(parcelPackageCSVWrapper.getClient()) ?
                            commonServiceRestemplateClient.getClientByName(warehouseId, parcelPackageCSVWrapper.getClient())
                            : null;
                    Long clientId = Objects.isNull(client) ? null : client.getId();
                    Order order = orderService.findByNumber(warehouseId, clientId, parcelPackageCSVWrapper.getOrder(), false);
                    if (Objects.isNull(order)) {
                        logger.debug("order {} is not created yet", parcelPackageCSVWrapper.getOrder());
                        throw OrderOperationException.raiseException("can't upload the tracking as the order " +
                                parcelPackageCSVWrapper.getOrder() + " is not in the system yet");
                    }
                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 0.5));

                    if (Strings.isBlank(parcelPackageCSVWrapper.getCarrier())) {

                        throw OrderOperationException.raiseException("can't upload the tracking information for order" +
                                parcelPackageCSVWrapper.getOrder() + ". carrier is required");
                    }
                    Carrier carrier = commonServiceRestemplateClient.getCarrierByName(
                            warehouseId, parcelPackageCSVWrapper.getCarrier());
                    if (Objects.isNull(carrier)) {

                        throw OrderOperationException.raiseException("can't upload the tracking information for order " +
                                parcelPackageCSVWrapper.getOrder() + ". carrier not found by name " +
                                parcelPackageCSVWrapper.getCarrier());
                    }

                    CarrierServiceLevel carrierServiceLevel =
                            Strings.isNotBlank(parcelPackageCSVWrapper.getCarrierServiceLevel()) ?
                                    commonServiceRestemplateClient.getCarrierServiceLevelByName(
                                            warehouseId, parcelPackageCSVWrapper.getCarrierServiceLevel()
                                    )
                                    :
                                    null;
                    addTracking(warehouseId, order,
                            parcelPackageCSVWrapper,
                            carrier.getId(),
                            Objects.isNull(carrierServiceLevel) ? null : carrierServiceLevel.getId()
                    );

                    fileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            parcelPackageCSVWrapper.toString(),
                            "success", ""
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process order tracking number upload file record: {}, \n error message: {}",
                            parcelPackageCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            parcelPackageCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    fileUploadResultMap.put(fileUploadProgressKey, fileUploadResults);
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


    public double getFileUploadProgress(String key) {
        return fileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getFileUploadResult(Long warehouseId, String key) {
        return fileUploadResultMap.getOrDefault(key, new ArrayList<>());
    }

    public void removeParcelPackage(Long warehouseId, Long id) {
        parcelPackageRepository.deleteById(id);
    }

    /**
     * Refresh the status of the hualei packages
     */
    public void refreshHualeiPackageStatus() {
        List<HualeiConfiguration> hualeiConfigurations = hualeiConfigurationService.listHualeiEnabledWarehouse();
        hualeiConfigurations.stream().filter(
                hualeiConfiguration -> Strings.isNotBlank(hualeiConfiguration.getGetPackageStatusProtocol()) &&
                        Strings.isNotBlank(hualeiConfiguration.getGetPackageStatusHost()) &&
                        Strings.isNotBlank(hualeiConfiguration.getGetPackageStatusPort()) &&
                                Strings.isNotBlank(hualeiConfiguration.getGetPackageStatusEndpoint())
        ).forEach(
                hualeiConfiguration -> refreshHualeiPackageStatusByWarehouse(hualeiConfiguration)
        );
    }

    private void refreshHualeiPackageStatusByWarehouse(HualeiConfiguration hualeiConfiguration) {
        logger.debug("start to refresh hualei package status for warehouse id {}",
                hualeiConfiguration.getWarehouseId());
        List<ParcelPackage> parcelPackages = findAll(hualeiConfiguration.getWarehouseId(),
                null, null, null, true, ParcelPackageRequestSystem.HUALEI);

        logger.debug("find {} packages that is shipped by hualei and not delivered yet",
                parcelPackages.size());
        logger.debug("we will fetch in batch, 20 packages in one batch");
        int count = 0;
        String[] trackingNumberArray = new String[20];

        // save the response of the package status from hualei
        List<HualeiTrackResponseData> hualeiTrackResponseDataList = new ArrayList<>();

        for (ParcelPackage parcelPackage : parcelPackages) {
            // add current package to the array
            trackingNumberArray[count] = parcelPackage.getTrackingCode();
            if (count == 19) {
                // ok, we already have 20 packages in the array, let's request the
                // status from hualei
                hualeiTrackResponseDataList.addAll(
                        hualeiShippingService.refreshHualeiPackageStatus(trackingNumberArray, hualeiConfiguration)
                );
            }
            count = (count + 1) % 20;
            // clear the old batch of tracking numbers
            if (count == 0) {
                trackingNumberArray = new String[20];
            }
        }
        if (count > 0) {

            hualeiTrackResponseDataList.addAll(
                hualeiShippingService.refreshHualeiPackageStatus(trackingNumberArray, hualeiConfiguration));
        }

        hualeiTrackResponseDataList.forEach(
                hualeiTrackResponseData -> {
                    logger.debug("start to set tracking number {}'s status to {}",
                            hualeiTrackResponseData.getTrackingNumber(),
                            hualeiTrackResponseData.getTrackContent());

                    ParcelPackage parcelPackage = findByTrackingCode(
                            hualeiConfiguration.getWarehouseId(),
                            hualeiTrackResponseData.getTrackingNumber()
                    );
                    if (Objects.nonNull(parcelPackage)) {
                        parcelPackage.setStatusDescription(hualeiTrackResponseData.getTrackContent());
                    }
                    saveOrUpdate(parcelPackage);
                }
        );
    }
}

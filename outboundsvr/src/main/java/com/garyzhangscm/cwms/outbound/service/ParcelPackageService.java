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
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.CartonRepository;
import com.garyzhangscm.cwms.outbound.repository.ParcelPackageRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@Service
public class ParcelPackageService  {
    private static final Logger logger = LoggerFactory.getLogger(ParcelPackageService.class);

    @Autowired
    private ParcelPackageRepository parcelPackageRepository;



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
                                       String trackingCode) {

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

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        return cartons;
    }

    public void updateTracker(String trackingCode, String status) {
        ParcelPackage parcelPackage = findByTrackingCode(trackingCode);
        if (Objects.nonNull(parcelPackage)) {
            logger.debug("we found a package with tracking code {}, let's update its status to {}",
                    trackingCode, status);
            parcelPackage.setStatus(status);
            saveOrUpdate(parcelPackage);
        }
        else {
            logger.debug("can't find any package with tracking code {}",
                    trackingCode);
        }
    }
}

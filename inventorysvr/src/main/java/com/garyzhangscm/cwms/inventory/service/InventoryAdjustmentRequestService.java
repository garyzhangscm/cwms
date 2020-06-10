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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryAdjustmentRequestRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 *
 */
@Service
public class InventoryAdjustmentRequestService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryAdjustmentRequestService.class);

    @Autowired
    private InventoryAdjustmentRequestRepository inventoryAdjustmentRequestRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;



    public InventoryAdjustmentRequest findById(Long id) {
        return findById(id, true);
    }
    public InventoryAdjustmentRequest findById(Long id, boolean includeDetails) {
        InventoryAdjustmentRequest inventoryAdjustmentRequest = inventoryAdjustmentRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory adjustment request not found by id: " + id));
        if (includeDetails) {
            loadAttribute(inventoryAdjustmentRequest);
        }
        return inventoryAdjustmentRequest;
    }

    public List<InventoryAdjustmentRequest> findAll() {
        return findAll(true);
    }

    public List<InventoryAdjustmentRequest> findAll(boolean includeDetails) {

        // Only return actual inventory
        List<InventoryAdjustmentRequest> inventoryAdjustmentRequests = inventoryAdjustmentRequestRepository.findAll();
        if (includeDetails && inventoryAdjustmentRequests.size() > 0) {
            loadAttribute(inventoryAdjustmentRequests);
        }
        return inventoryAdjustmentRequests;
    }
    public List<InventoryAdjustmentRequest> findAll(Long warehouseId,
                                                    String inventoryQuantityChangeType) {
        return findAll(warehouseId, inventoryQuantityChangeType, null, null,null, null,null, true);
    }


    public List<InventoryAdjustmentRequest> findAll(Long warehouseId) {
        return findAll(warehouseId, null, null, null, null,null, null, true);
    }

    public List<InventoryAdjustmentRequest> findAll(Long warehouseId,
                                                    String inventoryQuantityChangeType,
                                                    String status,
                                                    String itemName,
                                                    Long locationId,
                                                    String locationName,
                                                    Long inventoryId) {
        return findAll( warehouseId, inventoryQuantityChangeType, status, itemName, locationId, locationName,  inventoryId,true);
    }
    public List<InventoryAdjustmentRequest> findAll(Long warehouseId,
                                                    String inventoryQuantityChangeType,
                                                    String status,
                                                    String itemName,
                                                    Long locationId,
                                                    String locationName,
                                                    Long inventoryId,
                                                    boolean includeDetails) {
        List<InventoryAdjustmentRequest> inventoryAdjustmentRequests =  inventoryAdjustmentRequestRepository.findAll(
                (Root<InventoryAdjustmentRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {


                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(inventoryQuantityChangeType)) {
                        logger.debug("query by inventoryQuantityChangeType: {}", inventoryQuantityChangeType);
                        predicates.add(criteriaBuilder.equal(
                                root.get("inventoryQuantityChangeType"), InventoryQuantityChangeType.valueOf(inventoryQuantityChangeType)));
                    }

                    if (StringUtils.isNotBlank(itemName)) {
                        Join<InventoryAdjustmentRequest, Item> joinItem = root.join("item", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                    }
                    if (StringUtils.isNotBlank(status)) {
                        predicates.add(criteriaBuilder.equal(root.get("status"), InventoryAdjustmentRequestStatus.valueOf(status)));
                    }

                    if (Objects.nonNull(locationId)) {

                        predicates.add(criteriaBuilder.equal(root.get("locationId"), locationId));
                    }
                    else if (StringUtils.isNotBlank(locationName)) {

                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, locationName);
                        predicates.add(criteriaBuilder.equal(root.get("locationId"), location.getId()));
                    }
                    if (Objects.nonNull(inventoryId)) {

                        predicates.add(criteriaBuilder.equal(root.get("inventoryId"), inventoryId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (includeDetails && inventoryAdjustmentRequests.size() > 0) {
            loadAttribute(inventoryAdjustmentRequests);
        }

        return inventoryAdjustmentRequests;
    }


    public InventoryAdjustmentRequest save(InventoryAdjustmentRequest inventoryAdjustmentRequest) {
        return inventoryAdjustmentRequestRepository.save(inventoryAdjustmentRequest);
    }





    public void loadAttribute(List<InventoryAdjustmentRequest> inventoryAdjustmentRequests) {
        for(InventoryAdjustmentRequest inventoryAdjustmentRequest : inventoryAdjustmentRequests) {
            loadAttribute(inventoryAdjustmentRequest);
        }
    }

    public void loadAttribute(InventoryAdjustmentRequest inventoryAdjustmentRequest) {

        // Load location information
        if (inventoryAdjustmentRequest.getLocationId() != null) {
            inventoryAdjustmentRequest.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventoryAdjustmentRequest.getLocationId()));
        }



        // load the unit of measure details for the packate types
        inventoryAdjustmentRequest.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure ->
                itemUnitOfMeasure.setUnitOfMeasure(commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId())));



    }


    public void writeInventoryAdjustRequest(Inventory inventory, Long quantity, InventoryQuantityChangeType inventoryQuantityChangeType,
                                            String documentNumber, String comment) {


        InventoryAdjustmentRequest inventoryAdjustmentRequest
                = new InventoryAdjustmentRequest(inventory, quantity, inventoryQuantityChangeType, userService.getCurrentUserName(),
                                                 documentNumber, comment);
        // logger.debug("Will persist the adjust request: {}", inventoryAdjustmentRequest);
        save(inventoryAdjustmentRequest);

        logger.debug("we will lock the location {} as well",
                warehouseLayoutServiceRestemplateClient.getLocationById(inventoryAdjustmentRequest.getLocationId()).getName());

        lockLocation(inventoryAdjustmentRequest);

    }

    public InventoryAdjustmentRequest processInventoryAdjustmentRequest(Long id, Boolean approved, String comment) {
        if (approved) {
            return approveInventoryAdjustRequest(id, comment);
        }
        else {
            return denyInventoryAdjustRequest(id, comment);
        }
    }

    private InventoryAdjustmentRequest approveInventoryAdjustRequest(Long id, String comment) {
        return processInventoryAdjustRequest(id, InventoryAdjustmentRequestStatus.APPROVED, comment);

    }

    public InventoryAdjustmentRequest denyInventoryAdjustRequest(Long id, String comment) {
        return processInventoryAdjustRequest(id, InventoryAdjustmentRequestStatus.DENIED, comment);

    }

    private InventoryAdjustmentRequest processInventoryAdjustRequest(Long id,
                                                                     InventoryAdjustmentRequestStatus inventoryAdjustmentRequestStatus,
                                                                     String comment) {
        InventoryAdjustmentRequest inventoryAdjustmentRequest = findById(id);
        inventoryAdjustmentRequest.setStatus(inventoryAdjustmentRequestStatus);
        inventoryAdjustmentRequest.setProcessedByDateTime(LocalDateTime.now());
        inventoryAdjustmentRequest.setProcessedByUsername(userService.getCurrentUserName());
        // If the user pass in the comment, we will override the comment
        // that is generated during requesting. Otherwise, we will keep the
        // original comment
        if (StringUtils.isNotBlank(comment)) {
            inventoryAdjustmentRequest.setComment(comment);
        }
        inventoryAdjustmentRequest = save(inventoryAdjustmentRequest);

        // release the inventory
        if (Objects.nonNull(inventoryAdjustmentRequest.getInventoryId())) {
            inventoryService.releaseInventory(inventoryAdjustmentRequest.getInventoryId());
        }

        // Sent to MQ so that we can process the actual inventory adjust later
        // in InventoryService
        // in this case, we will release the location's lock after the inventory is adjust
        if (inventoryAdjustmentRequestStatus.equals(InventoryAdjustmentRequestStatus.APPROVED)) {
            // kafkaSender.send(inventoryAdjustmentRequest);
            // when we use MQ, we will lose the current user information.
            // Lots of process like inventory activities records are depends on
            // the current user. Let's directly call the processInventoryAdjustRequest
            // to actually change the inventory
            inventoryService.processInventoryAdjustRequest(inventoryAdjustmentRequest);
        }
        else {
            // release the location's lock right after the request is denied
            releaseLocationLock(inventoryAdjustmentRequest);
        }
        return inventoryAdjustmentRequest;

    }

    public void lockLocation(InventoryAdjustmentRequest inventoryAdjustmentRequest) {

        warehouseLayoutServiceRestemplateClient.lockLocation(inventoryAdjustmentRequest.getLocationId());

    }

    public void releaseLocationLock(InventoryAdjustmentRequest inventoryAdjustmentRequest) {

        // Check if we have any open adjust request against the location.
        // if so, we will lock the location
        if (findAll(inventoryAdjustmentRequest.getWarehouseId(),
                null,
                InventoryAdjustmentRequestStatus.PENDING.toString(),
                null,
                inventoryAdjustmentRequest.getLocationId(),null,null, false).size() == 0) {

            warehouseLayoutServiceRestemplateClient.releaseLocationLock(inventoryAdjustmentRequest.getLocationId());
        };
    }



}
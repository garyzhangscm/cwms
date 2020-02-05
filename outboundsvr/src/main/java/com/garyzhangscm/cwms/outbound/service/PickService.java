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

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class PickService {
    private static final Logger logger = LoggerFactory.getLogger(PickService.class);

    @Autowired
    private PickRepository pickRepository;
    @Autowired
    private ShippingStageAreaConfigurationService shippingStageAreaConfigurationService;
    @Autowired
    private PickMovementService pickMovementService;
    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService;

    @Autowired
    private PickListService pickListService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public Pick findById(Long id, boolean loadDetails) {
        Pick pick = pickRepository.findById(id).orElse(null);
        if (pick != null && loadDetails) {
            loadOrderAttribute(pick);
        }
        return pick;
    }

    public Pick findById(Long id) {
        return findById(id, true);
    }


    public List<Pick> findAll(String number, Long workOrderLineId, String workOrderLineIds,  boolean loadDetails) {

        List<Pick> picks =  pickRepository.findAll(
                (Root<Pick> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (workOrderLineId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderLineId"), workOrderLineId));
                    }
                    else if (!StringUtils.isBlank(workOrderLineIds)){

                        CriteriaBuilder.In<Long> inWorkOrderLineIds = criteriaBuilder.in(root.get("workOrderLineId"));
                        for(String id : workOrderLineIds.split(",")) {
                            inWorkOrderLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderLineIds));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (picks.size() > 0 && loadDetails) {
            loadOrderAttribute(picks);
        }
        return picks;
    }

    public List<Pick> findAll(String number, Long workOrderLineId, String workOrderLineIds) {
        return findAll(number, workOrderLineId, workOrderLineIds, true);
    }

    public Pick findByNumber(String number, boolean loadDetails) {
        Pick pick = pickRepository.findByNumber(number);
        if (pick != null && loadDetails) {
            loadOrderAttribute(pick);
        }
        return pick;
    }

    public Pick findByNumber(String number) {
        return findByNumber(number, true);
    }


    public void loadOrderAttribute(List<Pick> picks) {
        for (Pick pick : picks) {
            loadOrderAttribute(pick);
        }
    }

    public void loadOrderAttribute(Pick pick) {
        // Load the details for client and supplier informaiton
        if (pick.getSourceLocationId() != null && pick.getSourceLocation() == null) {
            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
        }
        if (pick.getDestinationLocationId() != null && pick.getDestinationLocation() == null) {
            pick.setDestinationLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getDestinationLocationId()));
        }

        // Load the item and inventory status information for each lines
        if (pick.getItemId() != null && pick.getItem() == null) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }

        // load pick's inventory
        if (pick.getShipmentLine().getOrderLine().getInventoryStatusId() != null &&
                pick.getShipmentLine().getOrderLine().getInventoryStatus() == null) {
            pick.getShipmentLine().getOrderLine().setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            pick.getShipmentLine().getOrderLine().getInventoryStatusId()
                    ));
        }


    }



    public Pick save(Pick pick) {
        Pick newPick = pickRepository.save(pick);
        loadOrderAttribute(newPick);
        return newPick;
    }

    public Pick saveOrUpdate(Pick pick) {
        if (pick.getId() == null && findByNumber(pick.getNumber()) != null) {
            pick.setId(findByNumber(pick.getNumber()).getId());
        }
        return save(pick);
    }


    public void delete(Pick pick) {
        pickRepository.delete(pick);
    }

    public void delete(Long id) {
        pickRepository.deleteById(id);
    }

    public List<Pick> getOpenPicksByItemId(Long itemId){

        return pickRepository.getOpenPicksByItemId(itemId);
    }

    public String getNextPickNumber() {
        return commonServiceRestemplateClient.getNextNumber("pick-number");

    }

    public List<Pick> cancelPicks(String pickIds) {

        return Arrays.stream(pickIds.split(",")).mapToLong(Long::parseLong).mapToObj(this::cancelPick).collect(Collectors.toList());
    }

    public Pick cancelPick(Long id) {
        return cancelPick(findById(id));
    }

    public Pick cancelPick(Pick pick) {
        return cancelPick(pick, pick.getQuantity() - pick.getPickedQuantity());

    }
    public Pick cancelPick(Pick pick, Long cancelQuantity) {
        if (pick.getStatus().equals(PickStatus.COMPLETED)) {
            throw new GenericException(10000, "Can't cancel pick that is already cancelled");
        }


        // we have nothing left to cancel
        if (cancelQuantity == 0) {
            return pick;
        }


        // return the open quantity back to the shipment line
        shipmentLineService.cancelPickQuantity(pick.getShipmentLine(), cancelQuantity);

        pick.setQuantity(pick.getQuantity() - cancelQuantity);
        if (pick.getQuantity() == 0) {

            // we cancelled the whole pick. let's mark
            // the pick with status 'Cancelled'
            pick.setStatus(PickStatus.CANCELLED);
        }
        return saveOrUpdate(pick);

    }


    public Pick generatePick(InventorySummary inventorySummary, Long quantity) {
        logger.debug("Start to generate pick");
        Pick pick = new Pick();
        pick.setItem(inventorySummary.getItem());
        pick.setItemId(inventorySummary.getItem().getId());
        pick.setSourceLocation(inventorySummary.getLocation());
        pick.setSourceLocationId(inventorySummary.getLocationId());
        pick.setQuantity(quantity);
        pick.setPickedQuantity(0L);
        pick.setNumber(getNextPickNumber());
        pick.setStatus(PickStatus.PENDING);

        return pick;
    }

    @Transactional
    public Pick generatePick(InventorySummary inventorySummary, ShipmentLine shipmentLine, Long quantity) {
        Pick pick = generatePick(inventorySummary, quantity);

        pick.setShipmentLine(shipmentLine);
        pick.setWarehouseId(shipmentLine.getWarehouseId());

        // Setup the destination, get from ship staging area


        Location stagingLocation = getDestinationLocationForPick(shipmentLine, pick);
        pick.setDestinationLocation(stagingLocation);
        pick.setDestinationLocationId(stagingLocation.getId());

        Pick savedPick = save(pick);

        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());


        // Let's see if we can group the pick either
        // 1. into an existing pick list
        // 2. or create a new picking list so other picks can be grouped
        processPickList(savedPick);

        return findById(savedPick.getId());
    }


    @Transactional
    public Pick generatePick(WorkOrder workOrder, InventorySummary inventorySummary, WorkOrderLine workOrderLine, Long quantity) {
        Pick pick = generatePick(inventorySummary, quantity);

        pick.setWorkOrderLineId(workOrderLine.getId());
        pick.setWarehouseId(workOrder.getWarehouseId());

        // Setup the destination, get from ship staging area

        logger.debug("get pick's destination for work order:\n{}", workOrder);
        Location stagingLocation = getDestinationLocationForPick(workOrder, pick);
        pick.setDestinationLocation(stagingLocation);
        pick.setDestinationLocationId(stagingLocation.getId());

        Pick savedPick = save(pick);

        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());


        // Let's see if we can group the pick either
        // 1. into an existing pick list
        // 2. or create a new picking list so other picks can be grouped
        processPickList(savedPick);

        return findById(savedPick.getId());
    }

    @Transactional
    public void processPickList(Pick pick) {
        try {
            PickList pickList = pickListService.getPickList(pick);
            pick.setPickList(pickList);
            saveOrUpdate(pick);
        }
        catch (Exception ex) {
            logger.debug("Exception while trying group the pick {} to list\n{}"
                         , pick.getNumber(), ex.getMessage());
        }
    }

    public Pick generatePick(InventorySummary inventorySummary, ShortAllocation shortAllocation, Long quantity) {
        Pick pick = generatePick(inventorySummary, quantity);

        pick.setShortAllocation(shortAllocation);
        pick.setWarehouseId(shortAllocation.getWarehouseId());

        // Setup the destination, get from ship staging area


        Location destinationLocation = getDestinationLocationForPick(pick);
        pick.setDestinationLocation(destinationLocation);
        pick.setDestinationLocationId(destinationLocation.getId());

        Pick savedPick = save(pick);

        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());

        return findById(savedPick.getId());
    }

    // For work order, the destination is always the inbound stage for the production line
    private Location getDestinationLocationForPick(WorkOrder workOrder, Pick pick) {
        logger.debug("workOrder.getProductionLine(): {}", workOrder.getProductionLine());
        logger.debug("workOrder.getProductionLine().getName(): {}", workOrder.getProductionLine().getName());
        logger.debug("workOrder.getProductionLine().getInboundStageLocation(): {}", workOrder.getProductionLine().getInboundStageLocation());
        logger.debug("workOrder.getProductionLine().getInboundStageLocation().getName(): {}", workOrder.getProductionLine().getInboundStageLocation().getName());
        return workOrder.getProductionLine().getInboundStageLocation();

    }
    private Location getDestinationLocationForPick(ShipmentLine shipmentLine, Pick pick) {
        ShippingStageAreaConfiguration shippingStageAreaConfiguration;
        logger.debug(">> Try to get ship stage for the pick");
        logger.debug(">> shipmentLine.getOrderLine().getOrder().getStageLocationGroupId(): {}",
                shipmentLine.getOrderLine().getOrder().getStageLocationGroupId());
        if (shipmentLine.getOrderLine().getOrder().getStageLocationGroupId() == null) {

            shippingStageAreaConfiguration = shippingStageAreaConfigurationService.getShippingStageArea(pick);
        }
        else {

            shippingStageAreaConfiguration = shippingStageAreaConfigurationService.getShippingStageArea(
                    pick, shipmentLine.getOrderLine().getOrder().getStageLocationGroupId());
        }
        logger.debug("OK, we find the ship stage configuration: {}", shippingStageAreaConfiguration.getSequence());
        Location stagingLocation = shippingStageAreaConfigurationService.reserveShippingStageLocation(shippingStageAreaConfiguration, pick);

        logger.debug("Bingo, we got the ship stage location: {}", stagingLocation.getName());
        return stagingLocation;

    }


    private Location getDestinationLocationForPick(Pick pick) {
        return emergencyReplenishmentConfigurationService.getEmergencyReplenishmentDestination(pick);

    }


    // Once we have the source and destination of the pick, we will need to setup the movement path of
    // the pick. THe movement path will guide the user to drop in different hops when moving inventory
    // from the source to the destination
    private void setupMovementPath(Pick pick) {
        List<MovementPath> movementPaths = inventoryServiceRestemplateClient.getPickMovementPath(pick);

        if (movementPaths.size() == 0) {
            // No hop area / location defined
            return;
        }

        // Loop through each configuraion until we can find a good hop chain from the source to the destination
        for(MovementPath movementPath : movementPaths) {
            if (setupMovementPath(pick, movementPath)) {
                // OK, we are able to setup the hop locaiton / areas based upon this movement configuration
                // let's return
                break;
            }
        }

    }
    private boolean setupMovementPath(Pick pick, MovementPath movementPath) {
        List<MovementPathDetail> movementPathDetails = movementPath.getMovementPathDetails();

        List<PickMovement> pickMovements = new ArrayList<>();
        try {
            for (MovementPathDetail movementPathDetail : movementPathDetails) {
                pickMovements.add(getPickMovement(pick, movementPathDetail));
            }
        }
        catch(Exception exception) {
            logger.debug("exception when we try to setup the movement path for the pick\n Pick: {}\n Movement Path: {}",
            pick, movementPath);
            return false;
        }

        if (pickMovements.size() == 0) {
            return false;
        }
        // Save the pick movement
        pickMovements.stream().forEach(pickMovement -> pickMovementService.save(pickMovement));
        return  true;



    }
    private PickMovement getPickMovement(Pick pick, MovementPathDetail movementPathDetail) {

        if (movementPathDetail.getHopLocationId() != null) {
            // OK we are suppose to reserve a location by the specific ID.
            // Let's see if we can reserve this typical location

            Location hopLocation = warehouseLayoutServiceRestemplateClient.reserveLocation(movementPathDetail.getHopLocationId(),
                    getReserveCode(pick, movementPathDetail), pick.getSize(), pick.getQuantity(), 1);
            return new PickMovement(pick, hopLocation, movementPathDetail.getSequence());
        }
        else if (movementPathDetail.getHopLocationGroupId() != null) {
            // OK we are suppose to reserve a location from a group
            // Let's see if we can reserve any location from a typical group

            Location hopLocation = warehouseLayoutServiceRestemplateClient.reserveLocationFromGroup(movementPathDetail.getHopLocationGroupId(),
                    getReserveCode(pick, movementPathDetail), pick.getSize(), pick.getQuantity(), 1);
            return new PickMovement(pick, hopLocation, movementPathDetail.getSequence());
        }
        throw new GenericException(10000, "Can't reserve any location by the movement path detail configuration: " + movementPathDetail.getSequence());
    }

    private String getReserveCode(Pick pick, MovementPathDetail movementPathDetail) {
        switch (movementPathDetail.getStrategy()) {
            case BY_ORDER:
                return pick.getOrderNumber();
            case BY_CUSTOMER:
                return pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomer().getName();
        }
        throw new GenericException(10000, "not possible to get reserve code for pick from the strategy: " + movementPathDetail.getStrategy());
    }


    public Pick confirmPick(Pick pick) throws IOException {
        if (pick.getPickMovements().size() == 0) {
            return confirmPick(pick, pick.getDestinationLocation());
        }
        else {
            return confirmPick(pick, pick.getPickMovements().get(0).getLocation());
        }
    }
    public Pick confirmPick(Long pickId, Long nextLocationId) throws IOException {
        if (nextLocationId != null) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
            if (nextLocation != null) {
                return confirmPick(findById(pickId), nextLocation);
            }
            else {
                throw new GenericException(10000,
                        "Can't confirm the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else {
            return confirmPick(findById(pickId));
        }
    }
    public Pick confirmPick(Pick pick, Location nextLocation) throws IOException {

        List<Inventory> pickableInventories = inventoryServiceRestemplateClient.getInventoryForPick(pick);
        logger.debug(" Get {} valid inventory for pick {}",
                pickableInventories.size(), pick.getNumber());
        pickableInventories.stream().forEach(System.out::print);
        Long quantityToBePicked = pick.getQuantity() - pick.getPickedQuantity();
        logger.debug(" start to pick with quantity {}",quantityToBePicked);
        Iterator<Inventory> inventoryIterator = pickableInventories.iterator();
        while(quantityToBePicked > 0 && inventoryIterator.hasNext()) {
            Inventory inventory = inventoryIterator.next();
            logger.debug(" pick from inventory {}", quantityToBePicked, inventory.getLpn());
            Long pickedQuantity = confirmPick(inventory, pick, nextLocation);
            logger.debug(" >> we actually picked {} from the inventory", pickedQuantity);
            quantityToBePicked -= pickedQuantity;
            logger.debug(" >> there's {} left in the pick work", quantityToBePicked);
        }


        // Get the latest pick information
        return findById(pick.getId());
    }

    public List<Pick> getPicksByShipment(Long shipmentId){
        return pickRepository.getPicksByShipmentId(shipmentId);
    }

    public Long confirmPick(Inventory inventory, Pick pick, Location nextLocation) throws IOException {
        return confirmPick(inventory, pick, nextLocation, "");
    }
    public Long confirmPick(Inventory inventory, Pick pick, Location nextLocation, String newLpn) throws IOException {
        if (!match(inventory, pick)) {
            throw new GenericException(10000, "inventory can't be picked for the pick. Attribute discrepancy found");
        }

        logger.debug("Start to pick from inventory\n inventory quantity: {} \n pick's quantity {} / {} "
                     , inventory.getQuantity(), pick.getPickedQuantity(), pick.getQuantity());
        Long quantityToBePick = Math.min(inventory.getQuantity(), pick.getQuantity() - pick.getPickedQuantity());

        logger.debug(" Will pick {} from the inventory", quantityToBePick);
        boolean pickWholeInventory = quantityToBePick.equals(inventory.getQuantity());

        // If we are not to pick the whole inventory, we will split the original inventory
        // into 2 LPN and only move the right LPN for the pick
        Inventory inventoryToBePicked;
        if (pickWholeInventory) {
            inventoryToBePicked = inventory;
        }
        else {
            // We pick partial quantity from the inventory, Let's split it
            // and give it a new LPN
            if (StringUtils.isBlank(newLpn)) {
                newLpn = commonServiceRestemplateClient.getNextNumber("lpn");
            }
            List<Inventory> inventories = inventoryServiceRestemplateClient.split(inventory, newLpn, quantityToBePick);
            if (inventories.size() != 2) {
                throw new GenericException(10000, "Inventory split for pick error! Inventory is not split into 2");
            }
            inventoryToBePicked = inventories.get(1);
        }
        logger.debug("Will pick from inventory {} ", inventoryToBePicked.getLpn());

        // Move the inventory to the next location for pick
        // Move the inventory to the next location
        inventoryServiceRestemplateClient.moveInventory(inventoryToBePicked, pick, nextLocation);
        // update the quantity in the pick
        logger.debug(" change the picked quantity from {} to {}",
                pick.getPickedQuantity(), (pick.getPickedQuantity() + quantityToBePick));
        pick.setPickedQuantity(pick.getPickedQuantity() + quantityToBePick);
        saveOrUpdate(pick);

        // Let's update the list if the pick belongs to any list
        pickListService.processPickConfirmed(pick);
        return quantityToBePick;

    }

    private boolean match(Inventory inventory, Pick pick) {
        logger.debug("check if the inventory match with the pick");
        logger.debug("========            Inventory   ===========");
        logger.debug(inventory.toString());
        logger.debug("========            pick   ===========");
        logger.debug(pick.toString());
        if (!inventory.getItem().equals(pick.getItem())) {
            logger.debug("Inventory doesn't match with Pick. \n >> Inventory's item: {} \n >> Pick's item: {}",
                    inventory.getItem().getName(), pick.getItem().getName());
            return false;
        }
        if (!inventory.getInventoryStatus().equals(pick.getInventoryStatus())) {
            logger.debug("Inventory status doesn't match with Pick. \n >> Inventory's status: {} \n >> Pick's status: {}",
                    inventory.getInventoryStatus().getName(), pick.getInventoryStatus().getName());
            return false;
        }
        return true;
    }


    public Pick unpick(Long id, Long unpickQuantity) {
        return unpick(findById(id), unpickQuantity);
    }

    // unpick will
    // 1. reset the picked quantity by deduct
    //    the unpickedquantity from the picked quantity
    // 2. return the unpicked quantity back to the shipment's open quantity
    //    so that we can re-allocate
    public Pick unpick(Pick pick, Long unpickedQuantity) {
        pick.setPickedQuantity(pick.getPickedQuantity() - unpickedQuantity);

        return cancelPick(pick, unpickedQuantity);
    }

}

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
import com.garyzhangscm.cwms.outbound.clients.WorkOrderServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ReplenishmentException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
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
    private CancelledPickService cancelledPickService;
    @Autowired
    private ShortAllocationService shortAllocationService;
    @Autowired
    private CartonizationService cartonizationService;

    @Autowired
    private PickListService pickListService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;

    public Pick findById(Long id, boolean loadDetails) {
        Pick pick = pickRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("pick not found by id: " + id));
        if (loadDetails) {
            loadAttribute(pick);
        }
        return pick;
    }

    public Pick findById(Long id) {
        return findById(id, true);
    }


    public List<Pick> findAll(String number, Long orderId, Long shipmentId, Long waveId,
                              Long  listId, Long cartonizationId,  String ids,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds,
                              Long shortAllocationId, boolean loadDetails) {

        List<Pick> picks =  pickRepository.findAll(
                (Root<Pick> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (Objects.nonNull(orderId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }
                    if (Objects.nonNull(shipmentId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment= joinShipmentLine.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));

                    }
                    if (Objects.nonNull(waveId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Wave> joinWave = joinShipmentLine.join("wave", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWave.get("id"), waveId));

                    }
                    if (Objects.nonNull(listId)) {
                        logger.debug("Start to find pick by list id: {}", listId);
                        Join<Pick, PickList> joinPickList = root.join("pickList", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPickList.get("id"), listId));

                    }
                    if (Objects.nonNull(cartonizationId)) {
                        Join<Pick, Cartonization> joinCartonization = root.join("cartonization", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinCartonization.get("id"), cartonizationId));

                    }
                    if (StringUtils.isNotBlank(ids)) {

                        CriteriaBuilder.In<Long> inIds = criteriaBuilder.in(root.get("id"));
                        for(String id : ids.split(",")) {
                            inIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inIds));
                    }

                    if (Objects.nonNull(itemId)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    if (Objects.nonNull(sourceLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), sourceLocationId));
                    }
                    if (Objects.nonNull(destinationLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), destinationLocationId));
                    }
                    if (Objects.nonNull(shortAllocationId )) {
                        Join<Pick, ShortAllocation> joinShortAllocation = root.join("shortAllocation", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShortAllocation.get("id"), shortAllocationId));
                    }

                    if (Objects.nonNull(workOrderLineId)) {
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
            loadAttribute(picks);
        }
        return picks;
    }

    public List<Pick> findAll(String number, Long orderId, Long shipmentId,Long waveId,
                              Long  listId, Long  cartonizationId,  String ids,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds,
                              Long shortAllocationId) {
        return findAll(number, orderId,shipmentId, waveId, listId, cartonizationId, ids,
                itemId, sourceLocationId, destinationLocationId,
                workOrderLineId, workOrderLineIds, shortAllocationId, true);
    }

    public Pick findByNumber(String number, boolean loadDetails) {
        Pick pick = pickRepository.findByNumber(number);
        if (pick != null && loadDetails) {
            loadAttribute(pick);
        }
        return pick;
    }

    public List<Pick> findByOrder(Order order) {
        return findAll(null, order.getId(), null,
                null, null,  null,null, null, null, null,
                null, null, null);
    }

    public Pick findByNumber(String number) {
        return findByNumber(number, true);
    }


    public void loadAttribute(List<Pick> picks) {
        for (Pick pick : picks) {
            loadAttribute(pick);
        }
    }

    public void loadAttribute(Pick pick) {
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

        // load pick's inventory status for
        if (pick.getInventoryStatusId() != null &&
                pick.getInventoryStatus() == null) {
            pick.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            pick.getInventoryStatusId()
                    ));
        }


    }



    public Pick save(Pick pick) {
        Pick newPick = pickRepository.save(pick);
        loadAttribute(newPick);
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

        return Arrays.stream(pickIds.split(","))
                .mapToLong(Long::parseLong).mapToObj(this::cancelPick).collect(Collectors.toList());
    }

    public Pick cancelPick(Long id) {
        return cancelPick(findById(id));
    }

    public Pick cancelPick(Pick pick) {
        return cancelPick(pick, pick.getQuantity() - pick.getPickedQuantity());

    }
    public Pick cancelPick(Pick pick, Long cancelledQuantity) {
        if (pick.getStatus().equals(PickStatus.COMPLETED)) {
            throw PickingException.raiseException("Can't cancel pick that is already cancelled");
        }

        // we have nothing left to cancel
        if (cancelledQuantity == 0) {
            return pick;
        }

        // return the open quantity back to the shipment line
        // shipmentLineService.cancelPickQuantity(pick.getShipmentLine(), cancelQuantity);
        logger.debug("pick.getShipmentLine() != null : {}", pick.getShipmentLine() != null);
        logger.debug("pick.getShortAllocation() != null : {}", pick.getShortAllocation() != null);
        logger.debug("pick.getWorkOrderLineId() != null : {}", pick.getWorkOrderLineId() != null);
        if (pick.getShipmentLine() != null) {
            shipmentLineService.registerPickCancelled(pick.getShipmentLine(), cancelledQuantity);
        }
        else if (pick.getShortAllocation() != null) {
            shortAllocationService.registerPickCancelled(pick.getShortAllocation(), cancelledQuantity);
        }
        else if (pick.getWorkOrderLineId() != null) {
            workOrderServiceRestemplateClient.registerPickCancelled(pick.getWorkOrderLineId(), cancelledQuantity);
        }

        // Save the data to cancelled pick table
        cancelledPickService.registerPickCancelled(pick, cancelledQuantity);

        pick.setQuantity(pick.getQuantity() - cancelledQuantity);
        if (pick.getQuantity() == 0) {
            // There's nothing left on the picks, let's remove it.
            // We can find the history in the cancelled pick table
            delete(pick);
            return pick;
        }
        else {
            return saveOrUpdate(pick);
        }

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
        pick.setInventoryStatusId(inventorySummary.getInventoryStatus().getId());

        return pick;
    }



    @Transactional
    public Pick generatePick(InventorySummary inventorySummary, ShipmentLine shipmentLine, Long quantity) {
        Pick pick = generatePick(inventorySummary, quantity);

        pick.setShipmentLine(shipmentLine);
        pick.setWarehouseId(shipmentLine.getWarehouseId());
        pick.setPickType(PickType.OUTBOUND);

        // Setup the destination, get from ship staging area


        Location stagingLocation = getDestinationLocationForPick(shipmentLine, pick);
        pick.setDestinationLocation(stagingLocation);
        pick.setDestinationLocationId(stagingLocation.getId());

        Pick savedPick = save(pick);

        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());


        processCartonization(savedPick);

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
        pick.setPickType(PickType.WORK_ORDER);

        // Setup the destination, get from ship staging area

        logger.debug("get pick's destination for work order:\n{}", workOrder);
        Long stagingLocationId = getDestinationLocationIdForPick(workOrder);
        pick.setDestinationLocationId(stagingLocationId);

        Pick savedPick = save(pick);

        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());


        processCartonization(savedPick);
        // Let's see if we can group the pick either
        // 1. into an existing pick list
        // 2. or create a new picking list so other picks can be grouped
        processPickList(savedPick);

        return findById(savedPick.getId());
    }

    private void processCartonization(Pick pick) {
        logger.debug(">> Start to process cartonization for pick: {}", pick);
        Cartonization cartonization = cartonizationService.processCartonization(pick);
        if (Objects.nonNull(cartonization)){
            // OK, we got a suitable cartonization, let's assign it to the pick
            pick.setCartonization(cartonization);
        }
        saveOrUpdate(pick);


    }
    private void processPickList(Pick pick) {
        try {
            logger.debug("Start to find pick list candidate");
            PickList pickList = pickListService.processPickList(pick);
            logger.debug("We will assign pick list {} to the current {}", pickList,
                    (Objects.nonNull(pick.getCartonization()) ? "Cartonization" : "Pick"));

            if (Objects.nonNull(pick.getCartonization())) {
                cartonizationService.processPickList(pick.getCartonization(), pickList);
            }
            pick.setPickList(pickList);
            saveOrUpdate(pick);
        }
        catch (GenericException ex) {
            logger.debug("Exception while trying group the pick {} to list\n{} / data: {}"
                         , pick.getNumber(), ex.getMessage(), ex.getData());
        }
    }

    public Pick generatePick(InventorySummary inventorySummary, ShortAllocation shortAllocation, Long quantity) {
        Pick pick = generatePick(inventorySummary, quantity);

        pick.setShortAllocation(shortAllocation);
        pick.setWarehouseId(shortAllocation.getWarehouseId());
        pick.setPickType(PickType.EMERGENCY_REPLENISHMENT);

        // Setup the destination, get from ship staging area
        Location destinationLocation = getDestinationLocationForPick(pick);
        if (Objects.isNull(destinationLocation)) {
            throw ReplenishmentException.raiseException("Can't find any destination location for the replenishment");
        }
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
    private Long getDestinationLocationIdForPick(WorkOrder workOrder) {
        if (workOrder.getProductionLine().getInboundStageLocationId() != null) {
            logger.debug("inbound stage location ID {} is setup for the production line {} ",
                    workOrder.getProductionLine().getInboundStageLocationId(), workOrder.getProductionLine().getName() );
            return workOrder.getProductionLine().getInboundStageLocationId();
        }
        else if (workOrder.getProductionLine().getInboundStageLocation() != null) {
            logger.debug("inbound stage location {} is setup for the production line {} ",
                    workOrder.getProductionLine().getInboundStageLocation().getName(),
                    workOrder.getProductionLine().getName() );
            return workOrder.getProductionLine().getInboundStageLocation().getId();
        }
        throw PickingException.raiseException("Can't get inbound location for the work order: " + workOrder.getNumber());

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
        throw PickingException.raiseException("Can't reserve any location by the movement path detail configuration: " + movementPathDetail.getSequence());
    }

    private String getReserveCode(Pick pick, MovementPathDetail movementPathDetail) {
        switch (movementPathDetail.getStrategy()) {
            case BY_ORDER:
                return pick.getOrderNumber();
            case BY_CUSTOMER:
                return pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomer().getName();
        }
        throw PickingException.raiseException("not possible to get reserve code for pick from the strategy: " + movementPathDetail.getStrategy());
    }


    /**
     * Confirm pick. No destination location is passed in, we will move the picked inventory
     * according to the movement path
     * @param pick The pick to be confirmed
     * @param quantity The quantity that will be picked
     * @return
     */
    public Pick confirmPick(Pick pick, Long quantity) {
        if (pick.getPickMovements().size() == 0) {
            return confirmPick(pick, quantity, pick.getDestinationLocation());
        }
        else {
            return confirmPick(pick, quantity, pick.getPickMovements().get(0).getLocation());
        }
    }
    public Pick confirmPick(Long pickId, Long quantity, Long nextLocationId)  {
        if (nextLocationId != null) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
            if (nextLocation != null) {
                return confirmPick(findById(pickId), quantity, nextLocation);
            }
            else {
                throw PickingException.raiseException(
                        "Can't confirm the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else {
            return confirmPick(findById(pickId), quantity);
        }
    }
    public Pick confirmPick(Pick pick, Long quantity, Location nextLocation)   {

        // make sure we are not over pick. At this moment, over pick is not allowed
        // If the quantity is not passed in, we will pick the whole quantity that is still left
        Long quantityToBePicked = quantity == null ? pick.getQuantity() - pick.getPickedQuantity() : quantity;
        if (quantityToBePicked <= 0 ||  quantityToBePicked > pick.getQuantity() - pick.getPickedQuantity()) {
            throw PickingException.raiseException("Over pick is not allowed. Try to pick: " + quantityToBePicked +
                    ", Quantity left: " + (pick.getQuantity() - pick.getPickedQuantity()));
        }
        List<Inventory> pickableInventories = inventoryServiceRestemplateClient.getInventoryForPick(pick);
        logger.debug(" Get {} valid inventory for pick {}",
                pickableInventories.size(), pick.getNumber());
        pickableInventories.stream().forEach(System.out::print);
        logger.debug(" start to pick with quantity {}",quantityToBePicked);
        Iterator<Inventory> inventoryIterator = pickableInventories.iterator();
        while(quantityToBePicked > 0 && inventoryIterator.hasNext()) {
            Inventory inventory = inventoryIterator.next();
            logger.debug(" pick from inventory {}", quantityToBePicked, inventory.getLpn());
            Long pickedQuantity = confirmPick(inventory, pick, quantityToBePicked, nextLocation);
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

    public Long confirmPick(Inventory inventory, Pick pick, Long quantityToBePicked, Location nextLocation)   {
        return confirmPick(inventory, pick, quantityToBePicked, nextLocation, "");
    }
    public Long confirmPick(Inventory inventory, Pick pick, Long quantityToBePicked, Location nextLocation, String newLpn)  {
        if (!match(inventory, pick)) {
            throw PickingException.raiseException( "inventory can't be picked for the pick. Attribute discrepancy found");
        }

        logger.debug("Start to pick from inventory\n inventory quantity: {} \n pick's quantity {} / {} "
                     , inventory.getQuantity(), quantityToBePicked, (pick.getQuantity() - pick.getPickedQuantity()));
        quantityToBePicked = Math.min(inventory.getQuantity(), quantityToBePicked);

        logger.debug(" Will pick {} from the inventory", quantityToBePicked);
        boolean pickWholeInventory = quantityToBePicked.equals(inventory.getQuantity());

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
            List<Inventory> inventories = inventoryServiceRestemplateClient.split(inventory, newLpn, quantityToBePicked);
            if (inventories.size() != 2) {
                throw PickingException.raiseException("Inventory split for pick error! Inventory is not split into 2");
            }
            inventoryToBePicked = inventories.get(1);
        }
        logger.debug("Will pick from inventory {} ", inventoryToBePicked.getLpn());

        // Move the inventory to the next location for pick
        // Move the inventory to the next location
        inventoryServiceRestemplateClient.moveInventory(inventoryToBePicked, pick, nextLocation);
        // update the quantity in the pick
        logger.debug(" change the picked quantity from {} to {}",
                pick.getPickedQuantity(), (pick.getPickedQuantity() + quantityToBePicked));
        pick.setPickedQuantity(pick.getPickedQuantity() + quantityToBePicked);
        saveOrUpdate(pick);

        // Let's update the list if the pick belongs to any list
        pickListService.processPickConfirmed(pick);
        return quantityToBePicked;

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


    /**
     * Unpick the inventory and return to stock. We will cancel the pick as well
     * @param id Pick Id
     * @param unpickedQuantity: quantity of the inventory being unpicked
     * @return pick that being cancelled
     */
    //
    public Pick unpick(Long id, Long unpickedQuantity) {
        return unpick(findById(id), unpickedQuantity);
    }

    /**
     * unpick will
     * 1. reset the picked quantity by deduct
     *    the unpickedquantity from the picked quantity
     * 2. return the unpicked quantity back to the shipment's open quantity
     *    so that we can re-allocate
     * @param pick Pick being unpicked
     * @param unpickedQuantity: quantity of the inventory being unpicked
     * @return pick that being cancelled
     */
    public Pick unpick(Pick pick, Long unpickedQuantity) {
        // Cancel the pick with unpicked quantity
        return cancelPick(pick, unpickedQuantity);
    }

}

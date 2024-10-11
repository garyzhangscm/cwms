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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.exception.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;


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
    private UserService userService;
    @Autowired
    private ShortAllocationService shortAllocationService;
    @Autowired
    private CartonizationService cartonizationService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private WaveService waveService;
    @Autowired
    private PickConfirmStrategyService pickConfirmStrategyService;
    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private OutboundConfigurationService outboundConfigurationService;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Autowired
    private PalletPickLabelContentService palletPickLabelContentService;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private PickListService pickListService;
    @Autowired
    private GridLocationConfigurationService gridLocationConfigurationService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Autowired
    private BulkPickService bulkPickService;
    @Autowired
    private PickReleaseService pickReleaseService;
    @Autowired
    private UnitService unitService;

    // syncronized key for picks to make sure
    // picks from same location and same item will be executed syncronized
    private Interner<String> pickKeyPool = Interners.newWeakInterner();

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


    public List<Pick> findAll(Long warehouseId, Long clientId, String number,
                              Long orderId, String orderIds, String orderNumber,
                              Long shipmentId, Long waveId,
                              Long  listId, Long cartonizationId,  String ids,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds,
                              Long shortAllocationId, Boolean openPickOnly,
                              Long inventoryStatusId,
                              String shipmentNumber,
                              String workOrderNumber,
                              String waveNumber,
                              String cartonizationNumber,
                              String itemNumber,
                              String sourceLocationName,
                              String destinationLocationName,
                              Long trailerAppointmentId,
                              Boolean includeCompletedPick,
                              Boolean includeCancelledPick,
                              ClientRestriction clientRestriction,
                              boolean loadDetails) {

        List<Pick> picks =  pickRepository.findAll(
                (Root<Pick> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (Objects.nonNull(clientId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("clientId"), clientId));

                    }
                    if (Objects.nonNull(orderId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }
                    if (Strings.isNotBlank(orderIds)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);


                        CriteriaBuilder.In<Long> inOrderIds = criteriaBuilder.in(joinOrder.get("id"));
                        for(String id : orderIds.split(",")) {
                            inOrderIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inOrderIds));
                    }
                    if (Strings.isNotBlank(orderNumber)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        if (orderNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinOrder.get("number"), orderNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                        }
                    }
                    if (Objects.nonNull(shipmentId) || Objects.nonNull(trailerAppointmentId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment= joinShipmentLine.join("shipment", JoinType.INNER);

                        if (Objects.nonNull(shipmentId)) {

                            predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));
                        }
                        if (Objects.nonNull(trailerAppointmentId)) {

                            Join<Shipment, Stop> joinStop = joinShipment.join("stop", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinStop.get("trailerAppointmentId"), trailerAppointmentId));
                        }


                    }
                    if (Strings.isNotBlank(shipmentNumber)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment= joinShipmentLine.join("shipment", JoinType.INNER);
                        if (shipmentNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinShipment.get("number"), shipmentNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinShipment.get("number"), shipmentNumber));
                        }

                    }


                    if (Objects.nonNull(waveId)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Wave> joinWave = joinShipmentLine.join("wave", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWave.get("id"), waveId));

                    }
                    if (Strings.isNotBlank(waveNumber)) {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Wave> joinWave = joinShipmentLine.join("wave", JoinType.INNER);
                        if (waveNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinWave.get("number"), waveNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinWave.get("number"), waveNumber));
                        }

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
                    if (Strings.isNotBlank(cartonizationNumber)) {
                        Join<Pick, Cartonization> joinCartonization = root.join("cartonization", JoinType.INNER);
                        if (cartonizationNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinCartonization.get("number"), cartonizationNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinCartonization.get("number"), cartonizationNumber));
                        }

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
                    if (Strings.isNotBlank(itemNumber)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, clientId, itemNumber);
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                    }
                    if (Objects.nonNull(inventoryStatusId)) {
                        predicates.add(criteriaBuilder.equal(root.get("inventoryStatusId"), inventoryStatusId));
                    }

                    if (Objects.nonNull(sourceLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), sourceLocationId));
                    }
                    if (Strings.isNotBlank(sourceLocationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId, sourceLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), location.getId()));
                    }

                    if (Objects.nonNull(destinationLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), destinationLocationId));
                    }
                    if (Strings.isNotBlank(destinationLocationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId, destinationLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), location.getId()));
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
                    if (Boolean.TRUE.equals(openPickOnly)) {

                        predicates.add(criteriaBuilder.greaterThan(root.get("quantity"), root.get("pickedQuantity")));
                    }
                    if (!Boolean.TRUE.equals(includeCancelledPick)) {

                        predicates.add(criteriaBuilder.notEqual(root.get("status"), PickStatus.CANCELLED));
                    }
                    if (!Boolean.TRUE.equals(includeCompletedPick)) {

                        predicates.add(criteriaBuilder.notEqual(root.get("status"), PickStatus.COMPLETED));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    // return criteriaBuilder.and(predicates.toArray(p));

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction)) {
                        return predicate;
                    }
                    else {
                        Join<Pick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        return
                                clientRestriction.addClientRestriction(predicate,
                                        joinOrder, criteriaBuilder);
                    }
                }
        );
        logger.debug("Find {} picks that match with the criteria",
                picks.size());

        logger.debug("load details? {}", loadDetails);
        if (picks.size() > 0 && loadDetails) {
            loadAttribute(picks);
        }
        return picks;
    }

    public List<Pick> findAll(Long warehouseId, Long clientId, String number,
                              Long orderId, String orderIds, String orderNumber,
                              Long shipmentId,Long waveId,
                              Long  listId, Long  cartonizationId,  String ids,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds,
                              Long shortAllocationId, Long inventoryStatusId,
                              String shipmentNumber,
                              String workOrderNumber,
                              String waveNumber,
                              String cartonizationNumber,
                              String itemNumber,
                              String sourceLocationName,
                              String destinationLocationName,
                              Long trailerAppointmentId,
                              Boolean openPickOnly,
                              Boolean includeCompletedPick,
                              Boolean includeCancelledPick,
                              ClientRestriction clientRestriction) {
        return findAll(warehouseId, clientId,  number, orderId, orderIds,
                orderNumber, shipmentId, waveId, listId, cartonizationId, ids,
                itemId, sourceLocationId, destinationLocationId,
                workOrderLineId, workOrderLineIds, shortAllocationId, openPickOnly, inventoryStatusId,
                shipmentNumber, workOrderNumber, waveNumber, cartonizationNumber,
                itemNumber, sourceLocationName, destinationLocationName,
                trailerAppointmentId, includeCompletedPick, includeCancelledPick,
                clientRestriction,
                true);
    }

    public Pick findByNumber(String number, boolean loadDetails) {
        Pick pick = pickRepository.findByNumber(number);
        if (pick != null && loadDetails) {
            loadAttribute(pick);
        }
        return pick;
    }

    public List<Pick> findByOrder(Order order) {
        return findAll(order.getWarehouseId(), null, null, order.getId(), null, null, null,
                null, null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public List<Pick> findByOrders(Long warehouseId, String orderIds) {
        return findAll(warehouseId, null, null, null, orderIds, null, null,
                null, null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null, null, null, null, null, null);

    }
    public List<Pick> findByOrders(Long warehouseId, List<Order> orders) {

        String orderIds =  orders.stream().map(Order::getId).map(String::valueOf).collect(Collectors.joining(","));
        return findByOrders(warehouseId, orderIds);
    }

    public List<Pick> findByShipment(Shipment shipment) {
        return findAll(shipment.getWarehouseId(),null, null, null, null, null, shipment.getId(),
                null, null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }
    public List<Pick> findByWorkOrder(WorkOrder workOrder) {
        String workOrderLineIds
                = workOrder.getWorkOrderLines().stream().
                        map(WorkOrderLine::getId).
                        map(Object::toString).
                        collect( Collectors.joining( "," ) );
        return findAll(workOrder.getWarehouseId(), null, null, null,null, null,  null,
                null, null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, workOrderLineIds, null,null, null, null, null, null, null);
    }
    public List<Pick> findByWave(Wave wave) {

        return findAll(wave.getWarehouseId(), null, null, null, null,null, null,
                wave.getId(), null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null,null, null, null, null, null);
    }
    public List<Pick> findByWave(Wave wave, boolean loadDetails) {

        return findAll(wave.getWarehouseId(), null, null, null, null,null, null,
                wave.getId(), null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null,
                null, null, null,null, null, null,
                null, null, loadDetails);
    }
    public List<Pick> findByPickList(PickList pickList) {

        return findAll(pickList.getWarehouseId(), null, null, null, null,null,  null,
                null, pickList.getId(),  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null,null, null, null, null, null);
    }
    public List<Pick> findByCartonization(Cartonization cartonization) {

        return findAll(cartonization.getWarehouseId(), null, null, null, null,null, null,
                null, null,  cartonization.getId(),null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null,null, null, null, null, null);
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
        logger.debug("start to load attribute for pick {}", pick.getNumber());
        // Load the details for client and supplier informaiton
        if (pick.getSourceLocationId() != null && pick.getSourceLocation() == null) {
            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
        }
        if (pick.getDestinationLocationId() != null && pick.getDestinationLocation() == null) {
            logger.debug("pick {}'s destination location id is {}, will get location from it",
                    pick.getNumber(),
                    pick.getDestinationLocationId());
            Location location = warehouseLayoutServiceRestemplateClient.getLocationById(pick.getDestinationLocationId());
            logger.debug("get location {} from id {}",
                    location.getName(),
                    pick.getDestinationLocationId());
            pick.setDestinationLocation(location);
        }

        // Load the item and inventory status information for each lines
        if (pick.getItemId() != null && pick.getItem() == null) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }

        // Load the item and inventory status information for each lines
        if (pick.getItemPackageTypeId() != null && pick.getItemPackageType() == null) {
            pick.setItemPackageType(inventoryServiceRestemplateClient.getItemPackageTypeById(pick.getItemPackageTypeId()));
        }
        // load pick's inventory status for
        if (pick.getInventoryStatusId() != null &&
                pick.getInventoryStatus() == null) {
            pick.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            pick.getInventoryStatusId()
                    ));
        }

        if (Objects.nonNull(pick.getWorkTaskId()) &&
                Objects.isNull(pick.getWorkTask())) {
            pick.setWorkTask(
                    resourceServiceRestemplateClient.getWorkTaskById(
                            pick.getWarehouseId(), pick.getWorkTaskId())
            );
        }


    }

    /**
     * We will allow the user to find pick by any one of the following id
     * 1. Pick work's number:
     * 2. Order Number:
     * 3. Shipment Number:
     * 4. Work Order Number:
     * 5. List Pick Number
     * 6. Carton Number
     * @param warehouseId
     * @param containerId
     * @return
     */
    public List<Pick> getPicksByContainer(Long warehouseId, Long clientId, String containerId) {
        // check if the container id is a pick work number

        if (Objects.nonNull(findByNumber(containerId, false))) {
            return Collections.singletonList(findByNumber(containerId));
        }

        // Check if the container id is a order number
        if (Objects.nonNull(orderService.findByNumber(warehouseId, clientId, containerId, false))) {
            return findByOrder(orderService.findByNumber(warehouseId, clientId, containerId));
        }
        // Check if the container id is a shipment number
        if (Objects.nonNull(shipmentService.findByNumber(containerId, false))) {
            return findByShipment(shipmentService.findByNumber( containerId, false));
        }
        // Check if the container id is a work order number
        if (Objects.nonNull(workOrderServiceRestemplateClient.getWorkOrderByNumber(warehouseId, containerId))) {
            return findByWorkOrder(workOrderServiceRestemplateClient.getWorkOrderByNumber(warehouseId, containerId));
        }
        // Check if the container id is a wave
        if (Objects.nonNull(waveService.findByNumber(warehouseId, containerId, false))) {
            return findByWave(waveService.findByNumber(warehouseId, containerId, false));
        }
        // Check if the container id is pick list
        if (Objects.nonNull(pickListService.findByNumber(warehouseId, containerId, false))) {
            return findByPickList(pickListService.findByNumber(warehouseId, containerId, false));
        }
        // Check if the container id is cartonization
        if (Objects.nonNull(cartonizationService.findByNumber(warehouseId, containerId))) {
            return findByCartonization(cartonizationService.findByNumber(warehouseId, containerId));
        }

        return new ArrayList<>();


    }


    @Transactional
    public Pick save(Pick pick) {
        return save(pick, true);
    }
    @Transactional
    public Pick save(Pick pick, boolean loadDetails) {
        Pick newPick = pickRepository.save(pick);
        if (loadDetails) {

            loadAttribute(newPick);
        }
        return newPick;
    }

    @Transactional
    public Pick saveOrUpdate(Pick pick) {
        return saveOrUpdate(pick, true);
    }

    @Transactional
    public Pick saveOrUpdate(Pick pick, boolean loadDetails ) {
        if (pick.getId() == null && findByNumber(pick.getNumber()) != null) {
            pick.setId(findByNumber(pick.getNumber()).getId());
        }
        return save(pick, loadDetails);
    }

    @Transactional
    public void delete(Pick pick) {

        pickRepository.delete(pick);
    }

    public void delete(Long id) {
        pickRepository.deleteById(id);
    }

    @Transactional
    public void removePick(Pick pick) {
        palletPickLabelContentService.onPickRemove(pick);
        delete(pick);
    }


    public List<Pick> getOpenPicksByItemIdAndSourceLocation(Long itemId, Location sourceLocation){
        return getOpenPicksByItemIdAndSourceLocation(itemId, sourceLocation, true);
    }


    public List<Pick> getOpenPicksByItemIdAndSourceLocation(Long itemId, Location sourceLocation, boolean loadDetails){
        return Objects.isNull(sourceLocation) ?
                getOpenPicksByItemId(itemId, loadDetails) :
                getOpenPicksByItemIdAndSourceLocationId(itemId, sourceLocation.getId(), loadDetails);
    }

    public List<Pick> getOpenPicksByItemIdAndSourceLocationId(Long itemId, Long sourceLocationId){

        return getOpenPicksByItemIdAndSourceLocationId(itemId, sourceLocationId, true);
    }


    public List<Pick> getOpenPicksByItemIdAndSourceLocationId(Long itemId, Long sourceLocationId, boolean loadDetails){

        List<Pick> picks = pickRepository.getOpenPicksByItemIdAndSourceLocationId(itemId, sourceLocationId);
        if (picks.size() > 0 && loadDetails) {
            loadAttribute(picks);
        }
        return picks;
    }


    public List<Pick> getOpenPicksByItemId(Long itemId){

        return getOpenPicksByItemId(itemId, true);
    }

    public List<Pick> getOpenPicksByItemId(Long itemId, boolean loadDetails){

        List<Pick> picks = pickRepository.getOpenPicksByItemId(itemId);
        if (picks.size() > 0 && loadDetails) {
            loadAttribute(picks);
        }
        return picks;
    }


    public List<Pick> getOpenPicks(Long warehouseId){

        return getOpenPicks(warehouseId, true);
    }
    public List<Pick> getOpenPicks(Long warehouseId, boolean loadDetails){

        List<Pick> picks = pickRepository.getOpenPicks(warehouseId);
        if (picks.size() > 0 && loadDetails) {
            loadAttribute(picks);
        }
        return picks;
    }


    public String getNextPickNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "pick-number");

    }

    public List<Pick> cancelPicks(String pickIds,
                                  boolean errorLocation,
                                  boolean generateCycleCount,
                                  boolean reallocate,
                                  boolean skipOriginalLocation) {

        List<Pick> picks = new ArrayList<>();

        Arrays.stream(pickIds.split(",")).forEach(
                pickIdString -> {
                    Long pickId = Long.parseLong(pickIdString);
                    // picks.add(findById(pickId));
                    picks.addAll(cancelPick(pickId, errorLocation, generateCycleCount, reallocate,
                            skipOriginalLocation));
                }
        );

        return picks;

    }

    public List<Pick> cancelPick(Long id, boolean errorLocation, boolean generateCycleCount,
                           boolean reallocate,
                           boolean skipOriginalLocation) {
        return cancelPick(findById(id), errorLocation, generateCycleCount, reallocate,
                skipOriginalLocation);
    }

    @Transactional
    public List<Pick> cancelPick(Pick pick, boolean errorLocation, boolean generateCycleCount,
                           boolean reallocate,
                           boolean skipOriginalLocation) {
        return cancelPick(pick, pick.getQuantity() - pick.getPickedQuantity(),
                errorLocation, generateCycleCount, reallocate,
                skipOriginalLocation);

    }
    @Transactional
    public List<Pick> cancelPick(Pick pick, Long cancelledQuantity, boolean errorLocation, boolean generateCycleCount,
                           boolean reallocate,
                           boolean skipOriginalLocation) {
        logger.debug("start to cancel pick {}", pick.getNumber());
        if (pick.getStatus().equals(PickStatus.COMPLETED)) {
            throw PickingException.raiseException("Can't cancel pick that is already completed!");
        }

        // we have nothing left to cancel
        if (cancelledQuantity == 0) {
            return new ArrayList<>();
        }

        // return the open quantity back to the shipment line
        // shipmentLineService.cancelPickQuantity(pick.getShipmentLine(), cancelQuantity);
        // logger.debug("pick.getShipmentLine() != null : {}", pick.getShipmentLine() != null);
        // logger.debug("pick.getShortAllocation() != null : {}", pick.getShortAllocation() != null);
        // logger.debug("pick.getWorkOrderLineId() != null : {}", pick.getWorkOrderLineId() != null);
        OrderActivity orderActivity = orderActivityService.createOrderActivity(
                pick.getWarehouseId(), pick.getShipmentLine(), pick, OrderActivityType.PICK_CALCELLATION
        );
        // If this is a pick that allocates a whole LPN, release the LPN
        if (Objects.nonNull(pick.getLpn())) {

            inventoryServiceRestemplateClient.releaseLPNAllocated(pick.getWarehouseId(), pick.getLpn(), pick.getId());
        }

        // Save the data to cancelled pick table
        cancelledPickService.registerPickCancelled(pick, cancelledQuantity);

        if (errorLocation) {
            warehouseLayoutServiceRestemplateClient.errorLocation(
                    pick.getWarehouseId(), pick.getSourceLocationId());
        }
        if (generateCycleCount) {

            Location location = Objects.nonNull(pick.getSourceLocation()) ?
                    pick.getSourceLocation() :
                    Objects.nonNull(pick.getSourceLocationId()) ?
                        warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()) : null;

            if (Objects.nonNull(location)) {

                inventoryServiceRestemplateClient.generateCycleCount(
                        pick.getWarehouseId(), location.getName()
                );
            }
        }

        pick.setQuantity(pick.getQuantity() - cancelledQuantity);

        // save the order activity
        orderActivity.setQuantityByNewPick(pick);
        orderActivityService.sendOrderActivity(orderActivity);

        pick = saveOrUpdate(pick);


        logger.debug("after we cancelled the quantity {}, there's still {} quantity left",
                cancelledQuantity, pick.getQuantity());

        if (pick.getQuantity() == 0) {
            // There's nothing left on the picks, let's remove it.
            // We can find the history in the cancelled pick table
            logger.debug("Remove the pick as there's nothing left for this pick {}", pick.getNumber());
            removePick(pick);

            // when we remove the pick, we may need to remove the work task, if there's any
            if (Objects.nonNull(pick.getWorkTaskId())) {
                resourceServiceRestemplateClient.cancelWorkTaskById(
                        pick.getWarehouseId(),
                        pick.getWorkTaskId()
                );
            }
        }

        // return the new picks in case of reallocate
        List<Pick> newPicks = new ArrayList<>();

        // reflect the quantity on shipment / work order/ short allocation about the cancelled pick
        if (pick.getShipmentLine() != null) {
            ShipmentLine newShipmentLine = shipmentLineService.registerPickCancelled(pick.getShipmentLine(), cancelledQuantity);
            orderActivity.setQuantityByNewShipmentLine(newShipmentLine);
            if (reallocate) {
                Set<Long> skipLocationIDs = new HashSet<>();
                if (skipOriginalLocation) {
                    skipLocationIDs.add(pick.getSourceLocationId());
                }
                AllocationResult allocationResult =
                        shipmentLineService.allocateShipmentLine(newShipmentLine, skipLocationIDs);
                newPicks.addAll(allocationResult.getPicks());
            }
        }
        else if (pick.getShortAllocation() != null) {
            ShortAllocation newShortAllocation =
                    shortAllocationService.registerPickCancelled(pick.getShortAllocation(), cancelledQuantity);
            orderActivity.setQuantityByNewShortAllocation(newShortAllocation);
        }
        else if (pick.getWorkOrderLineId() != null) {
            workOrderServiceRestemplateClient.registerPickCancelled(
                    pick.getWorkOrderLineId(),
                    cancelledQuantity,
                    pick.getDestinationLocationId());
        }

        return newPicks;

    }


    public Pick generateBasicPickInformation(Long warehouseId,InventorySummary inventorySummary,
                                             Long quantity,
                                             boolean wholeLPNPick) {
        return generateBasicPickInformation(
                warehouseId, inventorySummary, quantity, null, null, wholeLPNPick);
    }


    public Pick generateBasicPickInformation(Long warehouseId,
                                             Inventory inventory,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             String lpn,
                                             boolean wholeLPNPick) {
        return generateBasicPickInformation(warehouseId,
                inventory.getItem(),
                inventory.getLocation(),
                inventory.getInventoryStatus(),
                quantity,
                pickableUnitOfMeasure, lpn,
                wholeLPNPick);
    }
    public Pick generateBasicPickInformation(Long warehouseId,
                                             InventorySummary inventorySummary,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             String lpn,
                                             boolean wholeLPNPick) {

        return generateBasicPickInformation(warehouseId,
                inventorySummary.getItem(),
                inventorySummary.getLocation(),
                inventorySummary.getInventoryStatus(),
                quantity,
                pickableUnitOfMeasure,
                lpn ,
                wholeLPNPick);
    }
    public Pick generateBasicPickInformation(Long warehouseId,
                                             Item item,
                                             Location sourceLocation,
                                             InventoryStatus inventoryStatus,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             String lpn,
                                             boolean wholeLPNPick) {
        return generateBasicPickInformation(warehouseId,
                item, sourceLocation, inventoryStatus,
                quantity, pickableUnitOfMeasure, lpn,
                "", "", "", "","","","","", wholeLPNPick);

    }
    public Pick generateBasicPickInformation(Long warehouseId,
                                             Item item,
                                             Location sourceLocation,
                                             InventoryStatus inventoryStatus,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             String lpn,
                                             String color,
                                             String productSize,
                                             String style,
                                             String inventoryAttribute1,
                                             String inventoryAttribute2,
                                             String inventoryAttribute3,
                                             String inventoryAttribute4,
                                             String inventoryAttribute5,
                                             boolean wholeLPNPick) {

        Pick pick = new Pick();
        pick.setWarehouseId(warehouseId);
        pick.setItem(item);
        pick.setItemId(item.getId());
        pick.setSourceLocation(sourceLocation);
        pick.setSourceLocationId(sourceLocation.getId());
        pick.setQuantity(quantity);
        pick.setPickedQuantity(0L);
        pick.setNumber(getNextPickNumber(sourceLocation.getWarehouse().getId()));
        pick.setStatus(PickStatus.PENDING);
        pick.setInventoryStatusId(inventoryStatus.getId());
        pick.setWholeLPNPick(wholeLPNPick);

        pick.setColor(color);
        pick.setProductSize(productSize);
        pick.setStyle(style);

        pick.setInventoryAttribute1(inventoryAttribute1);
        pick.setInventoryAttribute2(inventoryAttribute2);
        pick.setInventoryAttribute3(inventoryAttribute3);
        pick.setInventoryAttribute4(inventoryAttribute4);
        pick.setInventoryAttribute5(inventoryAttribute5);


        if (Objects.nonNull(pickableUnitOfMeasure)) {

            pick.setUnitOfMeasureId(pickableUnitOfMeasure.getUnitOfMeasureId());
        }
        pick.setLpn(lpn);

        logger.debug("Start to get pick confirm strategy");

        // get the pick confirm flags
        PickConfirmStrategy pickConfirmStrategy
                = pickConfirmStrategyService.getMatchedPickConfirmStrategy(pick);

        if (Objects.isNull(pickConfirmStrategy)) {
            // if no matched strategy, by default, we will force
            // the user to confirm everything(most strict)
            logger.debug("No strategy found. Setup confirm flag for all fields");
            pick.setConfirmItemFlag(true);
            pick.setConfirmLocationFlag(true);
            pick.setConfirmLocationCodeFlag(false);

            pick.setConfirmLpnFlag(true);
        }
        else {

            logger.debug("Pick confirm strategy found. id: {}," +
                    " confirm item: {}, confirm location: {}, confirm location code: {}" +
                    ", confirm lpn: {}",
                    pickConfirmStrategy.getId(),
                    pickConfirmStrategy.isConfirmItemFlag(),
                    pickConfirmStrategy.isConfirmLocationFlag(),
                    pickConfirmStrategy.isConfirmLocationCodeFlag(),
                    pickConfirmStrategy.isConfirmLpnFlag());
            pick.setConfirmItemFlag(pickConfirmStrategy.isConfirmItemFlag());
            pick.setConfirmLocationFlag(pickConfirmStrategy.isConfirmLocationFlag());
            pick.setConfirmLocationCodeFlag(pickConfirmStrategy.isConfirmLocationCodeFlag());
            pick.setConfirmLpnFlag(pickConfirmStrategy.isConfirmLpnFlag());
        }


        return pick;
    }

    public Pick generateBasicPickInformation(Long warehouseId,InventorySummary inventorySummary,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             boolean wholeLPNPick) {

        return generateBasicPickInformation(
                warehouseId, inventorySummary, quantity, pickableUnitOfMeasure, null, wholeLPNPick);
    }
    public Pick generateBasicPickInformation(Long warehouseId, Inventory inventory,
                                             Long quantity,
                                             ItemUnitOfMeasure pickableUnitOfMeasure,
                                             boolean wholeLPNPick) {

        return generateBasicPickInformation(
                warehouseId, inventory, quantity, pickableUnitOfMeasure, null, wholeLPNPick);
    }

    public Pick generateBasicPickInformation(Long warehouseId,InventorySummary inventorySummary,
                                             Long quantity, String lpn,
                                             boolean wholeLPNPick) {

        return generateBasicPickInformation(
                warehouseId, inventorySummary, quantity, null, lpn, wholeLPNPick);
    }

    @Transactional
    private Pick setupShipmentInformation(Pick pick, ShipmentLine shipmentLine) {
        pick.setShipmentLine(shipmentLine);
        pick.setWarehouseId(shipmentLine.getWarehouseId());
        pick.setPickType(PickType.OUTBOUND);

        pick.setColor(shipmentLine.getOrderLine().getColor());
        pick.setProductSize(shipmentLine.getOrderLine().getProductSize());
        pick.setStyle(shipmentLine.getOrderLine().getStyle());

        pick.setInventoryAttribute1(shipmentLine.getOrderLine().getInventoryAttribute1());
        pick.setInventoryAttribute2(shipmentLine.getOrderLine().getInventoryAttribute2());
        pick.setInventoryAttribute3(shipmentLine.getOrderLine().getInventoryAttribute3());
        pick.setInventoryAttribute4(shipmentLine.getOrderLine().getInventoryAttribute4());
        pick.setInventoryAttribute5(shipmentLine.getOrderLine().getInventoryAttribute5());

        pick.setAllocateByReceiptNumber(shipmentLine.getOrderLine().getAllocateByReceiptNumber());

        // Setup the destination, get from ship staging area


        Location stagingLocation = getDestinationLocationForPick(shipmentLine, pick);
        if (Objects.isNull(stagingLocation)) {
            // if we can't find a staging location, let's set the picks'
            // status to hold
            pick.setStatus(PickStatus.HOLD);
        }
        else {

            pick.setStatus(PickStatus.PENDING);
            pick.setDestinationLocation(stagingLocation);
            pick.setDestinationLocationId(stagingLocation.getId());
        }

        return save(pick);


    }

    @Transactional
    private Pick processPick(Pick pick) {

        return processPick(pick, true);
    }
    @Transactional
    private Pick processPick(Pick pick, boolean loadDetails) {
        // setup the destination for the pick

        if (!pick.getStatus().equals(PickStatus.RELEASED) ||
            !pick.getStatus().equals(PickStatus.INPROCESS)) {
            logger.debug("Pick is not released!, skip further process");
            return pick;
        }
        // Setup the pick movement
        logger.debug("start to setup movement path for pick {}", pick.getNumber());
        setupMovementPath(pick);
        logger.debug("{} pick movement path setup for the pick", pick.getPickMovements().size());


        logger.debug("start to cartonize pick {}", pick.getNumber());
        processCartonization(pick);

        // Let's see if we can group the pick either
        // 1. into an existing pick list
        // 2. or create a new picking list so other picks can be grouped
        logger.debug("We will postpone the list pick and bulk pick until all the picks are generated for the wave. " +
                "List pick and bulk pick will only possible for wave at this moment");
        // processPickList(pick);

        logger.debug("pick {} is processed. we are good to go",
                pick.getNumber());
        return findById(pick.getId(), loadDetails);
    }

    @Transactional
    public Pick generatePick(InventorySummary inventorySummary,
                             ShipmentLine shipmentLine, long quantity,
                             String lpn,
                             boolean wholeLPNPick) {
        logger.debug("create picks for:");
        logger.debug("inventory summary: {}",
                Objects.isNull(inventorySummary.getLocation()) ? inventorySummary.getLocationId() :
                    inventorySummary.getLocation().getName());
        logger.debug("shipment line: {}", shipmentLine);
        logger.debug("quantity: {}", quantity);
        logger.debug("lpn: {}", lpn);
        Pick pick = generateBasicPickInformation(shipmentLine.getWarehouseId(), inventorySummary, quantity, lpn, wholeLPNPick);
        logger.debug("will need to setup shipment line information for the pick: {}", pick.getNumber());
        pick = setupShipmentInformation(pick, shipmentLine);
        logger.debug("start to process the pick: {}", pick.getNumber());

        return processPick(pick);
    }

    @Transactional
    public Pick generateMnaualPick(InventorySummary inventorySummary,
                             ShipmentLine shipmentLine, long quantity,
                             String lpn,
                             boolean wholeLPNPick) {
        logger.debug("create picks for:");
        logger.debug("inventory summary: {}",
                Objects.isNull(inventorySummary.getLocation()) ? inventorySummary.getLocationId() :
                        inventorySummary.getLocation().getName());
        logger.debug("shipment line: {}", shipmentLine);
        logger.debug("quantity: {}", quantity);
        logger.debug("lpn: {}", lpn);
        Pick pick = generateBasicPickInformation(shipmentLine.getWarehouseId(), inventorySummary, quantity, lpn, wholeLPNPick);
        logger.debug("will need to setup shipment line information for the pick: {}", pick.getNumber());
        pick = setupShipmentInformation(pick, shipmentLine);
        logger.debug("start to process the pick: {}", pick.getNumber());

        return processPick(pick);
    }

    @Transactional(dontRollbackOn = GenericException.class)
    public Pick generatePick(InventorySummary inventorySummary,
                             ShipmentLine shipmentLine, long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(shipmentLine.getWarehouseId(),
                inventorySummary, quantity, pickableUnitOfMeasure, wholeLPNPick);
        pick = setupShipmentInformation(pick, shipmentLine);
        return processPick(pick);
    }


    @Transactional
    public Pick generatePick(ShipmentLine shipmentLine, Inventory inventory,
                             Long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             boolean wholeLPNPick) {
        return generatePick(shipmentLine, inventory,
                quantity, pickableUnitOfMeasure, true,
                wholeLPNPick);
    }
    @Transactional
    public Pick generatePick(ShipmentLine shipmentLine, Inventory inventory,
                             Long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             boolean loadDetails,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(
                shipmentLine.getWarehouseId(), inventory, quantity, pickableUnitOfMeasure, wholeLPNPick);
        pick = setupShipmentInformation(pick, shipmentLine);
        return processPick(pick, loadDetails);
    }

    @Transactional
    private Pick setupWorkOrderInformation(Pick pick, WorkOrder workOrder,
                                           WorkOrderLine workOrderLine,
                                           Long destinationLocationId) {
        return setupWorkOrderInformation(pick,
                workOrder, workOrderLine, destinationLocationId, true);

    }
    @Transactional
    private Pick setupWorkOrderInformation(Pick pick, WorkOrder workOrder,
                                           WorkOrderLine workOrderLine,
                                           Long destinationLocationId,
                                           boolean loadDetails) {

        pick.setWorkOrderLineId(workOrderLine.getId());
        pick.setWarehouseId(workOrder.getWarehouseId());
        pick.setPickType(PickType.WORK_ORDER);

        // Setup the destination, get from ship staging area

        // Long stagingLocationId = getDestinationLocationIdForPick(workOrder);
        // for picks of work order, we will always assume that it has the
        // destination, which is the production line. So we will always release the
        // pick by default
        pick.setDestinationLocationId(destinationLocationId);
        pick.setStatus(PickStatus.RELEASED);

        return save(pick, loadDetails);
    }


    @Transactional
    public Pick generatePick(WorkOrder workOrder, InventorySummary inventorySummary,
                             WorkOrderLine workOrderLine, Long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             Long destinationLocationId,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(
                workOrder.getWarehouseId(), inventorySummary, quantity, pickableUnitOfMeasure, wholeLPNPick);
        pick = setupWorkOrderInformation(pick, workOrder, workOrderLine, destinationLocationId);
        return processPick(pick);
    }

    @Transactional
    public Pick generatePick(WorkOrder workOrder, Inventory inventory,
                             WorkOrderLine workOrderLine, Long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             Long destinationLocationId,
                             boolean wholeLPNPick) {
        return generatePick(workOrder, inventory,
                workOrderLine, quantity, pickableUnitOfMeasure, destinationLocationId, true,
                wholeLPNPick);
    }
    @Transactional
    public Pick generatePick(WorkOrder workOrder, Inventory inventory,
                             WorkOrderLine workOrderLine, Long quantity,
                             ItemUnitOfMeasure pickableUnitOfMeasure,
                             Long destinationLocationId,
                             boolean loadDetails,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(
                workOrder.getWarehouseId(), inventory, quantity, pickableUnitOfMeasure, wholeLPNPick);
        pick = setupWorkOrderInformation(pick, workOrder, workOrderLine, destinationLocationId);
        return processPick(pick, loadDetails);
    }


    @Transactional
    public Pick generatePick(WorkOrder workOrder,
                             InventorySummary inventorySummary,
                             WorkOrderLine workOrderLine,
                             long quantity,
                             String lpn,
                             Long destinationLocationId,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(
                workOrder.getWarehouseId(), inventorySummary, quantity, lpn, wholeLPNPick);
        pick = setupWorkOrderInformation(pick, workOrder, workOrderLine, destinationLocationId);
        return processPick(pick);
    }

    @Transactional
    private void processCartonization(Pick pick) {
        logger.debug(">> Start to process cartonization for pick: {}", pick.getNumber());
        Cartonization cartonization = cartonizationService.processCartonization(pick);
        if (Objects.nonNull(cartonization)){
            // OK, we got a suitable cartonization, let's assign it to the pick
            pick.setCartonization(cartonization);
        }
        saveOrUpdate(pick);


    }
    /**
    @Transactional
    private void processPickList(Pick pick) {
        try {
            logger.debug("Start to find pick list candidate");
            PickList pickList = pickListService.processPickList(pick);
            if(Objects.isNull(pickList)) {
                // We didn't get any potential pick list
                // Which normally means we have the list pick
                // function turned off
                return;
            }
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
**/
    /**
     * Generate emergency replenishment type of picks for short allocation
     * @param inventorySummary
     * @param shortAllocation
     * @param quantity
     * @param pickableUnitOfMeasure
     * @return
     */
    @Transactional
    public Pick generatePick(InventorySummary inventorySummary, ShortAllocation shortAllocation,
                             Long quantity, ItemUnitOfMeasure pickableUnitOfMeasure,
                             boolean wholeLPNPick) {
        Pick pick = generateBasicPickInformation(
                shortAllocation.getWarehouseId(), inventorySummary, quantity, pickableUnitOfMeasure,
                wholeLPNPick);

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

        resetDestinationLocationPendingVolume(pick);


        logger.debug("pick saved!!!! id : {}", savedPick.getId());
        // Setup the pick movement
        setupMovementPath(savedPick);
        logger.debug("{} pick movement path setup for the pick", savedPick.getPickMovements().size());

        return findById(savedPick.getId());
    }

    private void resetDestinationLocationPendingVolume(Pick pick) {
        // reserve the destination location
        // with empty reserve code so
        // it will update the pending volume only
        logger.debug("=> Will update the pending volume of location {}, SIZE {}, quantity {}",
                pick.getDestinationLocationId(), pick.getSize(unitService), pick.getQuantity());
        warehouseLayoutServiceRestemplateClient.reserveLocation(
                pick.getDestinationLocationId(),
                "",
                pick.getSize(unitService).getFirst(),
                pick.getQuantity(),
                1
        );
    }


    private Location getDestinationLocationForPick(ShipmentLine shipmentLine, Pick pick) {


        if (Objects.nonNull(shipmentLine.getOrderLine().getOrder().getStageLocationId())) {
            // we have the location setup on the order, we assume the location should be
            // already reserved when the order is created.
            // let's return the location
            return warehouseLayoutServiceRestemplateClient.getLocationById(
                    shipmentLine.getOrderLine().getOrder().getStageLocationId()
            );

        }


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

        logger.debug("OK, we find the ship stage configuration: {}", shippingStageAreaConfiguration);

        Location stagingLocation = shippingStageAreaConfigurationService.reserveShippingStageLocation(shippingStageAreaConfiguration, pick);

        logger.debug("Bingo, we got the ship stage location: \n{}", stagingLocation );
        return stagingLocation;

    }


    private Location getDestinationLocationForPick(Pick pick) {
        return emergencyReplenishmentConfigurationService.getEmergencyReplenishmentDestination(pick);

    }


    // Once we have the source and destination of the pick, we will need to setup the movement path of
    // the pick. THe movement path will guide the user to drop in different hops when moving inventory
    // from the source to the destination
    private void setupMovementPath(Pick pick) {
        logger.debug("Start to generate movement for pick {}, from {} / {} to {} / {}",
                pick.getNumber(),
                pick.getSourceLocationId(),
                Objects.nonNull(pick.getSourceLocation()) ? pick.getSourceLocation().getName() : "",
                pick.getDestinationLocationId(),
                Objects.nonNull(pick.getDestinationLocation()) ? pick.getDestinationLocation().getName() : "");
        List<MovementPath> movementPaths = inventoryServiceRestemplateClient.getPickMovementPath(pick);

        if (movementPaths.size() == 0) {
            // No hop area / location defined
            logger.debug("No movement path defined!");

            return;
        }

        logger.debug(">> We find {} movement path configuration available, we will go through one by one until we find a suitable configuration",
                movementPaths.size());
        // Loop through each configuraion until we can find a good hop chain from the source to the destination
        for(MovementPath movementPath : movementPaths) {
            if (setupMovementPath(pick, movementPath)) {
                // OK, we are able to setup the hop locaiton / areas based upon this movement configuration
                // let's return
                break;
            }
        }

    }
    @Transactional
    private boolean setupMovementPath(Pick pick, MovementPath movementPath) {
        logger.debug("Try movement path configuration: {} with {} details",
                movementPath, movementPath.getMovementPathDetails().size());
        List<MovementPathDetail> movementPathDetails = movementPath.getMovementPathDetails();

        List<PickMovement> pickMovements = new ArrayList<>();
        try {
            for (MovementPathDetail movementPathDetail : movementPathDetails) {
                PickMovement pickMovement = getPickMovement(pick, movementPathDetail);
                logger.debug("Get pickmove: {} \n from movement configuration {}",
                        pickMovement, movementPathDetail);
                pickMovements.add(pickMovement);
            }
        }
        catch(Exception exception) {
            logger.debug("exception when we try to setup the movement path for the pick\n Pick: {}\n Movement Path: {}",
            pick, movementPath);
            return false;
        }

        logger.debug("By the end, we get {} pick movement", pickMovements.size());
        if (pickMovements.size() == 0) {
            return false;
        }
        // Save the pick movement
        pickMovements.stream().forEach(pickMovement -> savePickMove(pickMovement));
        return  true;
    }
    @Transactional
    private void savePickMove(PickMovement pickMovement) {
        pickMovement = pickMovementService.save(pickMovement);

        // If the pick move is going through a grid location, then we
        // will setup the grid location's pending quantity
        GridLocationConfiguration gridLocation =
                gridLocationConfigurationService.findByWarehouseIdAndLocationId(
                        pickMovement.getWarehouseId(), pickMovement.getLocationId());
        if(Objects.nonNull(gridLocation)) {
            gridLocationConfigurationService.increasePendingQuantity(gridLocation, pickMovement.getPick().getQuantity());
        }

    }
    private PickMovement getPickMovement(Pick pick, MovementPathDetail movementPathDetail) {

        logger.debug("## getPickMovement: \n >> pick: {} \n movement path details: {}",
                        pick, movementPathDetail);
        if (movementPathDetail.getHopLocationId() != null) {
            // OK we are suppose to reserve a location by the specific ID.
            // Let's see if we can reserve this typical location
            logger.debug("## Start to get location by id {}",
                    movementPathDetail.getHopLocationId() );

            Location hopLocation = warehouseLayoutServiceRestemplateClient.reserveLocation(movementPathDetail.getHopLocationId(),
                    getReserveCode(pick, movementPathDetail), pick.getSize(unitService).getFirst(), pick.getQuantity(), 1);
            logger.debug("## we get location {}",
                    hopLocation);
            return new PickMovement(pick, hopLocation, movementPathDetail.getSequence());
        }
        else if (movementPathDetail.getHopLocationGroupId() != null) {
            // OK we are suppose to reserve a location from a group
            // Let's see if we can reserve any location from a typical group
            logger.debug("## Start to get location by group id {}",
                    movementPathDetail.getHopLocationGroupId() );

            Location hopLocation = warehouseLayoutServiceRestemplateClient.reserveLocationFromGroup(movementPathDetail.getHopLocationGroupId(),
                    getReserveCode(pick, movementPathDetail), pick.getSize(unitService).getFirst(), pick.getQuantity(), 1);
            logger.debug("## we get location {}",
                    hopLocation);
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
            case BY_SHIPMENT:
                return pick.getShipmentLine().getShipmentNumber();
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
        return confirmPick(pick, quantity, "", "");
    }
    public Pick confirmPick(Pick pick, Long quantity, String lpn, String destinationLpn) {
        if (pick.getPickMovements().size() == 0) {

            logger.debug("There's no movement for this pick: {}",
                    pick.getNumber());
            return confirmPick(pick, quantity, pick.getDestinationLocation(), lpn, destinationLpn);
        }
        else {
            Location nextLocation = pick.getPickMovements().get(0).getLocation();

            if (Objects.isNull(nextLocation) &&
                Objects.nonNull(pick.getPickMovements().get(0).getLocationId())) {
                nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(
                        pick.getPickMovements().get(0).getLocationId()
                );
            }
            if (Objects.isNull(nextLocation)) {

                throw PickingException.raiseException("Can't find destination location from the pick move for the pick: " +
                        pick.getNumber());
            }

            logger.debug("we get the next location {} from movement for this pick: {}",
                    nextLocation.getName(), pick.getNumber());
            return confirmPick(pick, quantity, nextLocation, lpn, destinationLpn);
        }
    }

    public Pick confirmPick(Long pickId, Long quantity, Long nextLocationId,
                            String nextLocationName,
                            boolean pickToContainer, String containerId) {
        return confirmPick(pickId, quantity, nextLocationId, nextLocationName,
                pickToContainer, containerId, "", "");
    }
    public Pick confirmPick(Long pickId, Long quantity, Long nextLocationId,
                            String nextLocationName,
                            boolean pickToContainer, String containerId,
                            String lpn, String destinationLpn)  {
        Pick pick = findById(pickId);

        if (Objects.nonNull(pick.getShipmentLine())) {
            Order order = pick.getShipmentLine().getOrderLine().getOrder();

            if (order.getStatus().equals(OrderStatus.COMPLETE)) {
                throw OrderOperationException.raiseException(
                        "Can't confirm the pick " + pick.getNumber() + " as its order " +
                                order.getNumber() + " already completed");
            }
            if (order.getStatus().equals(OrderStatus.CANCELLED)) {
                throw OrderOperationException.raiseException(
                        "Can't confirm the pick " + pick.getNumber() + " as its order " +
                                order.getNumber() + " already cancelled");
            }
            if (Boolean.TRUE.equals(order.getCancelRequested())) {
                throw OrderOperationException.raiseException("There's a cancel request on the pick " +
                        pick.getNumber() + "'s Order " + order.getNumber() + ", " +
                        "please cancel it before you want to continue");
            }
        }

        if (pickToContainer) {
            // OK we are picking to container, let's check if we already have
            // a location for the container. If not, we will create the location
            // on the fly.
            Location nextLocation =
                    warehouseLayoutServiceRestemplateClient.getLocationByContainerId(pick.getWarehouseId(), containerId);
            return confirmPick(pick, quantity, nextLocation, lpn, destinationLpn);

        }
        if (Objects.nonNull(nextLocationId)) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
            if (Objects.nonNull(nextLocation)) {
                return confirmPick(pick, quantity, nextLocation, lpn, destinationLpn);
            }
            else {
                throw PickingException.raiseException(
                        "Can't confirm the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else if (StringUtils.isNotBlank(nextLocationName)) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    pick.getWarehouseId(), nextLocationName);
            if (Objects.nonNull(nextLocation)) {
                return confirmPick(pick, quantity, nextLocation, lpn, destinationLpn);
            }
            else {
                logger.debug("Can't confirm the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
                throw PickingException.raiseException(
                        "Can't confirm the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else {
            return confirmPick(pick, quantity, lpn, destinationLpn);
        }
    }

    public Pick confirmPick(Pick pick, Long quantity, String lpn)   {
        return confirmPick(pick, quantity, lpn, "");
    }

    public Pick confirmPick(Pick pick, Long quantity, Location nextLocation)   {
        return confirmPick(pick, quantity, nextLocation, "", "");
    }
    public Pick confirmPick(Pick pick, Long quantity, Location nextLocation, String lpn, String destinationLpn)   {

        logger.debug("==> Before the pick confirm, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());
        // start the ord activity transaction
        OrderActivity orderActivity = orderActivityService.createOrderActivity(
                pick.getWarehouseId(), pick.getShipmentLine(), pick, OrderActivityType.PICK_CONFIRM
        );
        // make sure we are not over pick. At this moment, over pick is not allowed
        // If the quantity is not passed in, we will pick the whole quantity that is still left
        Long quantityToBePicked = quantity == null ? pick.getQuantity() - pick.getPickedQuantity() : quantity;
        Long totalQuantityPicked = 0L;
        if (quantityToBePicked <= 0 ||  quantityToBePicked > pick.getQuantity() - pick.getPickedQuantity()) {
            throw PickingException.raiseException("Over pick is not allowed. Try to pick: " + quantityToBePicked +
                    ", Quantity left: " + (pick.getQuantity() - pick.getPickedQuantity()));
        }

        // we will use synchronized to prevent multiple users picking the same item from the
        // location;
        // key is the location id and item id
        String key =  pick.getSourceLocationId() + "-" + pick.getItemId();


        synchronized (pickKeyPool.intern(key)) {
            List<Inventory> pickableInventories = inventoryServiceRestemplateClient.getInventoryForPick(pick, lpn, false);
            logger.debug(" Get {} valid inventory for pick {}",
                    pickableInventories.size(), pick.getNumber());
            if (pickableInventories.size() == 0) {
                throw PickingException.raiseException("There's no inventory available from location " +
                        (Objects.isNull(pick.getSourceLocation()) ?
                                warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()).getName() :
                                pick.getSourceLocation().getName()) +
                        ", for item " +
                        (Objects.isNull(pick.getItem()) ?
                                inventoryServiceRestemplateClient.getItemById(pick.getItemId()) :
                                pick.getItem().getName()));
            }
            // pickableInventories.stream().forEach(System.out::print);
            logger.debug(" start to pick with quantity {}",quantityToBePicked);
            Iterator<Inventory> inventoryIterator = pickableInventories.iterator();
            while(quantityToBePicked > 0 && inventoryIterator.hasNext()) {
                Inventory inventory = inventoryIterator.next();
                if(match(inventory, pick)) {
                    logger.debug(" pick from inventory {}, quantity {},  into locaiton {}",
                            inventory.getLpn(), quantityToBePicked,  nextLocation.getName());
                    Long pickedQuantity = confirmPick(inventory, pick, quantityToBePicked, nextLocation, destinationLpn);
                    logger.debug(" >> we actually picked {} from the inventory", pickedQuantity);
                    quantityToBePicked -= pickedQuantity;
                    totalQuantityPicked += pickedQuantity;
                    logger.debug(" >> there's {} left in the pick work", quantityToBePicked);
                }
                else {
                    logger.debug("inventory {} doesn't match with pick {}",
                            inventory.getLpn(), pick.getNumber());
                }

            }
            logger.debug("==> after the pick confirm, the destination location {} 's volume is {}",
                    warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                    warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());

            // If we are picking for a work order, we will send a notification to the work order
            sendNotification(pick, nextLocation, totalQuantityPicked);
        }

        // Get the latest pick information
        Pick newPick = findById(pick.getId());
        orderActivity.setQuantityByNewPick(newPick);
        orderActivityService.sendOrderActivity(orderActivity);
        // if the pick work is fully picked and there's work task attached to it,
        // let's complete the work task
        if (newPick.getQuantity().equals(newPick.getPickedQuantity()) &&
            Objects.nonNull(newPick.getWorkTaskId())) {
            resourceServiceRestemplateClient.completeWorkTask(
                    newPick.getWarehouseId(), newPick.getWorkTaskId());
        }
        return newPick;

    }

    /**
     * Check if the destination LPN is in the right location. return true if this is a
     * new LPN that is not even in the system yet. return false if this is an existing LPN
     * and in the right location. throw exception if the LPN already exists but not in the
     * right location
     * @param destinationLocation
     * @param destinationLpn
     * @return
     */
    private void validatePickDestinationLpn(Long warehouseId, Location destinationLocation, String destinationLpn) {
        if (Strings.isBlank(destinationLpn)) {

            throw PickingException.raiseException("can't validate the destination LPN as it is empty ");
        }
        List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLpn(
                warehouseId, destinationLpn
        );
        if (inventories.isEmpty()) {
            return;
        }
        if (inventories.stream().anyMatch(inventory -> !inventory.getLocationId().equals(destinationLocation.getId()))) {
            throw PickingException.raiseException("Can't pick to " + destinationLpn + " as it is not in the right location " +
                    destinationLocation.getName());
        }
    }

    private void sendNotification(Pick pick, Location nextLocation, Long totalQuantityPicked) {
        /*
        *  We will move this piece of logic into the inventory movement, not the pick confirm
        * as we may move to RF first during pick confirm and then into production line
        * */
        /**
         *

        if (Objects.nonNull(pick.getWorkOrderLineId())) {
            workOrderServiceRestemplateClient.inventoryPickedForWorkOrderLine(
                    pick.getWorkOrderLineId(), totalQuantityPicked, nextLocation.getId()
            );


        }
         */
    }

    public List<Pick> getPicksByShipment(Long shipmentId){
        return pickRepository.getPicksByShipmentId(shipmentId);
    }

    public List<Pick> getPicksByShipmentLine(Long shipmentLineId){
        return pickRepository.getPicksByShipmentLineId(shipmentLineId);
    }

    public Long confirmPick(Inventory inventory, Pick pick, Long quantityToBePicked, Location nextLocation)   {
        return confirmPick(inventory, pick, quantityToBePicked, nextLocation, "");
    }
    @Transactional
    public Long confirmPick(Inventory inventory, Pick pick, Long quantityToBePicked, Location nextLocation, String destinationLpn)  {

        if (Strings.isNotBlank(destinationLpn)) {
            validatePickDestinationLpn(pick.getWarehouseId(), nextLocation, destinationLpn);
        }

        if (!match(inventory, pick)) {
            throw PickingException.raiseException( "inventory can't be picked for the pick. Attribute discrepancy found");
        }

        logger.debug("Start to pick from inventory\n inventory quantity: {} \n pick's quantity {} / {} "
                     , inventory.getQuantity(), quantityToBePicked, (pick.getQuantity() - pick.getPickedQuantity()));
        quantityToBePicked = Math.min(inventory.getQuantity(), quantityToBePicked);

        logger.debug(" Will pick {} from the inventory", quantityToBePicked);
        boolean pickWholeInventory = quantityToBePicked.equals(inventory.getQuantity());
        logger.debug(" Will pick whole inventory from LPN {} ? {}",
                inventory.getLpn(),
                pickWholeInventory);

        // If we are not to pick the whole inventory, we will split the original inventory
        // into 2 LPN and only move the right LPN for the pick
        Inventory inventoryToBePicked;
        if (pickWholeInventory) {
            inventoryToBePicked = inventory;
        }
        else {
            // We pick partial quantity from the inventory, Let's split it
            // and give it a new LPN
            String newLpn = commonServiceRestemplateClient.getNextNumber(pick.getWarehouseId(), "lpn");
            logger.debug("start to split inventory lpn {} into new lpn {} for the pick",
                    inventory.getLpn(), newLpn);
            List<Inventory> inventories = inventoryServiceRestemplateClient.split(inventory, newLpn, quantityToBePicked);
            if (inventories.size() != 2) {
                throw PickingException.raiseException("Inventory split for pick error! Inventory is not split into 2");
            }
            inventoryToBePicked = inventories.get(1);
        }
        logger.debug("Will pick from inventory {} into LPN {} ", inventoryToBePicked.getLpn(), destinationLpn);

        logger.debug("==> before move inventory, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());

        // Move the inventory to the next location for pick
        // Move the inventory to the next location
        Inventory pickedInventory =
                inventoryServiceRestemplateClient.moveInventory(inventoryToBePicked, pick, nextLocation);
        if (Strings.isNotBlank(destinationLpn)) {

            logger.debug("start to relabel the picked inventory from {} to {}",
                    pickedInventory.getLpn(), destinationLpn);
            pickedInventory = inventoryServiceRestemplateClient.relabelInventory(
                    pick.getWarehouseId(), pickedInventory.getId(), destinationLpn, true);
        }

        logger.debug("==> after move inventory, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());
        // update the quantity in the pick
        logger.debug(" change the picked quantity from {} to {}",
                pick.getPickedQuantity(), (pick.getPickedQuantity() + quantityToBePicked));
        pick.setPickedQuantity(pick.getPickedQuantity() + quantityToBePicked);
        pick.setPickedTime(ZonedDateTime.now());
        pick.setPickedByUsername(userService.getCurrentUserName());
        saveOrUpdate(pick);

        // Let's update the list if the pick belongs to any list
        pickListService.processPickConfirmed(pick);

        logger.debug("==> after processPickConfirmed, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());
        if (Objects.nonNull(pick.getShortAllocation())) {
            shortAllocationService.processPickConfirmed(pick, quantityToBePicked);
        }
        return quantityToBePicked;

    }

    private boolean match(Inventory inventory, Pick pick) {
        logger.debug("check if the inventory match with the pick");
        logger.debug("========            Inventory   ===========");
        logger.debug(inventory.getLpn());
        logger.debug("========            pick   ===========");
        logger.debug(pick.getNumber());
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


        // make sure the inventory attribute matches
        if (Objects.nonNull(pick.getItemPackageTypeId()) &&
                 !pick.getItemPackageTypeId().equals(inventory.getItemPackageType().getId())) {
            logger.debug("Inventory's Item package type doesn't match with Pick. \n " +
                            ">> Inventory's Item package type: {} \n " +
                            ">> Pick's Item package type: {}",
                    inventory.getItemPackageType().getId(), pick.getItemPackageTypeId());
            return false;
        }
        if (Strings.isNotBlank(pick.getColor()) &&
                !pick.getColor().equalsIgnoreCase(inventory.getColor())) {
            logger.debug("Inventory's color doesn't match with Pick. \n " +
                            ">> Inventory's color: {} \n " +
                            ">> Pick's color: {}",
                    inventory.getColor(), pick.getColor());
            return false;
        }
        if (Strings.isNotBlank(pick.getStyle()) &&
                !pick.getStyle().equalsIgnoreCase(inventory.getStyle())) {
            logger.debug("Inventory's style doesn't match with Pick. \n " +
                            ">> Inventory's style: {} \n " +
                            ">> Pick's style: {}",
                    inventory.getStyle(), pick.getStyle());
            return false;
        }
        if (Strings.isNotBlank(pick.getProductSize()) &&
                !pick.getProductSize().equalsIgnoreCase(inventory.getProductSize())) {
            logger.debug("Inventory's product size doesn't match with Pick. \n " +
                            ">> Inventory's product size: {} \n " +
                            ">> Pick's product size: {}",
                    inventory.getProductSize(), pick.getProductSize());
            return false;
        }
        if (Strings.isNotBlank(pick.getInventoryAttribute1()) &&
                !pick.getInventoryAttribute1().equalsIgnoreCase(inventory.getAttribute1())) {
            logger.debug("Inventory's attribute 1 doesn't match with Pick. \n " +
                            ">> Inventory's attribute 1: {} \n " +
                            ">> Pick's attribute 1: {}",
                    inventory.getAttribute1(), pick.getInventoryAttribute1());
            return false;
        }
        if (Strings.isNotBlank(pick.getInventoryAttribute2()) &&
                !pick.getInventoryAttribute2().equalsIgnoreCase(inventory.getAttribute2())) {
            logger.debug("Inventory's attribute 2 doesn't match with Pick. \n " +
                            ">> Inventory's attribute 2: {} \n " +
                            ">> Pick's attribute 2: {}",
                    inventory.getAttribute2(), pick.getInventoryAttribute2());
            return false;
        }
        if (Strings.isNotBlank(pick.getInventoryAttribute3()) &&
                !pick.getInventoryAttribute3().equalsIgnoreCase(inventory.getAttribute3())) {
            logger.debug("Inventory's attribute 3 doesn't match with Pick. \n " +
                            ">> Inventory's attribute 3: {} \n " +
                            ">> Pick's attribute 3: {}",
                    inventory.getAttribute3(), pick.getInventoryAttribute3());
            return false;
        }
        if (Strings.isNotBlank(pick.getInventoryAttribute4()) &&
                !pick.getInventoryAttribute4().equalsIgnoreCase(inventory.getAttribute4())) {
            logger.debug("Inventory's attribute 4 doesn't match with Pick. \n " +
                            ">> Inventory's attribute 4: {} \n " +
                            ">> Pick's attribute 4: {}",
                    inventory.getAttribute4(), pick.getInventoryAttribute4());
            return false;
        }
        if (Strings.isNotBlank(pick.getInventoryAttribute5()) &&
                !pick.getInventoryAttribute5().equalsIgnoreCase(inventory.getAttribute5())) {
            logger.debug("Inventory's attribute 5 doesn't match with Pick. \n " +
                            ">> Inventory's attribute 5: {} \n " +
                            ">> Pick's attribute 5: {}",
                    inventory.getAttribute5(), pick.getInventoryAttribute5());
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
    public List<Pick> unpick(Long id, Long unpickedQuantity) {
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
    public List<Pick> unpick(Pick pick, Long unpickedQuantity) {
        // Cancel the pick with unpicked quantity
        return cancelPick(pick, unpickedQuantity, false, false, false, false);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for order line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        pickRepository.processItemOverride(oldItemId, newItemId, warehouseId);
    }

    /**
     *
     * Manually pick an LPN for certain work order. The LPN has to have only one item number
     * @param warehouseId
     * @param workOrderId
     * @param lpn
     * @return
     */
    @Transactional
    public List<Pick> generateManualPickForWorkOrder(Long warehouseId, Long workOrderId, Long productionLineId,
                                                    String lpn, Long pickableQuantity) {
        WorkOrder workOrder = workOrderServiceRestemplateClient.getWorkOrderById(workOrderId, false, false);
        return generateManualPickForWorkOrder(warehouseId, workOrder, productionLineId, lpn, pickableQuantity);
    }
    /**
     * Manually pick an LPN for certain work order. The LPN has to have only one item number
     * @param workOrder
     * @param lpn
     * @return
     */
    @Transactional
    public List<Pick> generateManualPickForWorkOrder(Long warehouseId, WorkOrder workOrder, Long productionLineId,
                                                    String lpn, Long pickableQuantity) {
        if (Strings.isBlank(lpn)) {
            throw PickingException.raiseException("Can't generate the manual pick as LPN is empty");
        }
        List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLpn(warehouseId, lpn);
        if (inventories.size() == 0) {
            throw PickingException.raiseException("Can't find the LPN. Fail to generate the manual pick");

        }
        // let's make sure there's only one item number
        List<Long> itemIdList = inventories.stream().map(inventory -> inventory.getItem().getId()).distinct().collect(Collectors.toList());
        if (itemIdList.size() > 1) {
            throw PickingException.raiseException("The LPN is mixed with different item. Fail to generate the manual pick");
        }

        // make sure we can pick enough quantity from the LPN
        Long inventoryQuantity = inventories.stream().mapToLong(Inventory::getQuantity).sum();
        if (inventoryQuantity < pickableQuantity) {

            throw PickingException.raiseException("Can't pick quantity " + pickableQuantity + " from LPN " + lpn +
                    ". The LPN only have quantity " + inventoryQuantity + ". Fail to generate the manual pick");
        }


        // let's get the item id and find the matched work order line. We will create the pick against the
        // work order line
        Long itemId = itemIdList.get(0);
        Optional<WorkOrderLine> matchedWorkOrderLineOptional = workOrder.getWorkOrderLines().stream().filter(
                workOrderLine -> itemId.equals(workOrderLine.getItemId())).findFirst();

        WorkOrderLine matchedWorkOrderLine = null;
        Item itemToBeAllocated = null;
        if (matchedWorkOrderLineOptional.isPresent()) {
            matchedWorkOrderLine = matchedWorkOrderLineOptional.get();

            itemToBeAllocated = matchedWorkOrderLine.getItem();
            if (Objects.isNull(itemToBeAllocated) && Objects.nonNull(matchedWorkOrderLine.getItemId())) {
                itemToBeAllocated = inventoryServiceRestemplateClient.getItemById(matchedWorkOrderLine.getItemId());
                matchedWorkOrderLine.setItem(itemToBeAllocated);
            }
        }
        else {
            // let's see if we may need to pick spare part

            outerLoop:
            for (WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {
                for (WorkOrderLineSparePart workOrderLineSparePart : workOrderLine.getWorkOrderLineSpareParts()) {
                    for (WorkOrderLineSparePartDetail workOrderLineSparePartDetail : workOrderLineSparePart.getWorkOrderLineSparePartDetails()) {
                        if (itemId.equals(workOrderLineSparePartDetail.getItemId())) {
                            logger.debug("Get spare part based on the item id {}, item {} is a spare part for work order line {}",
                                    itemId, workOrderLineSparePartDetail.getItem().getName(),
                                    workOrderLine.getId());
                            matchedWorkOrderLine = workOrderLine;
                            itemToBeAllocated = workOrderLineSparePartDetail.getItem();

                            if (Objects.isNull(itemToBeAllocated) && Objects.nonNull(workOrderLineSparePartDetail.getItemId())) {
                                itemToBeAllocated = inventoryServiceRestemplateClient.getItemById(workOrderLineSparePartDetail.getItemId());
                                workOrderLineSparePartDetail.setItem(itemToBeAllocated);
                            }
                            break  outerLoop;
                        }
                    }
                }
            }

        }
        if (Objects.isNull(matchedWorkOrderLine) || Objects.isNull(itemToBeAllocated)) {
            throw  PickingException.raiseException("can't find the matched work order line with item id " + itemId);
        }

        logger.debug("we will pick item {} for work order line {} / {}",
                matchedWorkOrderLine.getItem().getName(),
                workOrder.getNumber(),
                matchedWorkOrderLine.getNumber());

        Location sourceLocation = inventories.get(0).getLocation();
        if (Objects.isNull(sourceLocation)) {
            sourceLocation = warehouseLayoutServiceRestemplateClient.getLocationById(
                    inventories.get(0).getLocationId()
            );
        }
        if (Objects.isNull(sourceLocation)) {
            throw PickingException.raiseException("Error finding the location for LPN " + lpn + ". Fail to generate the manual pick");
        }


        // let's see if we can generate the manual pick
        AllocationResult allocationResult = generateManualPickForWorkOrder(workOrder,
                matchedWorkOrderLine, itemToBeAllocated,  productionLineId, sourceLocation,
                pickableQuantity, lpn);

        if (allocationResult.getShortAllocations().size() > 0) {
            // ok, we get short allocation. Something seems goes wrong.

            // cancel all the short allocation and pick and then
            allocationResult.getShortAllocations().forEach(
                    shortAllocation -> shortAllocationService.delete(shortAllocation)
            );
            allocationResult.getPicks().forEach(
                    pick -> removePick(pick)
            );
            throw PickingException.raiseException("Error! can't allocate from this LPN " + lpn);
        }
        logger.debug("We finished manual allocation, will return {} picks",
                allocationResult.getPicks().size());


        return allocationResult.getPicks();



    }

    @Transactional
    public AllocationResult generateManualPickForOutboundShipment(ShipmentLine shipmentLine,
                                                                  Location sourceLocation,
                                                                  String lpn, Long pickableQuantity) {

        // we will need to make sure there's only one production line assigned to the work order
        // so that we can know the destination for the pick

        logger.debug("Start to allocate the shipment line {} to get a manual pick " +
                        " from location {} and lpn {}, with quantity {}",
                shipmentLine.getId(),
                sourceLocation.getName(),
                lpn, pickableQuantity);
        
        AllocationResult allocationResult
                = allocationService.allocate(shipmentLine, sourceLocation, true, lpn, pickableQuantity);


        return allocationResult;

    }

    @Transactional
    public AllocationResult generateManualPickForWorkOrder(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                                           Item item,
                                                           Long productionLineId,
                                                           Location sourceLocation,
                                                           Long quantity,
                                                           String lpn) {

        // we will need to make sure there's only one production line assigned to the work order
        // so that we can know the destination for the pick
        /***
         * ignore the validation since we will always pass in the production line
        if (workOrder.getProductionLineAssignments().size() != 1) {
            throw PickingException.raiseException("We can only manually pick the LPN for a work order that " +
                    " has one and only one assigned production line, so as to know the destination for this " +
                    " picked LPN");
        }
**/
        // Make sure the production line passed in is valid
        if (workOrder.getProductionLineAssignments().stream().noneMatch(
                productionLineAssignment ->
                        productionLineId.equals(productionLineAssignment.getProductionLine().getId())
        )) {
            throw PickingException.raiseException("production line id " + productionLineId +
                    " is invalid. Fail to generate manual pick for the work order " + workOrder.getNumber());

        }


        AllocationResult allocationResult
                = allocationService.allocate(workOrder, workOrderLine, item,  productionLineId,
                0l, quantity, sourceLocation, true, lpn);


        return allocationResult;

    }


    /**
     *
     * Generate manual pick work. This happens when the user manually generate the pick and finish the pick
     * by specify the LPN. The other way to generate the pick is by allocation, which the source location is
     * calculated by the system
     * @param allocationRequest
     * @param sourceLocation
     * @return
     */
    public AllocationResult generateManualPick(AllocationRequest allocationRequest, Location sourceLocation) {

        // check if we can generate manual pick from the location
        // for now we will always consider the existing picks from the location
        // TO-DO: Allow the user to ignore existing pick so that they may not be able to finish
        // the existing pick, if after the manual pick there's nothing left
        if (!validateGeneratingManualPick(allocationRequest, sourceLocation, false)) {
            throw PickingException.raiseException("can't generate a manual pick from location " +
                    sourceLocation.getName() + " by item: " + allocationRequest.getItem().getName() +
                    " quantity: " + allocationRequest.getQuantity());
        }

        // ok we should be able to generate picks from the location, let's try generate the pick
        return allocationService.tryAllocate(allocationRequest, sourceLocation);


    }

    @Transactional
    private boolean validateGeneratingManualPick(AllocationRequest allocationRequest, Location sourceLocation, boolean ignoreExistingPick) {

        Item item = allocationRequest.getItem();
        Long openQuantity = allocationRequest.getQuantity();
        InventoryStatus inventoryStatus = allocationRequest.getInventoryStatus();

        Long existingPickQuantity = 0l;
        if (!ignoreExistingPick) {

            List<Pick> existingPicks =
                    getOpenPicksByItemIdAndSourceLocation(item.getId(), sourceLocation);
            existingPickQuantity = existingPicks.stream().map(pick -> pick.getQuantity() - pick.getPickedQuantity())
                    .filter(quantity -> quantity >=0 ).mapToLong(Long::longValue).sum();

        }

        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                item.getId(), inventoryStatus.getId(),
                Objects.isNull(sourceLocation) ?  null : sourceLocation.getId(),
                null,
                allocationRequest.getColor(), allocationRequest.getProductSize(),
                allocationRequest.getStyle(),
                allocationRequest.getInventoryAttribute1(),
                allocationRequest.getInventoryAttribute2(),
                allocationRequest.getInventoryAttribute3(),
                allocationRequest.getInventoryAttribute4(),
                allocationRequest.getInventoryAttribute5(),
                allocationRequest.getAllocateByReceiptNumber());

        long pickableInventoryQuantity = pickableInventory.stream().map(inventory -> inventory.getQuantity())
                .filter(quantity -> quantity >=0 ).mapToLong(Long::longValue).sum();

        // see if we can still pick x amount of quantity from the location
        if (pickableInventoryQuantity >= existingPickQuantity + allocationRequest.getQuantity()) {
            // ok we can pick from this location with requested quantity
            return true;
        }
        else {
            return false;
        }
    }

    /**
     *
     * @param id
     * @param warehouseId
     * @return
     */
    public Pick releasePick(Long id, Long warehouseId) {

        Pick pick = findById(id);
        if (pick.getStatus().equals(PickStatus.RELEASED)) {
            logger.debug("pick {} is already released",
                    pick.getNumber());
            return pick;
        }
        if (!pick.getStatus().equals(PickStatus.PENDING)) {
            throw PickingException.raiseException(
                    "pick " + pick.getNumber() + " is not in pending status"
            );
        }
        return saveOrUpdate(pickReleaseService.releasePick(pick));
    }

    public Pick assignToUser(Long id, Long warehouseId, Long userId) {
        Pick pick = findById(id);
        // the pick has to be released and
        // there's already work task id attached to it
        if (!pick.getStatus().equals(PickStatus.RELEASED)) {
            throw PickingException.raiseException("You can only assign user to" +
                    " released picks");
        }
        if (Objects.isNull(pick.getWorkTaskId())) {

            throw PickingException.raiseException("You can only assign user to" +
                    " the pick that already released into the work task");
        }

        resourceServiceRestemplateClient.assingUser(warehouseId,
                pick.getWorkTaskId(), userId);

        return pick;
    }


    public Pick unassignUser(Long id, Long warehouseId) {
        Pick pick = findById(id);
        // the pick has to be released and
        // there's already work task id attached to it

        if (Objects.isNull(pick.getWorkTaskId())) {

            throw PickingException.raiseException("You can only assign user to" +
                    " the pick that already released into the work task");
        }

        resourceServiceRestemplateClient.unassingUser(warehouseId,
                pick.getWorkTaskId());

        return pick;
    }

    public Pick assignPickToList(Pick pick, PickList pickList) {
        pick.setPickList(pickList);
        return saveOrUpdate(pick, false);
    }

    public Long getQuantityInOrderPick(Long warehouseId, Long clientId, Long itemId, Long inventoryStatusId,
                                   String color, String productSize, String style,
                                       String inventoryAttribute1, String inventoryAttribute2, String inventoryAttribute3,
                                       String inventoryAttribute4, String inventoryAttribute5,
                                       boolean exactMatch,
                                   ClientRestriction clientRestriction) {
        List<Pick> picks = findAll(
                warehouseId, clientId, null, null, null,null,
                null, null,
                null, null,  null,
                itemId, null, null,
                null, null,
                null, true,
                inventoryStatusId,
                null,
                null,null,null,null,null,null,null,null,null,
                clientRestriction, false
        );
        picks = picks.stream().filter(
                pick -> matchPickAttributeWithInventoryAttribute(pick.getColor(), color, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getProductSize(), productSize, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getStyle(), style, exactMatch)&&
                        matchPickAttributeWithInventoryAttribute(pick.getInventoryAttribute1(), inventoryAttribute1, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getInventoryAttribute2(), inventoryAttribute2, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getInventoryAttribute3(), inventoryAttribute3, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getInventoryAttribute4(), inventoryAttribute4, exactMatch) &&
                        matchPickAttributeWithInventoryAttribute(pick.getInventoryAttribute5(), inventoryAttribute5, exactMatch)
        ).collect(Collectors.toList());

        return picks.stream().map(pick -> pick.getQuantity() > pick.getPickedQuantity() ?
                pick.getQuantity() - pick.getPickedQuantity() : 0l).mapToLong(Long::longValue).sum();
    }

    private boolean matchPickAttributeWithInventoryAttribute(String pickAttribute,
                                                                  String inventoryAttribute,
                                                                  boolean exactMatch) {
        if (Strings.isBlank(pickAttribute) && Strings.isBlank(inventoryAttribute)) {
            return true;
        }
        if (Strings.isBlank(pickAttribute)) {
            // the pick doesn't have any requirement on the attribute but the inventory
            // has the attribute, return true if we are not looking for an exact match
            return !exactMatch;
        }
        if (Strings.isBlank(inventoryAttribute)) {
            // the pick has order line attribute setup but the inventory doesn't have the attribute
            // we know for sure the inventory is not for the order line
            return false;
        }
        // both the pick and the inventory has the attribute setup, let's return true if they
        // have the same value
        return pickAttribute.equalsIgnoreCase(inventoryAttribute);
    }
    /**
     * Get the next pick from the pick pool
     * 1. single pick
     * 2. bulk pick
     * 3. list pick
     * Assigned to the user first
     * @param warehouseId
     * @param currentLocationId
     * @return
     */
    /***
    public GroupPick getNextPick(Long warehouseId, Long currentLocationId) {
        // get the current user first as we will always start from
        // the picks that are assigned to the user
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        User user = userService.getCurrentUser(warehouse.getCompanyId());
        if (Objects.isNull(user)) {
            throw ResourceNotFoundException.raiseException("Can't find the current user");
        }

        // let's get all the picks and sort by the
        // distance between the pick and the current location
        // for pick groups(bulk pick / list pick / carton pick), we will
        // use the first pick that closest to the current location
        // as the pick of the whole pick group
        Location currentLocation = warehouseLayoutServiceRestemplateClient.getLocationById(currentLocationId);

        // get all the open picks
        List<Pick> openPicks = findAll(warehouseId,
                null, null, null, null,
                null, null,
                null, null, null,
                null, null, null,
                null, null,
                null, true,null,
                null, null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);

        // only consider the open pick that is released, and not in any group

        // get best bulk pick that is assigned
        List<BulkPick> openBulkPicks = bulkPickService.findAll(warehouseId,
                null,null,null,null,null,
                null, null, null, null, null, null,
                true, null,null,null,false);

        GroupPick groupPick = getBestAssignedPicks(user, currentLocation, openPicks, openBulkPicks);

        if (Objects.nonNull(groupPick)) {
            // OK, we get the best pick from the assigned pick / pick groups
            logger.debug("We found the best pick {} / {} that assigned to the current user {}",
                    groupPick.getGroupType(),
                    groupPick.getNumber(),
                    user.getUsername()
                    );
            return groupPick;
        }
        logger.debug("There's no picks that is assigned to current user, get the best picks that is not assigned yet");
        return getBestUnassignedPicks(user, currentLocation, openPicks, openBulkPicks);


    }
     **/

    /**
    private GroupPick getBestUnassignedPicks(User user, Location currentLocation, List<Pick> openPicks, List<BulkPick> openBulkPicks) {
        // 1. get the picks that is not assigned.
        Pick bestUnassignedSinglePick = openPicks.stream().filter(
                pick -> Objects.isNull(pick.getAssignedToUserId()) &&
                        Objects.isNull(pick.getPickingByUserId()) &&
                        Objects.isNull(pick.getBulkPick()) && Objects.isNull(pick.getPickList())
                        && Objects.isNull(pick.getCartonization()) && pick.getStatus().equals(PickStatus.RELEASED)

        ).sorted((pick1, pick2) ->  sortPickBasedOnCurrentLocation(currentLocation, pick1, pick2)
        ).findFirst().orElse(null);


        BulkPick bestUnassignedBulkPick = openBulkPicks.stream().filter(
                bulkPick -> Objects.isNull(bulkPick.getAssignedToUserId()) &&
                        Objects.isNull(bulkPick.getPickingByUserId()) &&
                        bulkPick.getStatus().equals(PickStatus.RELEASED) &&
                        Objects.nonNull(bulkPick.getNextPick(currentLocation))
                // only return the bulk that still have open pick
        ).sorted((bulkPick1, bulkPick2) -> sortPickBasedOnCurrentLocation(currentLocation,
                bulkPick1.getNextPick(currentLocation), bulkPick2.getNextPick(currentLocation))
        ).findFirst().orElse(null);

        if(Objects.isNull(bestUnassignedSinglePick)) {
            return bestUnassignedBulkPick;
        }
        else if (Objects.isNull(bestUnassignedBulkPick)) {
            return bestUnassignedSinglePick;
        }
        else {
            // compare the first pick in the bulk and the best single pick
            Pick nextPickInBestBulkPick = bestUnassignedBulkPick.getNextPick(currentLocation);
            if (sortPickBasedOnCurrentLocation(currentLocation, bestUnassignedSinglePick, nextPickInBestBulkPick) > 0) {
                return bestUnassignedBulkPick;
            }
            else {
                return bestUnassignedSinglePick;
            }
        }
    }
**/
    /**
    private GroupPick getBestAssignedPicks(User user,
                                           Location currentLocation,
                                           List<Pick> openPicks, List<BulkPick> openBulkPicks) {
        // 1. get the picks that is assigned.
        Pick bestAssignedSinglePick = openPicks.stream().filter(
                pick -> user.getId().equals(pick.getAssignedToUserId()) &&
                        Objects.isNull(pick.getPickingByUserId()) &&
                        Objects.isNull(pick.getBulkPick()) && Objects.isNull(pick.getPickList())
                        && Objects.isNull(pick.getCartonization()) && pick.getStatus().equals(PickStatus.RELEASED)

        ).sorted((pick1, pick2) ->  sortPickBasedOnCurrentLocation(currentLocation, pick1, pick2)
        ).findFirst().orElse(null);


        BulkPick bestAssignedBulkPick = openBulkPicks.stream().filter(
                bulkPick -> user.getId().equals(bulkPick.getAssignedToUserId()) &&
                        Objects.isNull(bulkPick.getPickingByUserId()) &&
                        bulkPick.getStatus().equals(PickStatus.RELEASED) &&
                        Objects.nonNull(bulkPick.getNextPick(currentLocation))
                // only return the bulk that still have open pick
        ).sorted((bulkPick1, bulkPick2) -> sortPickBasedOnCurrentLocation(currentLocation,
                bulkPick1.getNextPick(currentLocation), bulkPick2.getNextPick(currentLocation))
        ).findFirst().orElse(null);

        if(Objects.isNull(bestAssignedSinglePick)) {
            return bestAssignedBulkPick;
        }
        else if (Objects.isNull(bestAssignedBulkPick)) {
            return bestAssignedSinglePick;
        }
        else {
            // compare the first pick in the bulk and the best single pick
            Pick nextPickInBestBulkPick = bestAssignedBulkPick.getNextPick(currentLocation);
            if (sortPickBasedOnCurrentLocation(currentLocation, bestAssignedSinglePick, nextPickInBestBulkPick) > 0) {
                return bestAssignedBulkPick;
            }
            else {
                return bestAssignedSinglePick;
            }
        }
    }
**/
    /**
    public int sortPickBasedOnCurrentLocation(Location currentLocation, Pick pick1, Pick pick2) {

        Long currentPickSequence = Objects.isNull(currentLocation) ?
                0 :
                Objects.isNull(currentLocation.getPickSequence()) ? 0 : currentLocation.getPickSequence();

        Location location1 = Objects.isNull(pick1.getSourceLocation()) ?
                warehouseLayoutServiceRestemplateClient.getLocationById(pick1.getSourceLocationId()) :
                pick1.getSourceLocation();

        Location location2 = Objects.isNull(pick2.getSourceLocation()) ?
                warehouseLayoutServiceRestemplateClient.getLocationById(pick2.getSourceLocationId()) :
                pick1.getSourceLocation();
        Long pickSequence1 = Objects.isNull(location1) ?
                0 :
                Objects.isNull(location1.getPickSequence()) ? 0 : location1.getPickSequence();
        Long pickSequence2 = Objects.isNull(location2) ?
                0 :
                Objects.isNull(location2.getPickSequence()) ? 0 : location2.getPickSequence();
        if (Math.abs(pickSequence1 - currentPickSequence) > Math.abs(pickSequence2 - currentPickSequence)) {
            return 1;
        }
        else {
            return -1;
        }
    }
     **/



    public ReportHistory generatePickReport(Long warehouseId, String ids, String locale) throws JsonProcessingException {
        List<Pick> picks = findAll(warehouseId, null,  null, null, null,
                null, null, null, null, null, ids,
                null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null,null,null,
                true);
        if (picks.isEmpty()) {
            throw PickingException.raiseException("can't find picks by " + ids);
        }
        return generatePickReportByBulkPick(warehouseId, picks, locale);
    }

    public ReportHistory generatePickReportByBulkPick(Long warehouseId, List<Pick> picks, String locale)
            throws JsonProcessingException {

        Report reportData = new Report();
        reportData.setParameters(new HashMap<>());

        setupPickReportParameters(
                reportData, picks
        );
        setupPickReportData(
                reportData, picks
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PICK_SHEET, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupPickReportParameters(
            Report report, List<Pick> picks) {

        report.addParameter("pickNumbers", picks.stream().map(Pick::getNumber).collect(Collectors.joining(",")));

    }
    private String getQuantityByUOM(Long quantity, List<Inventory> pickableInventory) {

        logger.debug("getQuantityByUOM by quantity {}", quantity);
        StringBuilder pickQuantityByUOM = new StringBuilder();
        pickQuantityByUOM.append(quantity);

        if (pickableInventory != null && !pickableInventory.isEmpty() &&
                Objects.nonNull(pickableInventory.get(0).getItemPackageType())) {
            // get the information from the first inventory of the list
            // we will assume all the pickable inventory in the same location
            // has the same item UOM information. If the location is mixed with
            // different package type, the warehouse may have some difficulty for picking
            ItemUnitOfMeasure stockItemUnitOfMeasure =
                    pickableInventory.get(0).getItemPackageType().getStockItemUnitOfMeasure();
            ItemUnitOfMeasure caseItemUnitOfMeasure =
                    pickableInventory.get(0).getItemPackageType().getCaseItemUnitOfMeasure();

            if (Objects.nonNull(stockItemUnitOfMeasure) &&
                    Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {

                logger.debug("stockItemUnitOfMeasure: {}", stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                pickQuantityByUOM.append(" ")
                        .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
            }
            // if the item package type has case UOM defined, show the quantity in case UOM as well.
            if (Objects.nonNull(caseItemUnitOfMeasure) &&
                    Objects.nonNull(caseItemUnitOfMeasure.getUnitOfMeasure())) {

                Long caseQuantity = quantity / caseItemUnitOfMeasure.getQuantity();
                Long leftOverQuantity = quantity % caseItemUnitOfMeasure.getQuantity();
                if (caseQuantity > 0) {


                    pickQuantityByUOM.append(" (").append(caseQuantity).append(" ")
                            .append(caseItemUnitOfMeasure.getUnitOfMeasure().getName());
                    if (leftOverQuantity > 0) {
                        pickQuantityByUOM.append(", ").append(leftOverQuantity);
                        if (Objects.nonNull(stockItemUnitOfMeasure) &&
                                Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {
                            pickQuantityByUOM.append(" ")
                                    .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                        }
                    }
                    logger.debug("caseItemUnitOfMeasure: {}", caseItemUnitOfMeasure.getUnitOfMeasure().getName());
                    pickQuantityByUOM.append(")");
                }
            }
        }

        return pickQuantityByUOM.toString();
    }
    private void setupPickReportData(
            Report report, List<Pick> picks) {


        // key: item id - inventory status id - source location id -color - product size - style
        Map<String, List<Inventory>> pickableInventoryMap = new HashMap<>();

        picks.forEach(
                pick -> {
                    String key = pick.getItemId() + "-" + pick.getInventoryStatusId() + "-" +
                                 pick.getSourceLocationId() + "-" + pick.getColor() + "-" +
                                 pick.getProductSize() + "-" + pick.getStyle();

                    List<Inventory> pickableInventory =  pickableInventoryMap.getOrDefault(key,
                                inventoryServiceRestemplateClient.getPickableInventory(
                                        pick.getItemId(), pick.getInventoryStatusId(), pick.getSourceLocationId(),
                                        pick.getColor(), pick.getProductSize(), pick.getStyle(),
                                        pick.getInventoryAttribute1(),
                                        pick.getInventoryAttribute2(),
                                        pick.getInventoryAttribute3(),
                                        pick.getInventoryAttribute4(),
                                        pick.getInventoryAttribute5(),
                                        null, null)
                            );
                    pickableInventoryMap.putIfAbsent(key, pickableInventory);

                    // set the inventory attribute in one string
                    StringBuilder inventoryAttribute = new StringBuilder()
                            .append(Strings.isBlank(pick.getColor()) ? "" : pick.getColor()).append("    ")
                            .append(Strings.isBlank(pick.getProductSize()) ? "" : pick.getProductSize()).append("    ")
                            .append(Strings.isBlank(pick.getStyle()) ? "" : pick.getStyle()).append("    ")
                            .append(Strings.isBlank(pick.getInventoryAttribute1()) ? "" : pick.getInventoryAttribute1()).append("    ")
                            .append(Strings.isBlank(pick.getInventoryAttribute2()) ? "" : pick.getInventoryAttribute2()).append("    ")
                            .append(Strings.isBlank(pick.getInventoryAttribute3()) ? "" : pick.getInventoryAttribute3()).append("    ")
                            .append(Strings.isBlank(pick.getInventoryAttribute4()) ? "" : pick.getInventoryAttribute4()).append("    ")
                            .append(Strings.isBlank(pick.getInventoryAttribute5()) ? "" : pick.getInventoryAttribute5()).append("    ")
                            .append(Strings.isBlank(pick.getAllocateByReceiptNumber()) ? "" : pick.getAllocateByReceiptNumber());
                    pick.setInventoryAttribute(inventoryAttribute.toString());

                    pick.setQuantityByUOM(getQuantityByUOM(pick.getQuantity(), pickableInventory));
                }
        );


        report.setData(picks);

    }

    public Pick acknowledgePick(Long warehouseId, Long id, String rfCode) {
        Pick pick = findById(id);
        String currentUserName = userService.getCurrentUserName();
        if (!isPickAcknowledgeableByUser(id, currentUserName, rfCode)) {
            throw PickingException.raiseException("pick  " + pick.getNumber() +
                    " is already acknowledged by " + pick.getAcknowledgedUsername() +
                    " by  " + pick.getAcknowledgedRFCode());
        }
        pick.setAcknowledgedUsername(currentUserName);
        pick.setAcknowledgedRFCode(rfCode);

        return saveOrUpdate(pick);

    }
    public Pick unacknowledgePick(Long warehouseId, Long id) {
        Pick pick = findById(id);
        pick.setAcknowledgedUsername(null);
        pick.setAcknowledgedRFCode(null);

        return saveOrUpdate(pick);

    }

    public Integer getPickCount(Long warehouseId, ClientRestriction clientRestriction) {
        List<Pick> picks =
                findAll(warehouseId, null, null, null, null, null,null,
                null, null,  null,null, null, null, null,
                null, null,  null,null, null, null, null,
                null, null, null, null, null, null, null, null, clientRestriction, false);

        return picks.size();

    }

    public Long getOpenPickCount(Long warehouseId, ClientRestriction clientRestriction) {

        List<Pick> picks =
                findAll(warehouseId, null, null, null, null, null,
                        null,
                        null, null,  null,null, null, null, null,
                        null, null,  null,true, null, null, null,
                        null, null, null, null, null, null, null, null, clientRestriction, false);

        return picks.stream().filter(
                pick -> !pick.getStatus().equals(PickStatus.CANCELLED) && !pick.getStatus().equals(PickStatus.COMPLETED)
        ).filter(pick -> pick.getPickedQuantity() < pick.getQuantity()).count();

    }
    public Long getCompletedPickCount(Long warehouseId, ClientRestriction clientRestriction) {
        List<Pick> picks =
                findAll(warehouseId, null, null, null, null, null, null,
                        null, null,  null,null, null, null, null,
                        null, null,  null,null, null, null, null,
                        null, null, null, null, null, null, null, null, clientRestriction, false);

        return picks.stream().filter(
                pick -> !pick.getStatus().equals(PickStatus.CANCELLED) && !pick.getStatus().equals(PickStatus.COMPLETED)
        ).filter(pick -> pick.getPickedQuantity() >= pick.getQuantity()).count();

    }
    public Map<String, Integer[]> getPickCountByLocationGroup(Long warehouseId, ClientRestriction clientRestriction) {
        List<Pick> picks =
                findAll(warehouseId, null, null, null, null, null, null,
                        null, null,  null,null, null, null, null,
                        null, null,  null,null, null, null, null,
                        null, null, null, null, null, null, null, null, clientRestriction, false);


        // key: location group name
        // value: total pick count, total completed
        Map<String, Integer> pickCountByLocationGroup = new HashMap<>();
        Map<String, Integer> completedPickCountByLocationGroup = new HashMap<>();
        // setup the locations for each pick
        picks.forEach(
                pick -> {
                    try {
                        // Load the details for client and supplier informaiton
                        if (pick.getSourceLocationId() != null && pick.getSourceLocation() == null) {
                            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
                        }
                        if (Objects.nonNull(pick.getSourceLocation()) && Objects.nonNull(pick.getSourceLocation().getLocationGroup())) {
                            int accumlativeQuantity = pickCountByLocationGroup.getOrDefault(
                                    pick.getSourceLocation().getLocationGroup().getName(),
                                    0
                            );
                            pickCountByLocationGroup.put(
                                    pick.getSourceLocation().getLocationGroup().getName(),
                                    accumlativeQuantity + 1
                            );
                            accumlativeQuantity = completedPickCountByLocationGroup.getOrDefault(
                                    pick.getSourceLocation().getLocationGroup().getName(),
                                    0
                            );
                            if (pick.getPickedQuantity() >= pick.getQuantity()) {

                                completedPickCountByLocationGroup.put(
                                        pick.getSourceLocation().getLocationGroup().getName(),
                                        accumlativeQuantity + 1
                                );
                            }
                            else {

                                completedPickCountByLocationGroup.put(
                                        pick.getSourceLocation().getLocationGroup().getName(),
                                        accumlativeQuantity
                                );
                            }
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();

                        // ignore any error when get information for the pick
                    }

                }
        );


        Map<String, Integer[]> result = new HashMap<>();
        pickCountByLocationGroup.entrySet().forEach(
                entry -> {
                    int totalPickCount = entry.getValue();
                    int totalCompletedPickCount =
                            completedPickCountByLocationGroup.getOrDefault(
                                    entry.getKey(), 0
                    );
                    result.put(entry.getKey(), new Integer[]{totalPickCount, totalCompletedPickCount});
                }
        );

        return result;


    }

    /**
     * See if the pick is a full pallet pick
     * @param pick
     * @return
     */
    public boolean isFullPalletPick(Pick pick) {
        if (Objects.isNull(pick.getItem())) {
            pick.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            pick.getItemId()
                    )
            );
        }
        ItemPackageType itemPackageType = pick.getItemPackageType();
        if (Objects.isNull(itemPackageType) && Objects.nonNull(pick.getItemPackageTypeId())) {
            itemPackageType = inventoryServiceRestemplateClient.getItemPackageTypeById(
                    pick.getItemPackageTypeId()
            );
        }
        if (Objects.isNull(itemPackageType)) {
            itemPackageType = pick.getItem().getDefaultItemPackageType();
        }
        logger.debug("see if we can find the tracking LPN UOM for item {}, item package type {}",
                pick.getItem().getName(),
                Objects.isNull(itemPackageType) ? "N/A" : itemPackageType.getName());
        logger.debug("Tracking LPN UOM: {}",
                Objects.isNull(itemPackageType.getTrackingLpnUOM()) ? "N/A" :
                    itemPackageType.getTrackingLpnUOM().getUnitOfMeasure().getName());
        // see if the pick's quantity is more than the LPN quantity
        if (Objects.isNull(itemPackageType.getTrackingLpnUOM())) {
            // there's no tracking LPN uom defined, let's see if there's
            // maximun pallet size defined for outbound
            OutboundConfiguration outboundConfiguration = outboundConfigurationService.findByWarehouse(pick.getWarehouseId());
            if (Objects.nonNull(outboundConfiguration) && Objects.nonNull(outboundConfiguration.getMaxPalletSize())
                && outboundConfiguration.getMaxPalletSize() > 0) {
                logger.debug("the max pallet size is defined for this warehouse, " +
                                "see if the pick's size {} is greater than the pallet size {}",
                        pick.getSize(unitService),
                        outboundConfiguration.getMaxPalletSize());
                return pick.getSize(unitService).getFirst() >= outboundConfiguration.getMaxPalletSize();
            }
            logger.debug("the item doesn't have a tracking LPN uom defined and there's no max pallet size defined for the outbound, " +
                    "let's always assume that the pick is a full pallet pick");
            return true;
        }
        return pick.getQuantity() >= itemPackageType.getTrackingLpnUOM().getQuantity();
    }

    public void confirmManualPickForOrder(
            Long warehouseId,
            Long clientId,
            String orderNumber,
            String lpn,
            boolean completeOrderAfterFullyPicked,
            ClientRestriction clientRestriction)  {

        Order order = orderService.findByNumber(warehouseId, clientId, orderNumber, false);
        if (Objects.isNull(order)) {
            throw OrderOperationException.raiseException("Can't find order by number " + orderNumber);
        }
        List<Pick> picks = orderService.generateManualPick(order, lpn, true);

        if (picks.isEmpty()) {
            throw OrderOperationException.raiseException("Fail to generate picks for number " + orderNumber +
                    ", from LPN " + lpn);
        }

        logger.debug("We generated {} picks for manual pick from order {}, lpn {}, " +
                "let's confirm all of them",
                picks.size(), orderNumber, lpn);


        picks.forEach(
                pick -> {
                    confirmPick(pick.getId(), pick.getQuantity(),
                            null, null,
                            false,null,
                            lpn, null);
                    logger.debug("Pick with number {} is confirmed with quantity {}",
                            pick.getNumber(), pick.getQuantity());
                }
        );

        // see if the order is fully picked
        logger.debug("do we need to check if we can complete the order after the pick? {}",
                completeOrderAfterFullyPicked);
        if (completeOrderAfterFullyPicked && orderService.validateOrderIsFullyStaged(order.getId())) {
            orderService.completeOrder(order.getId(), order);
        }

    }

    public Boolean isPickAcknowledgeableByCurrentUser(Long warehouseId, Long id, String rfCode) {

        String currentUserName = userService.getCurrentUserName();
        // make sure the pick is either no one is acknowledged yet,
        // or already acknowledged by the same user with same rf device

        return isPickAcknowledgeableByUser(id, currentUserName, rfCode);
    }

    public Boolean isPickAcknowledgeableByUser(Long id, String username,  String rfCode) {

        Pick pick = findById(id);
        // make sure the pick is either no one is acknowledged yet,
        // or already acknowledged by the same user with same rf device

        return (pick.getQuantity() > pick.getPickedQuantity()) &&
                    (Strings.isBlank(pick.getAcknowledgedUsername()) ||
                    pick.getAcknowledgedUsername().equalsIgnoreCase(username)) &&
                    (Strings.isBlank(pick.getAcknowledgedRFCode()) ||
                            pick.getAcknowledgedRFCode().equalsIgnoreCase(rfCode)) ;
    }
}

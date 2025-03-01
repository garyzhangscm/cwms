package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.KafkaSender;
import com.garyzhangscm.cwms.outbound.clients.WorkOrderServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class AllocationService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationService.class);

    @Autowired
    private AllocationStrategyFactory allocationStrategyService;
    @Autowired
    private PickService pickService;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Autowired
    private UserService userService;


    @Autowired
    private AllocationRequestService allocationRequestService;

    @Autowired
    KafkaSender kafkaSender;



    /**
     * Allocate the shipment line
     * @param shipmentLine shipment line to be allocated
     */
    public AllocationResult allocate(ShipmentLine shipmentLine,
                                     Set<Long> skipLocations){
        // Allocate the shipment with the allocation strategyp type specified in the order line
        logger.debug("start to allocate shipment line: {} / {}, with skip locations {}",
                shipmentLine.getShipmentNumber(), shipmentLine.getNumber(),
                skipLocations);
        return allocate(shipmentLine, shipmentLine.getOrderLine().getAllocationStrategyType(),
                skipLocations);
    }

    /**
     * Allocate the shipment line with a user specified allocation strategy type
     * @param shipmentLine shipment line to be allocated
     * @param allocationStrategyType user specified allocation stratetyp type
     */
    public AllocationResult allocate(ShipmentLine shipmentLine,
                                     AllocationStrategyType allocationStrategyType,
                                     Set<Long> skipLocations){
        AllocationRequest allocationRequest = allocationRequestService.getAllocationRequest(shipmentLine, skipLocations);


        // save the allocate request to Kafka so that we will allocate later

        /**
            logger.debug("Start to save allocation request to Kafka");
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            userService.addUserServletRequestAttribute(oauth2ClientContext.getAccessToken().getValue(), servletRequestAttributes);
            kafkaSender.send("ALLOCATION_REQUEST", oauth2ClientContext.getAccessToken(), allocationRequest);

            logger.debug("Allocation request saved!");
        */

        // If we specify the allocation strategy type, then override the one
        // from the shipment line(order line)

        AllocationResult allocationResult = tryAllocate(allocationRequest);
        persistAllocationResult(allocationResult);
        return allocationResult;
    }

    @Transactional
    public AllocationResult allocate(ShipmentLine shipmentLine,
                                     Location sourceLocation,
                                     boolean manualAllocation,
                                     String lpn, Long pickableQuantity){

        AllocationRequest allocationRequest = allocationRequestService.getAllocationRequest(shipmentLine);
        allocationRequest.setQuantity(pickableQuantity);
        allocationRequest.setManualAllocation(manualAllocation);
        allocationRequest.setLpn(lpn);

        // If we specify the allocation strategy type, then override the one
        // from the shipment line(order line)

        AllocationResult allocationResult = tryAllocate(allocationRequest, sourceLocation);

        logger.debug("We got {} picks, {} short allocations for order line {} / {}, ",
                allocationResult.getPicks().size(),
                allocationResult.getShortAllocations().size(),
                shipmentLine.getOrderLine().getOrderNumber(),
                shipmentLine.getOrderLine().getNumber());

        return allocationResult;
    }


    public AllocationResult allocate(Long workOrderId, WorkOrderLine workOrderLine, Long productionLineId, Long allocatingWorkOrderLineQuantity){
        logger.debug("Start to allocate Work Order Line {} ", workOrderLine.getId());

        AllocationResult allocationResult = new AllocationResult();
        WorkOrder workOrder = workOrderServiceRestemplateClient.getWorkOrderById(workOrderId);
        // only allow the work order line that has open quantity
        if (workOrderLine.getOpenQuantity() > 0) {

            allocationResult
                    = allocate(workOrder, workOrderLine, productionLineId, null, allocatingWorkOrderLineQuantity);
        }
        return allocationResult;
    }
    public AllocationResult allocate(WorkOrder workOrder, Long productionLineId, Long allocatingWorkOrderQuantity){
        logger.debug("Start to allocate Work Order {} ", workOrder.getNumber());

        workOrder.getProductionLineAssignments().forEach(
                productionLineAssignment -> {
                    logger.debug("production line {} is assigned quantity {}",
                            productionLineAssignment.getProductionLine().getName(),
                            productionLineAssignment.getQuantity());
                    if (Objects.nonNull(productionLineId)) {
                        logger.debug("Production Line ID is passsed in, we will only allocate for certain production line");
                        logger.debug("Current production id: {}, required production id: {}, current production line will be used? {}",
                                productionLineAssignment.getProductionLine().getId(), productionLineId,
                                productionLineId.equals(productionLineAssignment.getProductionLine().getId()));

                    }
                    if (Objects.nonNull(allocatingWorkOrderQuantity)) {
                        logger.debug("Quantity is specified, we will only allocate {}", allocatingWorkOrderQuantity);
                    }

                }

        );

        AllocationResult allocationResult = new AllocationResult();

        // only allow the work order that has open quantity
        workOrder.getWorkOrderLines()
                .stream()
                .filter(workOrderLine ->
                    workOrderLine.getOpenQuantity() > 0)
                .forEach(workOrderLine -> {
                        // if we have multiple production lines, we may need to allocate
                        AllocationResult workOrderAllocationResult
                                = allocate(workOrder, workOrderLine, productionLineId, allocatingWorkOrderQuantity);
                        allocationResult.addPicks(workOrderAllocationResult.getPicks());
                        allocationResult.addShortAllocations(workOrderAllocationResult.getShortAllocations());
                    });
        return allocationResult;

    }

    /**
     * Allocate the work order line according to the quantity of the work order's finish good's quantity
     * @param workOrder
     * @param workOrderLine
     * @param productionLineId
     * @param allocatingWorkOrderQuantity finish good's quantity
     * @return
     */
    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Long productionLineId, Long allocatingWorkOrderQuantity){
        return allocate(workOrder, workOrderLine, workOrderLine.getItem(), productionLineId, allocatingWorkOrderQuantity, 0L);

    }



    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Item item,
                                     Long productionLineId, Long allocatingWorkOrderQuantity){
        return allocate(workOrder, workOrderLine, item, productionLineId, allocatingWorkOrderQuantity, 0L);

    }

    /**
     * Allocate the work order line based on either the finish good's quantity, or the raw material quantity
     * @param workOrder
     * @param workOrderLine
     * @param productionLineId
     * @param allocatingWorkOrderQuantity
     * @param allocatingWorkingOrderLineQuantity
     * @return
     */

    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Long productionLineId, Long allocatingWorkOrderQuantity,
                                     Long allocatingWorkingOrderLineQuantity){
        return allocate(workOrder, workOrderLine, workOrderLine.getItem(), productionLineId,
                allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity, null, false);
    }

    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Item item,
                                     Long productionLineId, Long allocatingWorkOrderQuantity,
                                     Long allocatingWorkingOrderLineQuantity){
        return allocate(workOrder, workOrderLine, item, productionLineId,
                allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity, null, false);
    }

    @Transactional
    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Item item,
                                     Long productionLineId, Long allocatingWorkOrderQuantity,
                                     Long allocatingWorkingOrderLineQuantity,
                                     Location sourceLocation,
                                     boolean manualAllocation) {
        return allocate(workOrder, workOrderLine, item, productionLineId,
                allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity,
                sourceLocation, manualAllocation, "");
    }
    @Transactional
    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine,
                                     Item item,
                                     Long productionLineId, Long allocatingWorkOrderQuantity,
                                     Long allocatingWorkingOrderLineQuantity,
                                     Location sourceLocation,
                                     boolean manualAllocation,
                                     String lpn){

        // for work order, we may have multiple production lines assign to this work order
        // in order to generate picks for each production line, we may have to allocate
        // based on the work order line and production line
        // after that, we will sum up everything and return the allocate result as a whole
        AllocationResult fullAllocationResult = new AllocationResult();
        Stream<ProductionLineAssignment> productionLineAssignmentStream = workOrder.getProductionLineAssignments().stream();
        if (Objects.nonNull(productionLineId)) {
            productionLineAssignmentStream = productionLineAssignmentStream
                    .filter(productionLineAssignment -> productionLineId.equals(productionLineAssignment.getProductionLine().getId()));
        }

        productionLineAssignmentStream.forEach(
                productionLineAssignment -> {

                    logger.debug("start to allocate work order {} / {} / {} for production line {} / {} / {}, from location {}",
                            workOrder.getNumber(),
                            workOrderLine.getItem().getName(), item.getName(),
                            productionLineAssignment.getProductionLine().getName(),
                            productionLineAssignment.getProductionLine().getInboundStageLocationId(),
                            productionLineAssignment.getProductionLine().getInboundStageLocation() == null ?
                                    "N/A" : productionLineAssignment.getProductionLine().getInboundStageLocation().getId(),
                            Objects.isNull(sourceLocation) ? "N/A" : sourceLocation.getName());


                    AllocationRequest allocationRequest = allocationRequestService.getAllocationRequest(
                            workOrder, workOrderLine, item, productionLineAssignment
                            , allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity);

                    allocationRequest.setManualAllocation(manualAllocation);
                    allocationRequest.setLpn(lpn);

                    logger.debug("will allocate the work order to destination location id {} for quantity {}",
                            allocationRequest.getDestinationLocationId(),
                            allocationRequest.getQuantity());
                    // If we specify the allocation strategy type, then override the one
                    // from the shipment line(order line)

                    AllocationResult allocationResult = tryAllocate(allocationRequest, sourceLocation);

                    logger.debug("We got {} picks, {} short allocations for work order line {} / {}, ",
                            allocationResult.getPicks().size(),
                            allocationResult.getShortAllocations().size(),
                            workOrder.getNumber(),
                            workOrderLine.getNumber());


                    fullAllocationResult.merge(allocationResult);

                }
        );
        persistAllocationResult(fullAllocationResult);
        return fullAllocationResult;
    }

    /**
     * Try allocate the allocation request. We will only generate the result but
     * won't persist the result.
     * We won't generate any pick group(like pick list / carton / etc) as well.
     * We use this function to get a temporary result, compare it with other
     * result generated by other allocation rules and see which works best for us
     * @param allocationRequest Allocation Request
     * @return Allocation Result
     */
    public AllocationResult tryAllocate(AllocationRequest allocationRequest) {

        return tryAllocate(allocationRequest, null);
    }


    @Transactional
    public AllocationResult tryAllocate(AllocationRequest allocationRequest, Location sourceLocation) {

        logger.debug("Start to allocate request: \n " +
                        "Item: {} \n" +
                        "Quantity: {}",
                    allocationRequest.getItem().getName(),
                allocationRequest.getQuantity());

        // Let's get the allocation strategy that assigned to the request

        // For now we only allow one strategy per request. We will ignore
        // all allocation strategy other than the first one

        AllocationStrategy allocationStrategy =
                allocationRequest.getAllocationStrategyTypes().size() > 0 ?
                        allocationStrategyService.getAllocationStrategyByType(
                                allocationRequest.getAllocationStrategyTypes().get(0)
                        ).orElse(allocationStrategyService.getDefaultAllocationStrategy())
                        :
                        allocationStrategyService.getDefaultAllocationStrategy();
        logger.debug("Will allocate with strategy: {}", allocationStrategy.getClass());
        return allocationStrategy.allocate(allocationRequest, sourceLocation);


    }
    public void persistAllocationResult(AllocationResult allocationResult) {

    }

}

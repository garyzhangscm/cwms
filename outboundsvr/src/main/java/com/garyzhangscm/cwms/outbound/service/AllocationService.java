package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AllocationService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationService.class);

    @Autowired
    private AllocationStrategyFactory allocationStrategyService;
    @Autowired
    private PickService pickService;

    /**
     * Allocate the shipment line
     * @param shipmentLine shipment line to be allocated
     */
    public AllocationResult allocate(ShipmentLine shipmentLine){
        // Allocate the shipment with the allocation strategyp type specified in the order line
        logger.debug("start to allocate shipment line: {} / {}",
                shipmentLine.getShipmentNumber(), shipmentLine.getNumber());
        return allocate(shipmentLine, shipmentLine.getOrderLine().getAllocationStrategyType());
    }

    /**
     * Allocate the shipment line with a user specified allocation strategy type
     * @param shipmentLine shipment line to be allocated
     * @param allocationStrategyType user specified allocation stratetyp type
     */
    public AllocationResult allocate(ShipmentLine shipmentLine, AllocationStrategyType allocationStrategyType){
        AllocationRequest allocationRequest = new AllocationRequest(shipmentLine);
        // If we specify the allocation strategy type, then override the one
        // from the shipment line(order line)

        AllocationResult allocationResult = tryAllocate(allocationRequest);
        persistAllocationResult(allocationResult);
        return allocationResult;
    }

    public AllocationResult allocate(WorkOrder workOrder){
        logger.debug("Start to allocate Work Order {} ", workOrder.getNumber());

        workOrder.getProductionLineAssignments().forEach(
                productionLineAssignment ->
                        logger.debug("production line {} is assigned quantity {}",
                                productionLineAssignment.getProductionLine().getName(),
                                productionLineAssignment.getQuantity())
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
                                = allocate(workOrder, workOrderLine);
                        allocationResult.addPicks(workOrderAllocationResult.getPicks());
                        allocationResult.addShortAllocations(workOrderAllocationResult.getShortAllocations());
                    });
        return allocationResult;

    }

    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine){

        // for work order, we may have multiple production lines assign to this work order
        // in order to generate picks for each production line, we may have to allocate
        // based on the work order line and production line
        // after that, we will sum up everything and return the allocate result as a whole
        AllocationResult fullAllocationResult = new AllocationResult();

        workOrder.getProductionLineAssignments().forEach(
                productionLineAssignment -> {
                    logger.debug("start to allocate work order {} / {} for production line {} / {} / {}",
                            workOrder.getNumber(),
                            workOrderLine.getItem().getName(),
                            productionLineAssignment.getProductionLine().getName(),
                            productionLineAssignment.getProductionLine().getInboundStageLocationId(),
                            productionLineAssignment.getProductionLine().getInboundStageLocation() == null ?
                                    "N/A" : productionLineAssignment.getProductionLine().getInboundStageLocation().getId());


                    AllocationRequest allocationRequest = new AllocationRequest(workOrder, workOrderLine, productionLineAssignment);
                    logger.debug("will allocate the work order to destination location id {} for quantity {}",
                            allocationRequest.getDestinationLocationId(),
                            allocationRequest.getQuantity());
                    // If we specify the allocation strategy type, then override the one
                    // from the shipment line(order line)

                    AllocationResult allocationResult = tryAllocate(allocationRequest);

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
        return allocationStrategy.allocate(allocationRequest);


    }
    public void persistAllocationResult(AllocationResult allocationResult) {

    }

}

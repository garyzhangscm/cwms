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

        AllocationResult allocationResult = new AllocationResult();

        workOrder.getWorkOrderLines().forEach(workOrderLine -> {
            AllocationResult workOrderAllocationResult
                    = allocate(workOrder, workOrderLine);
            allocationResult.addPicks(workOrderAllocationResult.getPicks());
            allocationResult.addShortAllocations(workOrderAllocationResult.getShortAllocations());
        });
        return allocationResult;

    }

    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine){

        AllocationRequest allocationRequest = new AllocationRequest(workOrder, workOrderLine);
        // If we specify the allocation strategy type, then override the one
        // from the shipment line(order line)

        AllocationResult allocationResult = tryAllocate(allocationRequest);
        logger.debug("We got {} picks, {} short allocations for work order line {} / {}, ",
                allocationResult.getPicks().size(),
                allocationResult.getShortAllocations().size(),
                workOrder.getNumber(),
                workOrderLine.getNumber());
        persistAllocationResult(allocationResult);
        return allocationResult;
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
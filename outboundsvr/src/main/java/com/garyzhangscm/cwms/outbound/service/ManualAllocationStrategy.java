package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.AllocationException;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ShortAllocationException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.NotSupportedException;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManualAllocationStrategy implements AllocationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ManualAllocationStrategy.class);

    @Autowired
    protected PickService pickService;

    @Autowired
    private AllocationTransactionHistoryService allocationTransactionHistoryService;

    @Autowired
    protected InventorySummaryService inventorySummaryService;

    @Autowired
    protected ShortAllocationService shortAllocationService;

    @Autowired
    protected InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    @Override
    public AllocationStrategyType getType() {
        return AllocationStrategyType.MANUAL_ALLOCATION;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public AllocationResult allocate(AllocationRequest allocationRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AllocationResult allocate(AllocationRequest allocationRequest, Location sourceLocation) {
        // for manual allocated request, we should already have the LPN specified
        // let's validate if the LPN is a pickable LPN and then we will directly generate the
        // pick


        logger.debug("Start to allocate request with Manual Strategy. \n item: {} / {} \n quantity: {} \n inventory status: {}, from location {}",
                allocationRequest.getItem().getId(),
                allocationRequest.getItem().getName(),
                allocationRequest.getQuantity(),
                allocationRequest.getInventoryStatus().getName(),
                Objects.isNull(sourceLocation) ? "N/A" : sourceLocation.getName());

        AllocationResult allocationResult = new AllocationResult();

        if (Strings.isBlank(allocationRequest.getLpn())) {
            throw PickingException.raiseException("LPN is missing, not able to generate manual pick");
        }

        Item item = allocationRequest.getItem();
        InventoryStatus inventoryStatus = allocationRequest.getInventoryStatus();

        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                item.getId(), inventoryStatus.getId(),
                Objects.isNull(sourceLocation) ?  null : sourceLocation.getId(),
                allocationRequest.getLpn(),
                allocationRequest.getColor(),
                allocationRequest.getProductSize(),
                allocationRequest.getStyle(),
                allocationRequest.getInventoryAttribute1(),
                allocationRequest.getInventoryAttribute2(),
                allocationRequest.getInventoryAttribute3(),
                allocationRequest.getInventoryAttribute4(),
                allocationRequest.getInventoryAttribute5(),
                allocationRequest.getAllocateByReceiptNumber(), null);

        logger.debug("We have {} pickable inventory of this item, location specified? {}",
                pickableInventory.size(),
                Objects.isNull(sourceLocation) ? "N/A" : sourceLocation.getName());

        pickableInventory.forEach(
                inventory ->   allocationResult.addPick(tryCreatePickForManualAllocation(allocationRequest, inventory))
        );

        Long totalPickQuantity = allocationResult.getPicks().stream().map(Pick::getQuantity).mapToLong(Long::longValue).sum();
        Long openQuantity = allocationRequest.getQuantity() - totalPickQuantity;
        logger.debug("after manual allocation, we get total pick quantity {}, open quantity {}",
                totalPickQuantity, openQuantity);
        if (openQuantity > 0) {

            allocationResult.addShortAllocation(generateShortAllocation(allocationRequest, item, openQuantity));
        }

        return allocationResult;

    }

    /**
     * @param allocationRequest
     * @param item
     * @param quantity
     * @return
     */
    private ShortAllocation generateShortAllocation(AllocationRequest allocationRequest, Item item, Long quantity) {

        logger.debug("Start to generate short allocation for item {}, quantity {}",
                item.getName(), quantity);
        // For now we will only support allocate one line by one line
        // either shipment line or work order line
        if (allocationRequest.getShipmentLines().size() > 0) {
            return generateShortAllocation(item, allocationRequest.getShipmentLines().get(0), quantity);
        }

        else if (Objects.nonNull(allocationRequest.getWorkOrder()) &&
                allocationRequest.getWorkOrderLines().size() > 0) {
            return generateShortAllocation(allocationRequest.getWorkOrder(),
                    item, allocationRequest.getWorkOrderLines().get(0), quantity);

        }
        else {
            throw ShortAllocationException.raiseException("Can't generate short allocation for allocation request: ");
        }

    }

    private ShortAllocation generateShortAllocation(Item item, ShipmentLine shipmentLine, Long quantity) {

        return shortAllocationService.generateShortAllocation(item, shipmentLine, quantity);

    }

    private ShortAllocation generateShortAllocation(WorkOrder workOrder, Item item, WorkOrderLine workOrderLine, Long quantity) {

        return shortAllocationService.generateShortAllocation(workOrder, item, workOrderLine, quantity);

    }

    @Transactional(dontRollbackOn = GenericException.class)
    private Pick tryCreatePickForManualAllocation(AllocationRequest allocationRequest, Inventory inventory) {
        long pickQuantity = Math.min(allocationRequest.getQuantity(), inventory.getQuantity());

        if (allocationRequest.getShipmentLines().size() > 1) {

            throw PickingException.raiseException("We can't only proceed manual pick for order, one line at a time");
        }
        else if (allocationRequest.getShipmentLines().size() == 1) {
            return pickService.generatePick(allocationRequest.getShipmentLines().get(0),
                    inventory,
                    pickQuantity, inventory.getItemPackageType().getStockItemUnitOfMeasure(),
                    false);
        }
        else if(allocationRequest.getWorkOrder() != null &&
                allocationRequest.getWorkOrderLines().size() > 0) {
            return pickService.generatePick(allocationRequest.getWorkOrder() ,
                    inventory, allocationRequest.getWorkOrderLines().get(0),
                    pickQuantity, inventory.getItemPackageType().getStockItemUnitOfMeasure(),
                    allocationRequest.getDestinationLocationId(), false);
        }
        else {
            throw AllocationException.raiseException("now we only support new allocation for single shipment line or single work order line");
        }

    }



}

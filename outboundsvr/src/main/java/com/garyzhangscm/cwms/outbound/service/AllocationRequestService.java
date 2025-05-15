package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;


@Service
public class AllocationRequestService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationRequestService.class);

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public AllocationRequest getAllocationRequest(
            WorkOrder workOrder,
            WorkOrderLine workOrderLine,
            Item item,
            ProductionLineAssignment productionLineAssignment,
            Long allocatingWorkOrderQuantity,
            Long allocatingWorkingOrderLineQuantity) {

        if (Objects.isNull(workOrderLine.getItem()) && Objects.nonNull(workOrderLine.getItemId())) {
            workOrderLine.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            workOrderLine.getItemId()
                    )
            );
        }

        if (Objects.isNull(workOrderLine.getInventoryStatus()) && Objects.nonNull(workOrderLine.getInventoryStatusId())) {
            workOrderLine.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            workOrderLine.getInventoryStatusId()
                    )
            );
        }

        return new AllocationRequest(workOrder, workOrderLine, item, productionLineAssignment
                , allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity);


    }

    public AllocationRequest getAllocationRequest(
            ShipmentLine shipmentLine,
            Set<Long> skipLocations
    ) {

        return new AllocationRequest(shipmentLine, skipLocations);


    }

    public AllocationRequest getAllocationRequest(
            ShipmentLine shipmentLine
    ) {

        return new AllocationRequest(shipmentLine);


    }

}

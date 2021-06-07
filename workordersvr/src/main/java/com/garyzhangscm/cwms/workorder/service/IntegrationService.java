package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.KafkaSender;
import com.garyzhangscm.cwms.workorder.model.*;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);


    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private WorkOrderByProductService workOrderByProductService;
    @Autowired
    private WorkOrderInstructionService workOrderInstructionService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;

    public void process(WorkOrderConfirmation workOrderConfirmation) {

        kafkaSender.send(workOrderConfirmation);
        logger.debug(">> work order confirmation sent!");

    }

    public void process(WorkOrder workOrder) {


        workOrder.setProducedQuantity(0L);
        workOrder.setStatus(WorkOrderStatus.PENDING);

        workOrderService.saveOrUpdate(workOrder, false);

        // We will init the work order with some default 0 quantity
        // Since when we get work order from integration, the integration
        // data will only need to specify the expect quantity
        workOrder.getWorkOrderLines().forEach(workOrderLine -> {


            workOrderLine.setOpenQuantity(workOrderLine.getExpectedQuantity());
            workOrderLine.setInprocessQuantity(0L);
            //TO-DO: Default to FIFO for now
            workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);
            workOrderLine.setWorkOrder(workOrder);
            workOrderLineService.saveOrUpdate(workOrderLine, false);

        });

        workOrder.getWorkOrderByProducts().forEach(workOrderByProduct -> {

            workOrderByProduct.setWorkOrder(workOrder);
            workOrderByProduct.setProducedQuantity(0L);
            workOrderByProductService.save(workOrderByProduct, false);
        });

        workOrder.getWorkOrderInstructions().forEach(workOrderInstruction -> {

            workOrderInstruction.setWorkOrder(workOrder);
            workOrderInstructionService.save(workOrderInstruction);
        });
        logger.debug(">> work order information saved!");

    }

    public void process(BillOfMaterial billOfMaterial) {

        billOfMaterial.getBillOfMaterialLines().forEach(billOfMaterialLine -> {

            billOfMaterialLine.setBillOfMaterial(billOfMaterial);
        });

        billOfMaterial.getBillOfMaterialByProducts().forEach(billOfMaterialByProduct -> {

            billOfMaterialByProduct.setBillOfMaterial(billOfMaterial);
        });

        billOfMaterial.getWorkOrderInstructionTemplates().forEach(workOrderInstructionTemplate -> {

            workOrderInstructionTemplate.setBillOfMaterial(billOfMaterial);
        });
        billOfMaterialService.saveOrUpdate(billOfMaterial);
        logger.debug(">> B.O.M information saved!");

    }


}

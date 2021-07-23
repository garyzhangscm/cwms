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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import com.garyzhangscm.cwms.workorder.model.ProductionLineKanbanData;
import com.garyzhangscm.cwms.workorder.model.WorkOrderProduceTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class ProductionLineKanbanService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineKanbanService.class);

    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;

    @Autowired
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;

    public List<ProductionLineKanbanData> getProductionLineKanbanData(Long productionLineId) {
        // Get all the production line assignment and we will calculate
        // all the number
        List<ProductionLineAssignment> productionLineAssignments
                = productionLineAssignmentService.findAll(productionLineId, null);
        List<ProductionLineKanbanData> productionLineKanbanDataList = new ArrayList<>();
        productionLineAssignments.forEach(
                productionLineAssignment -> {
                    ProductionLineKanbanData productionLineKanbanData = new ProductionLineKanbanData();
                    productionLineKanbanData.setProductionLineName(
                            productionLineAssignment.getProductionLine().getName()
                    );
                    productionLineKanbanData.setWorkOrderNumber(
                            productionLineAssignment.getWorkOrder().getNumber()
                    );
                    productionLineKanbanData.setProductionLineModel(
                            productionLineAssignment.getProductionLine().getModel()
                    );
                    productionLineKanbanData.setProductionLineTargetOutput(0.0);
                    productionLineKanbanData.setProductionLineTotalActualOutput(0.0);
                    productionLineKanbanData.setProductionLineTotalTargetOutput(
                            productionLineAssignment.getQuantity().doubleValue()
                    );
                    productionLineKanbanData.setProductionLineTotalActualOutput(0.0);

                    productionLineKanbanDataList.add(productionLineKanbanData);
                }
        );
        return productionLineKanbanDataList;

    }

    private Long getTotalProducedQuantity(ProductionLineAssignment productionLineAssignment) {
        // Get the total quantity from the transactions
        List<WorkOrderProduceTransaction>  workOrderProduceTransactions
                = workOrderProduceTransactionService.findAll(
                            productionLineAssignment.getWorkOrder().getWarehouseId(),
                            productionLineAssignment.getWorkOrder().getNumber(),
                            productionLineAssignment.getProductionLine().getId(),
                            false, false
                    );
        Long totalProducedQuantity =
                workOrderProduceTransactions.stream()
                        .flatMap(workOrderProduceTransaction -> workOrderProduceTransaction.getWorkOrderProducedInventories())
                    .

    }

}

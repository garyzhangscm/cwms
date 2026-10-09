package com.garyzhangscm.cwms.workorder.controller;

import com.garyzhangscm.cwms.workorder.model.AssignedMachineSummary;
import com.garyzhangscm.cwms.workorder.model.WorkOrderStatus;
import com.garyzhangscm.cwms.workorder.repository.AssignedMachineSummaryRepository;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
public class AssignedMachineSummaryController {
    private static final Logger logger = LoggerFactory.getLogger(AssignedMachineSummaryController.class);
    private final AssignedMachineSummaryRepository repository;

    public AssignedMachineSummaryController(AssignedMachineSummaryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/production-line-assignments/active-machine-summaries")
    public List<AssignedMachineSummary> getActiveMachineSummaries(
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam(value = "productionLineId", required = false) Long productionLineId) {
        long started = System.nanoTime();
        var rows = repository.findActiveMachineSummaries(warehouseId, productionLineId,
                List.of(WorkOrderStatus.PENDING, WorkOrderStatus.INPROCESS,
                        WorkOrderStatus.STAGED, WorkOrderStatus.WORK_IN_PROCESS));
        logger.info("Machine summary query: {} ms, {} rows, warehouse {}",
                (System.nanoTime() - started) / 1_000_000, rows.size(), warehouseId);
        return rows;
    }
}

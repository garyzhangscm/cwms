package com.garyzhangscm.cwms.workorder.repository;

import com.garyzhangscm.cwms.workorder.model.AssignedMachineSummary;
import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import com.garyzhangscm.cwms.workorder.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** A separate read-only projection repository avoids altering existing queries. */
public interface AssignedMachineSummaryRepository extends Repository<ProductionLineAssignment, Long> {
    @Query("select distinct new com.garyzhangscm.cwms.workorder.model.AssignedMachineSummary(" +
            "p.id, p.name, w.id, w.number, w.itemId, w.warehouseId, w.status, " +
            "w.expectedQuantity, w.producedQuantity) " +
            "from ProductionLineAssignment a join a.productionLine p join a.workOrder w " +
            "where p.warehouseId = :warehouseId " +
            "and (:productionLineId is null or p.id = :productionLineId) " +
            "and (a.deassigned is null or a.deassigned = false) " +
            "and w.status in :statuses order by p.name, w.id")
    List<AssignedMachineSummary> findActiveMachineSummaries(
            @Param("warehouseId") Long warehouseId,
            @Param("productionLineId") Long productionLineId,
            @Param("statuses") List<WorkOrderStatus> statuses);

}

package com.garyzhangscm.cwms.workorder.deletion;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface WorkOrderDeletionRepository extends JpaRepository<WorkOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wo from WorkOrder wo where wo.id = :id")
    Optional<WorkOrder> findForDeletion(@Param("id") Long id);
}

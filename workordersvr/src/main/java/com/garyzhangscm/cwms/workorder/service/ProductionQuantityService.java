package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.sql.Timestamp;

/** Runs inside the reporting transaction, including its overproduction checks. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ProductionQuantityService {
    static final String INCREMENT_SQL = "update work_order set " +
            "produced_quantity = coalesce(produced_quantity, 0) + ?1, " +
            "last_modified_time = ?2, last_modified_by = ?3 where work_order_id = ?4";

    @PersistenceContext
    private EntityManager entityManager;

    public WorkOrder lockForReporting(Long id) {
        WorkOrder order = entityManager.find(WorkOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) throw ResourceNotFoundException.raiseException("Work order not found: " + id);
        // It may already be in the persistence context from setup. Reload the
        // committed counter after acquiring the lock, before validating limits.
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        return order;
    }

    public WorkOrder increment(WorkOrder order, Long quantity, String username) {
        if (quantity == null || quantity < 0) throw new IllegalArgumentException("Invalid production quantity");
        if (!entityManager.contains(order)) throw new IllegalStateException("Production requires a managed work order");
        if (entityManager.getLockMode(order) != LockModeType.PESSIMISTIC_WRITE) {
            throw new IllegalStateException("Lock work order before validating and recording production");
        }
        // Flush pending changes before bulk SQL, then refresh the managed entity
        // so later ORM writes cannot restore the old counter.
        entityManager.flush();
        int updated = entityManager.createNativeQuery(INCREMENT_SQL)
                .setParameter(1, quantity)
                .setParameter(2, Timestamp.from(Instant.now()))
                .setParameter(3, username)
                .setParameter(4, order.getId()).executeUpdate();
        if (updated != 1) throw new IllegalStateException("Work order quantity was not recorded");
        entityManager.refresh(order);
        return order;
    }
}

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.exception.QCException;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Prepares only an untouched inspection, before the operator enters answers. */
@Service
public class QCReviewPreparationService {
    private final QCInspectionRequestService requests;
    @PersistenceContext private EntityManager entityManager;
    public QCReviewPreparationService(QCInspectionRequestService requests) { this.requests=requests; }

    @Transactional
    public QCInspectionRequest prepare(Long warehouseId,Long id) {
        QCInspectionRequest stored=entityManager.find(QCInspectionRequest.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(stored==null || !Objects.equals(stored.getWarehouseId(),warehouseId))
            throw QCException.raiseException("Inspection is not available in this warehouse.");
        boolean pristine=stored.getQcTime()==null && (stored.getQcUsername()==null || stored.getQcUsername().isBlank()) &&
            !stored.getQcInspectionRequestItems().isEmpty() && stored.getQcInspectionRequestItems().stream().allMatch(g ->
              g.getQcInspectionResult()==QCInspectionResult.PENDING && g.getQcInspectionRequestItemOptions().stream().allMatch(o ->
                o.getQcInspectionResult()==QCInspectionResult.PENDING && o.getBooleanValue()==null && o.getDoubleValue()==null &&
                (o.getStringValue()==null || o.getStringValue().isBlank()) && (o.getExceptionNotes()==null || o.getExceptionNotes().isBlank())));
        boolean changed=false;
        if(stored.getQcInspectionResult()!=QCInspectionResult.PENDING) {
            Inventory inv=stored.getInventory();
            boolean legacy=stored.getQcInspectionResult()==QCInspectionResult.PASS && pristine &&
                (stored.getType()==QCInspectionRequestType.BY_INVENTORY || stored.getType()==QCInspectionRequestType.INBOUND_BY_INVENTORY) &&
                inv!=null && Boolean.TRUE.equals(inv.getInboundQCRequired()) && inv.getLastQCTime()==null &&
                Objects.equals(inv.getWarehouseId(),warehouseId) &&
                stored.getQcInspectionRequestItems().stream().allMatch(g -> !g.getQcInspectionRequestItemOptions().isEmpty());
            if(!legacy) throw QCException.raiseException("This inspection has already been recorded. Reload the task.");
            stored.setQcInspectionResult(QCInspectionResult.PENDING); changed=true;
        }
        if(pristine) for(QCInspectionRequestItem group:stored.getQcInspectionRequestItems()) {
            if(group.getQcRule()==null || group.getQcRule().getQcRuleItems()==null) continue;
            boolean hasEnabled=group.getQcInspectionRequestItemOptions().stream().anyMatch(o -> o.getQcRuleItem()!=null && Boolean.TRUE.equals(o.getQcRuleItem().getEnabled()));
            if(hasEnabled) continue;
            List<QCRuleItem> active=new ArrayList<>();
            for(QCRuleItem rule:group.getQcRule().getQcRuleItems()) if(Boolean.TRUE.equals(rule.getEnabled())) active.add(rule);
            if(active.isEmpty()) continue;
            Set<Long> existing=new HashSet<>();
            for(QCInspectionRequestItemOption option:group.getQcInspectionRequestItemOptions()) {
                if(option.getQcRuleItem()!=null) existing.add(option.getQcRuleItem().getId());
                option.setQcInspectionResult(QCInspectionResult.NOT_APPLY);
            }
            for(QCRuleItem rule:active) if(!existing.contains(rule.getId())) {
                QCInspectionRequestItemOption option=new QCInspectionRequestItemOption();
                option.setQcRuleItem(rule);option.setQcInspectionResult(QCInspectionResult.PENDING);
                option.setQcInspectionRequestItem(group);group.addQcInspectionRequestItemOption(option);
            }
            changed=true;
        }
        if(changed) { requests.save(stored); entityManager.flush(); }
        return requests.findById(id);
    }
}

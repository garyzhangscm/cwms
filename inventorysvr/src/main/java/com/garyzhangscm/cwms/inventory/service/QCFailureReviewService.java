package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.exception.QCException;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class QCFailureReviewService {
    private final QCInspectionRequestService requests;
    @PersistenceContext private EntityManager entityManager;
    public QCFailureReviewService(QCInspectionRequestService requests) { this.requests = requests; }

    @Transactional
    public List<QCInspectionRequest> submit(Long warehouseId, List<QCInspectionRequest> inputs, boolean acknowledged) {
        return submit(warehouseId, inputs, acknowledged, "");
    }
    @Transactional
    public List<QCInspectionRequest> submit(Long warehouseId, List<QCInspectionRequest> inputs, boolean acknowledged, String rfCode) {
        if (inputs == null || inputs.isEmpty() || inputs.size() > 100)
            throw QCException.raiseException("Submit between 1 and 100 inspection requests.");
        Set<Long> ids = new HashSet<>();
        for (QCInspectionRequest input : inputs)
            if (input == null || input.getId() == null || !ids.add(input.getId()))
                throw QCException.raiseException("Duplicate or missing inspection identifier.");
        List<QCInspectionRequest> ordered = new ArrayList<>(inputs);
        ordered.sort(Comparator.comparing(QCInspectionRequest::getId));
        List<QCInspectionRequest> reviewed = new ArrayList<>();
        for (QCInspectionRequest input : ordered) {
            QCInspectionRequest stored = entityManager.find(QCInspectionRequest.class, input.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (stored == null || !Objects.equals(stored.getWarehouseId(), warehouseId))
                throw QCException.raiseException("Inspection is not available in this warehouse.");
            QCFailureReviewPolicy.apply(stored, input, acknowledged);
            // Inventory membership comes from trusted server records, not detached client objects.
            List<Inventory> existing = new ArrayList<>(stored.getInventories() == null ? Collections.emptyList() : stored.getInventories());
            Map<Long, Inventory> trusted = new LinkedHashMap<>();
            existing.forEach(i -> trusted.put(i.getId(), i));
            if (input.getInventories() != null) {
                Set<Long> selected = new HashSet<>();
                for (Inventory incoming : input.getInventories()) {
                    if (incoming == null || incoming.getId() == null || !selected.add(incoming.getId()))
                        throw QCException.raiseException("Invalid or duplicate inspection inventory.");
                    if (!trusted.containsKey(incoming.getId())) {
                        if (incoming.getLpn() == null || incoming.getLpn().isBlank())
                            throw QCException.raiseException("An LPN is required for additional inventory.");
                        Inventory validated = requests.validateLPNForInspectionByQCRequest(stored.getId(), warehouseId, incoming.getLpn(), false)
                            .stream().filter(i -> Objects.equals(i.getId(), incoming.getId())).findFirst()
                            .orElseThrow(() -> QCException.raiseException("Additional inventory is not eligible for this inspection."));
                        if (!Objects.equals(validated.getWarehouseId(), warehouseId))
                            throw QCException.raiseException("Additional inventory belongs to a different warehouse.");
                        trusted.put(validated.getId(), validated);
                    }
                }
                for (Inventory old : existing) if (!selected.contains(old.getId()))
                    throw QCException.raiseException("Existing inspection inventory cannot be removed during submission.");
            }
            stored.setInventories(new ArrayList<>(trusted.values()));
            if (stored.getWorkOrderQCSampleId()!=null && input.getQcQuantity()!=null) {
                if (input.getQcQuantity()<0) throw QCException.raiseException("QC quantity cannot be negative.");
                stored.setQcQuantity(input.getQcQuantity());
            }
            reviewed.add(stored);
        }
        List<QCInspectionRequest> completed = new ArrayList<>(), drafts = new ArrayList<>();
        for (QCInspectionRequest record : reviewed) {
            // Saving an incomplete inspection must not increment work-order QC quantity.
            if (record.getQcInspectionResult() == QCInspectionResult.PENDING) drafts.add(requests.save(record));
            else completed.add(record);
        }
        List<QCInspectionRequest> saved = completed.isEmpty() ? new ArrayList<>()
            : requests.savePendingQCInspectionRequest(warehouseId, completed, rfCode==null ? "" : rfCode);
        saved.addAll(drafts);
        return saved;
    }
}

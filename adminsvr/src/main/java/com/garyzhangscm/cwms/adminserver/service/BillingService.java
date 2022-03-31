package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.model.BillableCategory;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public interface BillingService {
    public BillingRequest generateBillingRequest(LocalDateTime startTime, LocalDateTime endTime, Long companyId, Long warehouseId, Long clientId);

    public BillableCategory getBillableCategory();

}

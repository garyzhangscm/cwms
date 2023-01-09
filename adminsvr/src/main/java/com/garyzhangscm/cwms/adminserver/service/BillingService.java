package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.model.BillableCategory;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
public interface BillingService {
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize);

    public BillableCategory getBillableCategory();

}

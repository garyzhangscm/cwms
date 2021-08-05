package com.garyzhangscm.cwms.outbound.service;

import org.springframework.stereotype.Service;

@Service
public class OutboundConfiguration {

    public boolean isShortAutoAllocationEnabled() {
        return false;
    }
}

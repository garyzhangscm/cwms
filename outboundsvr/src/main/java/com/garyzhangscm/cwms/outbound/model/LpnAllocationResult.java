package com.garyzhangscm.cwms.outbound.model;

import java.util.HashMap;
import java.util.Map;

public class LpnAllocationResult {

    // key: LPN
    // value: total quantity of the LPN
    private Map<String, Long> lpns = new HashMap<>();



    private Long requiredQuantity;

    public Long getQuantityDiff() {
        return getTotalQuantity() - requiredQuantity;
    }

    public Long getTotalQuantity() {
        return lpns.entrySet().stream().mapToLong(entry -> entry.getValue()).sum();
    }
    public Map<String, Long> getLpns() {
        return lpns;
    }

    public void setLpns(Map<String, Long> lpns) {
        this.lpns = lpns;
    }




    public Long getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Long requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }
}

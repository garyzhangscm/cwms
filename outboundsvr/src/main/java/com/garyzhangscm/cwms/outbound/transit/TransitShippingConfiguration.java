package com.garyzhangscm.cwms.outbound.transit;

import java.util.ArrayList;
import java.util.List;

/** Dedicated configuration, independent of existing outbound configuration and database schema. */
public class TransitShippingConfiguration {
    public long revision;
    public String modifiedBy;
    public String modifiedAt;
    public List<Rule> rules = new ArrayList<>();
    public static class Rule {
        public Long customerId;
        public Long destinationLocationId;
        public boolean enabled;
        public Rule() {}
        public Rule(Long customer, Long destination, boolean enabled) {
            this.customerId = customer; this.destinationLocationId = destination; this.enabled = enabled;
        }
    }
}

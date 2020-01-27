package com.garyzhangscm.cwms.outbound.model;


public class ListPickingConfigurationCSVWrapper {


    private Integer sequence;

    private String warehouse;

    private String client;

    private String pickType;

    private String groupRule;

    private boolean enabled;


    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }
    public String getGroupRule() {
        return groupRule;
    }

    public void setGroupRule(String groupRule) {
        this.groupRule = groupRule;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPickType() {
        return pickType;
    }

    public void setPickType(String pickType) {
        this.pickType = pickType;
    }
}

package com.garyzhangscm.cwms.outbound.model;

public class AllocationRoundUpStrategy {

    private AllocationRoundUpStrategyType type;

    private Double value;

    public AllocationRoundUpStrategy() {}
    public AllocationRoundUpStrategy(AllocationRoundUpStrategyType type,
                                     Double value) {
        this.type = type;
        this.value = value;
    }
    public static AllocationRoundUpStrategy unlimit() {
        // for unlimit round up, there's no need to setup the value;
        return new AllocationRoundUpStrategy(AllocationRoundUpStrategyType.NO_LIMIT, 0.0);
    }
    public static AllocationRoundUpStrategy none() {
        // for unlimit round up, there's no need to setup the value;
        return new AllocationRoundUpStrategy(AllocationRoundUpStrategyType.NONE, 0.0);
    }
    public AllocationRoundUpStrategyType getType() {
        return type;
    }

    public void setType(AllocationRoundUpStrategyType type) {
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public boolean isRoundUpAllowed() {
        boolean isRoundUpAllowed = false;
        switch (type) {
            case NO_LIMIT:
                isRoundUpAllowed = true;
                break;
            case BY_PERCENTAGE:
            case BY_QUANTITY:
                // if we allow round up by percentage or quantity
                // we will round up only when the value is bigger than 0
                isRoundUpAllowed = (value > 0.0);
                break;
            default:
                isRoundUpAllowed = false;
                break;
        }
        return isRoundUpAllowed;
    }
}

package com.garyzhangscm.cwms.outbound.model;

public enum OrderStatus {
    OPEN(0),
    ALLOCATING(1),
    INPROCESS(2),
    COMPLETE(3),
    CANCELLED(3);

    private int orderStage;

    private OrderStatus(int orderStage) {
        this.orderStage = orderStage;
    }

    public boolean isBefore(OrderStatus anotherOrderStatus) {
        return  orderStage < anotherOrderStatus.orderStage;
    }

    public boolean noLaterThan(OrderStatus anotherOrderStatus) {
        return  orderStage <= anotherOrderStatus.orderStage;
    }
}

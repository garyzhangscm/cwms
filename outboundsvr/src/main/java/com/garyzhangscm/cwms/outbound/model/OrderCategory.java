package com.garyzhangscm.cwms.outbound.model;

public enum OrderCategory {
    SALES_ORDER(false, false, true),
    WAREHOUSE_TRANSFER_ORDER(true, false, true),
    OUTSOURCING_ORDER(true, true, false);   // orders taht will be fulfilled by other party

    private boolean autoGenerateReceipt;
    private boolean outsourcingOrder;
    private boolean waveable;
    private OrderCategory(boolean autoGenerateReceipt,
                          boolean outsourcingOrder,
                          boolean waveable) {
        this.autoGenerateReceipt = autoGenerateReceipt;
        this.outsourcingOrder = outsourcingOrder;
        this.waveable = waveable;
    }

    public boolean isAutoGenerateReceipt() {
        return autoGenerateReceipt;
    }
    public boolean isOutsourcingOrder() {
        return outsourcingOrder;
    }
    public boolean isWaveable() {
        return waveable;
    }
}

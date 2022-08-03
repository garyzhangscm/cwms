package com.garyzhangscm.cwms.quickbook.model;

public enum OrderCategory {
    SALES_ORDER(false, false),
    WAREHOUSE_TRANSFER_ORDER(true, false),
    OUTSOURCING_ORDER(true, true);   // orders taht will be fulfilled by other party

    private boolean autoGenerateReceipt;
    private boolean outsourcingOrder;
    private OrderCategory(boolean autoGenerateReceipt, boolean outsourcingOrder) {
        this.autoGenerateReceipt = autoGenerateReceipt;
        this.outsourcingOrder = outsourcingOrder;
    }

    public boolean isAutoGenerateReceipt() {
        return autoGenerateReceipt;
    }
    public boolean isOutsourcingOrder() {
        return outsourcingOrder;
    }
}

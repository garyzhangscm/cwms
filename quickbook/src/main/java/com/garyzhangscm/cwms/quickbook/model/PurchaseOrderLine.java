package com.garyzhangscm.cwms.quickbook.model;


import java.io.Serializable;

public class PurchaseOrderLine implements Serializable {

    private Long id;

    // invoice number
    private Long lineNum;

    private String description;

    private Double amount;

    private ItemBasedExpenseLineDetail itemBasedExpenseLineDetail;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLineNum() {
        return lineNum;
    }

    public void setLineNum(Long lineNum) {
        this.lineNum = lineNum;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public ItemBasedExpenseLineDetail getItemBasedExpenseLineDetail() {
        return itemBasedExpenseLineDetail;
    }

    public void setItemBasedExpenseLineDetail(ItemBasedExpenseLineDetail itemBasedExpenseLineDetail) {
        this.itemBasedExpenseLineDetail = itemBasedExpenseLineDetail;
    }
}

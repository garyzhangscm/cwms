package com.garyzhangscm.cwms.workorder.model.lightMES;

public class PulseCountHistoryByItem {


    String itemName;
    int count;

    public PulseCountHistoryByItem(){}
    public PulseCountHistoryByItem(String itemName){
        this(itemName, 0);
    }

    public PulseCountHistoryByItem(String itemName, int count) {
        this.itemName = itemName;
        this.count = count;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

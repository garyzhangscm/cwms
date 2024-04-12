package com.garyzhangscm.cwms.inventory.model;

public enum InventoryMixRestrictionAttribute {

    clientId(Long.class),
    receiptLineId(Long.class),
    item(Item.class),
    itemPackageType(ItemPackageType.class),
    inventoryStatus(InventoryStatus.class),
    color(String.class),
    productSize(String.class),
    style(String.class),
    attribute1(String.class),
    attribute2(String.class),
    attribute3(String.class),
    attribute4(String.class),
    attribute5(String.class);


    private Class clazz;

    private InventoryMixRestrictionAttribute(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return  clazz;
    }



}

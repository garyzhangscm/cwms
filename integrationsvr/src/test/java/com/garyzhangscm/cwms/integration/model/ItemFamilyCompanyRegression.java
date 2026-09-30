package com.garyzhangscm.cwms.integration.model;

import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;

/** Standalone regression, runnable against the deployed JAR without Spring or a database. */
public class ItemFamilyCompanyRegression {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ItemFamily input = new ItemFamily();
        input.setName("Finish Good");
        input.setDescription("Finish Good");
        input.setCompanyCode("20901");
        input.setWarehouseId(1L);
        input.setWarehouseName("TEST_WAREHOUSE");

        // Exercise the actual parent Item -> integration Item -> nested family path.
        Item item = new Item();
        item.setName("TEST-COMPANY-REGRESSION");
        item.setCompanyCode("20901");
        item.setWarehouseId(1L);
        item.setItemFamily(input);
        DBBasedItem parent = new DBBasedItem(item);
        DBBasedItemFamily family = parent.getItemFamily();
        require("20901".equals(family.getCompanyCode()), "nested family lost company code");
        require(family.getCompanyId() == null, "company code must not become database ID");
        require(family.getStatus() == IntegrationStatus.ATTACHED, "nested status changed");
        require("Finish Good".equals(family.getDescription()), "family description lost");

        WarehouseLayoutServiceRestemplateClient layout = new WarehouseLayoutServiceRestemplateClient() {
            @Override
            public Company getCompanyByCode(String code) {
                require("20901".equals(code), "wrong company lookup");
                Company company = new Company();
                company.setId(1L);
                return company;
            }
        };
        ItemFamily converted = family.convertToItemFamily(layout);
        require(Long.valueOf(1L).equals(converted.getCompanyId()), "company code lookup failed");
        require(Long.valueOf(1L).equals(converted.getWarehouseId()), "warehouse changed");

        input.setCompanyId(7L);
        input.setCompanyCode(null);
        DBBasedItemFamily withId = new DBBasedItemFamily(input);
        require(Long.valueOf(7L).equals(withId.getCompanyId()), "explicit company ID lost");
        ItemFamily byId = withId.convertToItemFamily(new WarehouseLayoutServiceRestemplateClient() {
            @Override
            public Company getCompanyByCode(String code) {
                throw new AssertionError("must not look up an already supplied ID");
            }
        });
        require(Long.valueOf(7L).equals(byId.getCompanyId()), "explicit ID changed");
        System.out.println("PASS: nested company code, explicit company ID, conversion and attached status");
    }
}

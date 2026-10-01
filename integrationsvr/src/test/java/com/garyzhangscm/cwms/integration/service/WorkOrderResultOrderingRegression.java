package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.*;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedWorkOrderRepository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;

/** A fast success reply must not be overwritten by a later SENT save. */
public class WorkOrderResultOrderingRegression {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void run(boolean transaction) throws Exception {
        DBBasedWorkOrderIntegration service = new DBBasedWorkOrderIntegration();
        IntegrationStatus[] persisted = {IntegrationStatus.PENDING};
        int[] sends = {0};
        DBBasedWorkOrder record = new DBBasedWorkOrder() {
            @Override
            public WorkOrder convertToWorkOrder() {
                WorkOrder order = new WorkOrder();
                order.setNumber("WO-TEST-001");
                order.setWorkOrderLines(new ArrayList<>());
                order.setWorkOrderByProducts(new ArrayList<>());
                order.setWorkOrderInstructions(new ArrayList<>());
                return order;
            }
        };
        record.setId(123L);
        record.setCompanyId(1L);
        record.setItemName("FG-001");
        record.setWorkOrderLines(new HashSet<>());
        service.warehouseLayoutServiceRestemplateClient = new WarehouseLayoutServiceRestemplateClient() {
            @Override
            public Long getWarehouseId(Long companyId, String companyCode, Long warehouseId, String warehouseName) {
                return 1L;
            }
            @Override
            public Warehouse getWarehouseById(Long id) {
                Company company = new Company(); company.setId(1L);
                Warehouse warehouse = new Warehouse(); warehouse.setId(1L); warehouse.setCompany(company);
                return warehouse;
            }
        };
        service.inventoryServiceRestemplateClient = new InventoryServiceRestemplateClient() {
            @Override
            public Item getItemByName(Long companyId, Long warehouseId, Long clientId, String name) {
                Item item = new Item(); item.setId(1L); return item;
            }
        };
        service.dbBasedWorkOrderRepository = (DBBasedWorkOrderRepository) Proxy.newProxyInstance(
                DBBasedWorkOrderRepository.class.getClassLoader(),
                new Class[]{DBBasedWorkOrderRepository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        persisted[0] = ((DBBasedWorkOrder) args[0]).getStatus();
                        return args[0];
                    }
                    throw new AssertionError("Unexpected repository call: " + method.getName());
                });
        service.kafkaSender = new KafkaSender() {
            @Override
            public <K,V> void send(IntegrationType type, K key, V value) {
                require(persisted[0] == IntegrationStatus.SENT, "published before SENT was saved");
                sends[0]++;
                persisted[0] = IntegrationStatus.COMPLETED;
            }
        };
        if (transaction) {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
        }
        try {
            Method process = DBBasedWorkOrderIntegration.class.getDeclaredMethod("process", DBBasedWorkOrder.class);
            process.setAccessible(true); process.invoke(service, record);
            if (transaction) {
                require(sends[0] == 0, "published before transaction commit");
                for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                    sync.afterCommit();
                }
            }
            require(sends[0] == 1, "expected exactly one publication");
            require(persisted[0] == IntegrationStatus.COMPLETED, "late SENT overwrote completion");
        } finally {
            if (transaction) {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        run(false); run(true);
        System.out.println("PASS: Work Order fast reply preserved and transaction commit respected");
    }
}

package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.*;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemRepository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/** Reproduces a success reply arriving inside publish, before the caller returns. */
public class ItemResultOrderingRegression {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void run(boolean transaction) throws Exception {
        DBBasedItemIntegration service = new DBBasedItemIntegration();
        IntegrationStatus[] persisted = {IntegrationStatus.PENDING};
        int[] sent = {0};
        DBBasedItem record = new DBBasedItem() {
            @Override
            public Item convertToItem(InventoryServiceRestemplateClient inventory,
                    CommonServiceRestemplateClient common, WarehouseLayoutServiceRestemplateClient layout) {
                Item item = new Item(); item.setCompanyId(1L); item.setWarehouseId(1L);
                return item;
            }
        };
        record.setId(123L); record.setStatus(IntegrationStatus.PENDING);
        record.setItemPackageTypes(new ArrayList<>());
        service.dbBasedItemRepository = (DBBasedItemRepository) Proxy.newProxyInstance(
                DBBasedItemRepository.class.getClassLoader(), new Class[]{DBBasedItemRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        persisted[0] = ((DBBasedItem) args[0]).getStatus();
                        return args[0];
                    }
                    throw new AssertionError("Unexpected repository call: " + method.getName());
                });
        service.kafkaSender = new KafkaSender() {
            @Override
            public <K,V> void send(IntegrationType type, K key, V value) {
                require(persisted[0] == IntegrationStatus.SENT, "published before SENT was saved");
                sent[0]++;
                // Simulate the separate result consumer's committed update.
                persisted[0] = IntegrationStatus.COMPLETED;
            }
        };
        if (transaction) {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
        }
        try {
            Method process = DBBasedItemIntegration.class.getDeclaredMethod("process", DBBasedItem.class);
            process.setAccessible(true); process.invoke(service, record);
            if (transaction) {
                require(sent[0] == 0, "published before enclosing transaction committed");
                for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                    sync.afterCommit();
                }
            }
            require(sent[0] == 1, "expected one publication");
            require(persisted[0] == IntegrationStatus.COMPLETED, "late SENT save overwrote completion");
        } finally {
            if (transaction) {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        run(false); run(true);
        System.out.println("PASS: fast completion preserved; publication waits for transaction commit");
    }
}

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import java.util.*;

@Service
public class InventoryRemovalQueue implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryRemovalQueue.class);
    private static volatile InventoryRemovalQueue instance;
    private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();
    @Autowired private InventoryService inventoryService;
    @Autowired private UserService userService;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private WarehouseLayoutServiceRestemplateClient layout;
    @Value("${inventory.removal.queue.directory:/var/lib/cwms-inventory-removal}") private String directory;
    @Value("${inventory.removal.queue.workers:2}") private int concurrency;
    @Value("${inventory.removal.queue.capacity:2000}") private int capacity;
    private volatile DurableRemovalQueue queue;
    public static String currentActor() { return ACTOR.get(); }
    public static InventoryRemovalQueue getInstance() {
        InventoryRemovalQueue service = instance;
        if (service == null || service.queue == null) throw new IllegalStateException("Removal queue not ready");
        return service;
    }
    public synchronized String submit(Long companyId, String inventoryIds, Boolean asynchronous) {
        if (companyId == null || companyId <= 0) throw new IllegalArgumentException("Company is required");
        if (inventoryIds == null || inventoryIds.trim().isEmpty()) return "no inventory id is passed in";
        if (inventoryIds.length() > 22000) throw new IllegalArgumentException("Too many inventory IDs");
        String[] values = inventoryIds.split(",", -1);
        if (values.length > 1000) throw new IllegalArgumentException("At most 1000 inventory records per request");
        Set<Long> ids = new LinkedHashSet<>();
        for (String value : values) {
            long id = Long.parseLong(value.trim());
            if (id <= 0) throw new IllegalArgumentException("Invalid inventory ID");
            ids.add(id);
        }
        if (queue == null) throw new IllegalStateException("Removal queue not ready");
        String actor = userService.getCurrentUserName();
        if (actor == null || actor.trim().isEmpty()) throw new IllegalArgumentException("User is required");
        List<DurableRemovalQueue.Task> tasks = new ArrayList<>();
        Map<Long, Long> companies = new HashMap<>();
        for (Long id : ids) {
            Inventory row = inventoryService.findById(id, false);
            long owner = companies.computeIfAbsent(row.getWarehouseId(), w -> layout.getWarehouseById(w).getCompanyId());
            if (owner != companyId.longValue()) throw new IllegalArgumentException("Inventory outside requested company");
            if (queue.wasSubmitted(id)) continue;
            if (Boolean.TRUE.equals(row.getVirtual())) throw new IllegalArgumentException("Inventory already virtual: " + id);
            if (row.getQuantity() == null || row.getQuantity() <= 0) throw new IllegalArgumentException("Inventory quantity must be positive");
            DurableRemovalQueue.Task task = new DurableRemovalQueue.Task();
            task.inventoryId = id; task.companyId = companyId; task.warehouseId = row.getWarehouseId();
            task.locationId = row.getLocationId(); task.quantity = row.getQuantity(); task.lpn = row.getLpn(); task.actor = actor;
            tasks.add(task);
        }
        try {
            int count = queue.submit(tasks);
            LOG.info("Inventory removal batch accepted: {} records, actor {}, pending {}", count, actor, queue.pendingCount());
            // Preserve the existing browser response. Both modes must do actual work.
            return "remove request has been sent";
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot persist inventory removal request", e); }
    }
    private String execute(DurableRemovalQueue.Task task) throws Exception {
        org.springframework.transaction.support.TransactionTemplate transaction =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.setTimeout(120);
        return transaction.execute(status -> executeInTransaction(task));
    }
    private String executeInTransaction(DurableRemovalQueue.Task task) {
        ACTOR.set(task.actor);
        try {
            Inventory row = inventoryService.findById(task.inventoryId);
            if (row.getWarehouseId() != task.warehouseId || row.getLocationId() != task.locationId ||
                    row.getQuantity() != task.quantity || !Objects.equals(row.getLpn(), task.lpn) ||
                    Boolean.TRUE.equals(row.getVirtual()) || !Boolean.FALSE.equals(row.getLockedForAdjust()) ||
                    (row.getLocks() != null && !row.getLocks().isEmpty()) || row.getAllocatedByPickId() != null) {
                LOG.warn("Inventory removal skipped because record changed or is unavailable: {}", task.inventoryId);
                return "SKIPPED_CHANGED_OR_UNAVAILABLE";
            }
            Inventory result = inventoryService.removeInventory(row, "", "");
            String status = Boolean.TRUE.equals(result.getVirtual()) ? "COMPLETED" : "AWAITING_APPROVAL";
            LOG.info("Inventory removal {}: {}", task.inventoryId, status);
            return status;
        } catch (Exception e) {
            LOG.error("Inventory removal {} uncertain; no automatic retry", task.inventoryId, e);
            throw e;
        } finally { ACTOR.remove(); }
    }
    @Override public void start() {
        try {
            queue = new DurableRemovalQueue(Paths.get(directory), concurrency, capacity, this::execute);
            instance = this;
            LOG.info("Durable inventory removal queue started: {} workers, {} capacity", concurrency, capacity);
        } catch (Exception e) { throw new IllegalStateException("Cannot start durable inventory removal queue", e); }
    }
    @Override public void stop() {
        DurableRemovalQueue current = queue;
        queue = null; instance = null;
        if (current != null) try { current.close(); } catch (Exception e) { LOG.error("Removal queue shutdown incomplete", e); }
    }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }
    @Override public boolean isRunning() { return queue != null; }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE - 100; }
}

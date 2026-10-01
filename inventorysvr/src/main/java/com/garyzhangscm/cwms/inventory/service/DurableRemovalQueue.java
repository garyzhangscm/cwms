package com.garyzhangscm.cwms.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Single-process durable queue. RUNNING work is never replayed after a crash. */
public final class DurableRemovalQueue implements AutoCloseable {
    public static class Task {
        public long inventoryId, companyId, warehouseId, locationId, quantity;
        public String lpn, actor, status = "QUEUED", error;
    }
    public static class Batch {
        public String id;
        public List<Task> tasks = new ArrayList<>();
    }
    public interface Handler { String handle(Task task) throws Exception; }
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path directory;
    private final int capacity;
    private final Handler handler;
    private final Set<Long> submitted = new HashSet<>();
    private final Map<String, Batch> active = new LinkedHashMap<>();
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final ExecutorService workers;
    private boolean closed;
    private int pending;
    private IOException storageFailure;

    public DurableRemovalQueue(Path directory, int concurrency, int capacity, Handler handler) throws IOException {
        if (concurrency < 1 || concurrency > 4 || capacity < 1 || capacity > 10000)
            throw new IllegalArgumentException("Invalid removal queue limits");
        this.directory = directory;
        this.capacity = capacity;
        this.handler = handler;
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(directory.resolve(".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try { lock = lockChannel.tryLock(); }
        catch (RuntimeException | IOException e) { lockChannel.close(); throw e; }
        if (lock == null) { lockChannel.close(); throw new IOException("Removal queue already owned by another process"); }
        try {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory, "*.json")) {
                for (Path path : paths) {
                    Batch batch = mapper.readValue(path.toFile(), Batch.class);
                    boolean changed = false, queued = false;
                    for (Task task : batch.tasks) {
                        submitted.add(task.inventoryId);
                        if ("RUNNING".equals(task.status)) {
                            task.status = "UNCERTAIN";
                            task.error = "Service stopped during processing; not automatically retried";
                            changed = true;
                        }
                        if ("QUEUED".equals(task.status)) { pending++; queued = true; }
                    }
                    if (changed) save(batch);
                    if (queued) active.put(batch.id, batch);
                }
            }
            if (pending > capacity) throw new IOException("Saved queue exceeds configured capacity");
        } catch (IOException | RuntimeException e) { lock.release(); lockChannel.close(); throw e; }
        workers = Executors.newFixedThreadPool(concurrency, r -> {
            Thread thread = new Thread(r, "inventory-removal-worker");
            thread.setDaemon(true);
            return thread;
        });
        // Only these fixed long-lived worker loops enter the executor; per-item work stays on disk.
        for (int i = 0; i < concurrency; i++) workers.execute(this::work);
    }
    private void save(Batch batch) throws IOException {
        Path temporary = Files.createTempFile(directory, ".batch-", ".tmp");
        try {
            byte[] bytes = mapper.writeValueAsBytes(batch);
            try (FileOutputStream out = new FileOutputStream(temporary.toFile())) {
                out.write(bytes); out.getFD().sync();
            }
            Files.move(temporary, directory.resolve(batch.id + ".json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel dir = FileChannel.open(directory, StandardOpenOption.READ)) { dir.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public synchronized boolean wasSubmitted(long id) { return submitted.contains(id); }
    public synchronized int pendingCount() { return pending; }
    public synchronized int submit(List<Task> tasks) throws IOException {
        if (closed || storageFailure != null) throw new IOException("Removal queue unavailable");
        if (tasks.size() > 1000) throw new IllegalArgumentException("At most 1000 inventory records per request");
        Batch batch = new Batch(); batch.id = UUID.randomUUID().toString();
        Set<Long> ids = new HashSet<>();
        for (Task task : tasks) {
            if (task.inventoryId <= 0 || task.companyId <= 0) throw new IllegalArgumentException("Invalid inventory or company ID");
            if (!submitted.contains(task.inventoryId) && ids.add(task.inventoryId)) batch.tasks.add(task);
        }
        if (pending + batch.tasks.size() > capacity) throw new IllegalStateException("Removal queue full; request not accepted");
        if (batch.tasks.isEmpty()) return 0;
        try { save(batch); } catch (IOException e) { storageFailure = e; notifyAll(); throw e; }
        // Atomic admission: no worker can run a partially persisted batch.
        active.put(batch.id, batch);
        submitted.addAll(ids);
        pending += batch.tasks.size();
        notifyAll();
        return batch.tasks.size();
    }
    private void work() {
        while (true) {
            Batch batch = null; Task task = null;
            synchronized (this) {
                while (!closed && storageFailure == null && task == null) {
                    for (Batch candidate : active.values()) {
                        for (Task t : candidate.tasks) if ("QUEUED".equals(t.status)) { batch = candidate; task = t; break; }
                        if (task != null) break;
                    }
                    if (task == null) try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                if (closed || storageFailure != null) return;
                task.status = "RUNNING";
                try { save(batch); } catch (IOException e) { storageFailure = e; notifyAll(); return; }
            }
            String result, error = null;
            try { result = handler.handle(task); }
            catch (Exception e) { result = "UNCERTAIN"; error = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()); }
            synchronized (this) {
                task.status = result;
                task.error = error == null ? null : error.substring(0, Math.min(error.length(), 500));
                try { save(batch); } catch (IOException e) { storageFailure = e; notifyAll(); return; }
                pending--;
                if (batch.tasks.stream().noneMatch(t -> "QUEUED".equals(t.status) || "RUNNING".equals(t.status))) active.remove(batch.id);
                notifyAll();
            }
        }
    }
    @Override public void close() throws IOException {
        synchronized (this) { closed = true; notifyAll(); }
        workers.shutdown();
        try {
            if (!workers.awaitTermination(120, TimeUnit.SECONDS)) {
                // Keep the file lock until process exit if a business call remains stuck.
                throw new IOException("Removal workers still active; queue lock retained");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
        lock.release(); lockChannel.close();
    }
}

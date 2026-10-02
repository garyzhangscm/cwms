import com.garyzhangscm.cwms.inventory.service.DurableRemovalQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class RemovalQueueRegression {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static DurableRemovalQueue.Task task(long id) {
        DurableRemovalQueue.Task t = new DurableRemovalQueue.Task(); t.inventoryId=id; t.companyId=1; return t;
    }
    static void awaitDone(DurableRemovalQueue queue) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while(queue.pendingCount()!=0 && System.nanoTime()<end) Thread.sleep(10);
        check(queue.pendingCount()==0, "queue did not drain");
    }
    public static void main(String[] args) throws Exception {
        // Verify modified deployed classes without starting Spring or touching inventory.
        Class.forName("com.garyzhangscm.cwms.inventory.service.InventoryService");
        Class.forName("com.garyzhangscm.cwms.inventory.service.UserService");
        Path dir=Files.createTempDirectory("removal-test-");
        AtomicInteger current=new AtomicInteger(), maximum=new AtomicInteger(), calls=new AtomicInteger();
        CountDownLatch started=new CountDownLatch(2), release=new CountDownLatch(1);
        try(DurableRemovalQueue q=new DurableRemovalQueue(dir,2,4,t->{
            int n=current.incrementAndGet(); maximum.accumulateAndGet(n,Math::max); calls.incrementAndGet();
            started.countDown(); release.await(); current.decrementAndGet(); return "COMPLETED";
        })) {
            check(q.submit(Arrays.asList(task(1),task(2),task(3),task(4)))==4,"batch acceptance");
            check(started.await(3,TimeUnit.SECONDS),"workers did not start");
            check(q.submit(Arrays.asList(task(1),task(2)))==0,"duplicate replay");
            try { q.submit(Arrays.asList(task(5))); throw new AssertionError("overflow accepted"); }
            catch(IllegalStateException expected) { }
            check(!q.wasSubmitted(5),"overflow partially admitted");
            release.countDown(); awaitDone(q);
            check(maximum.get()==2 && calls.get()==4,"concurrency not bounded");
        }
        try(DurableRemovalQueue q=new DurableRemovalQueue(dir,2,4,t->{throw new AssertionError("completed work replayed");})) {
            check(q.submit(Arrays.asList(task(1)))==0,"restart duplicate replay");
        }
        Path recovery=Files.createTempDirectory("removal-recovery-");
        DurableRemovalQueue.Batch b=new DurableRemovalQueue.Batch(); b.id="recovery";
        DurableRemovalQueue.Task running=task(10); running.status="RUNNING";
        b.tasks=Arrays.asList(running,task(11)); new ObjectMapper().writeValue(recovery.resolve("recovery.json").toFile(),b);
        AtomicInteger recovered=new AtomicInteger();
        try(DurableRemovalQueue q=new DurableRemovalQueue(recovery,1,4,t->{
            check(t.inventoryId==11,"interrupted task replayed"); recovered.incrementAndGet(); throw new IllegalStateException("simulated failure");
        })) { awaitDone(q); check(recovered.get()==1,"queued task not recovered"); }
        DurableRemovalQueue.Batch result=new ObjectMapper().readValue(recovery.resolve("recovery.json").toFile(),DurableRemovalQueue.Batch.class);
        check(result.tasks.stream().allMatch(t->"UNCERTAIN".equals(t.status)),"uncertainty not persisted");
        try(DurableRemovalQueue q=new DurableRemovalQueue(recovery,1,4,t->{throw new AssertionError("failed work replayed");})) {
            check(q.submit(Arrays.asList(task(10),task(11)))==0,"uncertain task resubmitted");
        }
        Path tenDir=Files.createTempDirectory("removal-ten-");
        AtomicInteger tenCurrent=new AtomicInteger(), tenMax=new AtomicInteger();
        CountDownLatch tenStarted=new CountDownLatch(10), tenRelease=new CountDownLatch(1);
        try(DurableRemovalQueue q=new DurableRemovalQueue(tenDir,10,12,t->{
            int n=tenCurrent.incrementAndGet(); tenMax.accumulateAndGet(n,Math::max);
            tenStarted.countDown(); tenRelease.await(); tenCurrent.decrementAndGet(); return "COMPLETED";
        })) {
            List<DurableRemovalQueue.Task> dozen=new ArrayList<>();
            for(long id=100;id<112;id++) dozen.add(task(id));
            check(q.submit(dozen)==12,"ten-worker batch admission");
            check(tenStarted.await(5,TimeUnit.SECONDS),"ten workers did not start");
            check(tenMax.get()==10,"expected exactly ten concurrent workers");
            try { q.submit(Arrays.asList(task(112))); throw new AssertionError("ten-worker overflow accepted"); }
            catch(IllegalStateException expected) { }
            tenRelease.countDown(); awaitDone(q);
        }
        System.out.println("PASS: ten workers and bounded capacity; bounded workers/capacity, atomic rejection, dedup, restart recovery, uncertainty persistence and bytecode verification");
    }
}

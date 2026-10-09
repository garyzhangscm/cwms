package com.garyzhangscm.cwms.workorder.service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.garyzhangscm.cwms.workorder.clients.*;
import com.garyzhangscm.cwms.workorder.model.*;
import org.springframework.transaction.support.*;

/** Standalone local H2 concurrency check; never connects to business databases. */
public class ProductionQuantityLocalCheck {
    static final String URL="jdbc:h2:mem:production;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    static Connection connection() throws SQLException { return DriverManager.getConnection(URL); }
    static void increment(Connection c) throws SQLException {
        try (PreparedStatement p=c.prepareStatement(ProductionQuantityService.INCREMENT_SQL.replaceAll("\\?[1-4]", "?"))) {
            p.setLong(1,1); p.setTimestamp(2,new Timestamp(System.currentTimeMillis()));
            p.setString(3,"LOCAL-TEST"); p.setLong(4,1);
            if(p.executeUpdate()!=1) throw new AssertionError();
        }
    }
    static long count() throws SQLException {
        try(Connection c=connection(); ResultSet r=c.createStatement().executeQuery("select produced_quantity from work_order where work_order_id=1")) {r.next(); return r.getLong(1);}
    }
    static void reset() throws SQLException {try(Connection c=connection()){c.createStatement().execute("update work_order set produced_quantity=0");}}
    public static void main(String[] args) throws Exception {
        try(Connection c=connection()) {
            c.createStatement().execute("create table work_order(work_order_id bigint primary key, produced_quantity bigint, last_modified_time timestamp, last_modified_by varchar(100))");
            c.createStatement().execute("insert into work_order(work_order_id,produced_quantity) values(1,0)");
        }
        ExecutorService workers=Executors.newFixedThreadPool(8);
        List<Future<?>> futures=new ArrayList<>();
        for(int n=0;n<40;n++) futures.add(workers.submit(()->{
            try(Connection c=connection()){ c.setAutoCommit(false); increment(c); c.commit(); }
            catch(Exception e){throw new RuntimeException(e);}
        }));
        for(Future<?> f:futures) f.get();
        if(count()!=40) throw new AssertionError("Lost concurrent increments");
        try(Connection c=connection()){c.setAutoCommit(false); increment(c); c.rollback();}
        if(count()!=40) throw new AssertionError("Rollback changed quantity");
        reset(); futures.clear(); AtomicInteger accepted=new AtomicInteger();
        for(int n=0;n<20;n++) futures.add(workers.submit(()->{
            try(Connection c=connection()){
                c.setAutoCommit(false);
                try(ResultSet r=c.createStatement().executeQuery("select produced_quantity from work_order where work_order_id=1 for update")) {
                    r.next(); if(r.getLong(1)<5){increment(c);accepted.incrementAndGet();}
                }
                c.commit();
            } catch(Exception e){throw new RuntimeException(e);}
        }));
        for(Future<?> f:futures) f.get(); workers.shutdown();
        if(count()!=5||accepted.get()!=5)throw new AssertionError("Concurrent overproduction");
        AtomicInteger sends=new AtomicInteger(); CountDownLatch sent=new CountDownLatch(1);
        KafkaSender sender=new KafkaSender(){@Override public void send(Alert a){sends.incrementAndGet(); sent.countDown();}};
        WarehouseLayoutServiceRestemplateClient layout=new WarehouseLayoutServiceRestemplateClient(){
            @Override public Warehouse getWarehouseById(Long id){Warehouse w=new Warehouse();w.setCompanyId(1L);return w;}
        };
        ProductionNotificationService notifications=new ProductionNotificationService(sender,layout);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        notifications.afterCommit(1L,"TEST",0,"TEST");
        if(sends.get()!=0) throw new AssertionError("Notification before commit");
        for(var sync:TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        TransactionSynchronizationManager.clear();
        if(sends.get()!=0)throw new AssertionError("Rollback sent a notification");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        notifications.afterCommit(1L,"TEST",0,"TEST");
        for(var sync:TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
        if(!sent.await(3,java.util.concurrent.TimeUnit.SECONDS)||sends.get()!=1)throw new AssertionError("Notification not dispatched");
        TransactionSynchronizationManager.clear(); notifications.close();
        System.out.println("PASS: 40 concurrent increments, rollback, concurrent limit=5, commit-only notification");
    }
}

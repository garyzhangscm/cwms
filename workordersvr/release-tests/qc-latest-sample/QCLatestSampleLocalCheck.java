package com.garyzhangscm.cwms.workorder.service;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import java.time.Instant;
import java.lang.reflect.Field;
import java.util.*;

/** Real Hibernate/H2 query test with synthetic history only; no business DB. */
public class QCLatestSampleLocalCheck {
    @Entity(name="TestAssignment") @Table(name="test_assignment")
    public static class Assignment {
        @Id public Long id;
        public Assignment() {}
        Assignment(long id) { this.id=id; }
    }
    @Entity(name="WorkOrderQCSample") @Table(name="test_qc_sample")
    public static class Row {
        @Id public Long id;
        public Long warehouseId;
        @ManyToOne public Assignment productionLineAssignment;
        public Instant createdTime;
        public String number;
        public Boolean removed;
        public Row() {}
        Row(long id,long warehouse,Assignment assignment,Instant time,String number,Boolean removed) {
            this.id=id;this.warehouseId=warehouse;this.productionLineAssignment=assignment;
            this.createdTime=time;this.number=number;this.removed=removed;
        }
    }
    public static class Capture implements StatementInspector {
        static final List<String> selects=new ArrayList<>();
        public String inspect(String sql) { if(sql.startsWith("select")) selects.add(sql); return sql; }
    }
    static class Service extends WorkOrderQCSampleService {
        int hydrated;
        @Override public WorkOrderQCSample findById(Long id) {
            hydrated++; WorkOrderQCSample value=new WorkOrderQCSample(); value.setId(id);return value;
        }
    }
    static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        Configuration config=new Configuration().addAnnotatedClass(Assignment.class).addAnnotatedClass(Row.class);
        config.setProperty("hibernate.connection.driver_class","org.h2.Driver");
        config.setProperty("hibernate.connection.url","jdbc:h2:mem:latest;MODE=PostgreSQL;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.hbm2ddl.auto","create-drop");
        config.setStatementInspector(new Capture());
        try(var factory=config.buildSessionFactory(); Session em=factory.openSession()) {
            var tx=em.beginTransaction();
            Assignment m04=new Assignment(7),other=new Assignment(8);em.persist(m04);em.persist(other);
            Instant base=Instant.parse("2026-01-01T00:00:00Z");
            for(int n=1;n<=914;n++) em.persist(new Row(n,3,m04,base.plusSeconds(n),"TEST-"+n,false));
            em.persist(new Row(2000,4,m04,base.plusSeconds(20000),"OTHER-WAREHOUSE",false));
            em.persist(new Row(2001,3,other,base.plusSeconds(20000),"OTHER-ASSIGNMENT",false));
            em.persist(new Row(2002,3,m04,base.plusSeconds(20000),"REMOVED",true));
            em.persist(new Row(2003,3,m04,base.plusSeconds(20000),"NULL-REMOVED",null));
            tx.commit();em.clear();
            Service service=new Service();Field field=WorkOrderQCSampleService.class.getDeclaredField("entityManager");
            field.setAccessible(true);field.set(service,em);
            Capture.selects.clear();
            List<WorkOrderQCSample> latest=service.findLatestForAssignment(3L,7L);
            check(latest.size()==1&&latest.get(0).getId()==914L,"Incorrect latest or scope");
            check(service.hydrated==1,"History associations were loaded");
            check(Capture.selects.size()==1,"Unexpected count/history queries");
            String sql=Capture.selects.get(0);
            check(sql.contains("fetch first")||sql.contains("limit"),"Missing database row limit");
            check(service.findLatestForAssignment(99L,7L).isEmpty(),"Missing warehouse should return empty");
            check(service.hydrated==1,"Empty response must not hydrate");
            try {service.findLatestForAssignment(3L,null);throw new AssertionError("Missing assignment accepted");}catch(WorkOrderException expected){}
            try {service.findLatestForAssignment(0L,7L);throw new AssertionError("Invalid warehouse accepted");}catch(WorkOrderException expected){}
            tx=em.beginTransaction();
            em.persist(new Row(920,3,em.getReference(Assignment.class,7L),base.plusSeconds(10000),"SAME",false));
            em.persist(new Row(921,3,em.getReference(Assignment.class,7L),base.plusSeconds(10000),"SAME",false));
            tx.commit();em.clear();
            check(service.findLatestForAssignment(3L,7L).get(0).getId()==921L,"Tie order unstable");
            tx=em.beginTransaction();
            em.persist(new Row(930,3,em.getReference(Assignment.class,7L),null,"NULL-TIME",false));
            tx.commit();em.clear();
            Long oldFirst=em.createQuery("select s.id from WorkOrderQCSample s where s.warehouseId=3 and s.productionLineAssignment.id=7 and s.removed=false order by s.createdTime desc, s.number desc",Long.class).getResultList().get(0);
            check(service.findLatestForAssignment(3L,7L).get(0).getId().equals(oldFirst),"Legacy null ordering changed");
            System.out.println("PASS: real SQL limit=1; 914-history latest selection; warehouse/assignment isolation; removed exclusion; empty; invalid scope; stable ties; legacy null-date ordering");
            System.out.println("SQL: "+sql);
        }
    }
}

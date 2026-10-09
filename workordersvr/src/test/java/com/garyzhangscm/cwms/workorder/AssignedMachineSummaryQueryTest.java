package com.garyzhangscm.cwms.workorder;

import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.AssignedMachineSummaryRepository;
import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Query;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AssignedMachineSummaryQueryTest {
    @Test
    void scalarQueryFiltersHistoricalAndEndedOrdersAndPreservesAllMachinePairs() throws Exception {
        String url = "jdbc:h2:mem:machine_summary;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var sql = connection.createStatement()) {
            sql.execute("create table production_line (production_line_id bigint primary key, name varchar(50), warehouse_id bigint)");
            sql.execute("create table work_order (work_order_id bigint primary key, number varchar(50), item_id bigint, warehouse_id bigint, status varchar(40), expected_quantity bigint, produced_quantity bigint)");
            sql.execute("create table production_line_assignment (production_line_assignment_id bigint primary key, production_line_id bigint, work_order_id bigint, deassigned boolean)");
            sql.execute("insert into production_line values (1,'CM01',7),(2,'CM02',7),(3,'OTHER',8)");
            sql.execute("insert into work_order values (10,'WO10',99,7,'INPROCESS',100,12),(11,'WO11',99,7,'COMPLETED',100,100),(12,'WO12',99,7,'PENDING',100,0),(13,'WO13',99,8,'STAGED',100,0),(14,'WO14',99,7,'CLOSED',100,100),(15,'WO15',99,7,'CANCELLED',100,0),(16,'WO16',99,7,'WORK_IN_PROCESS',100,5)");
            sql.execute("insert into production_line_assignment values (1,1,10,false),(2,2,10,null),(3,1,10,false),(4,1,11,false),(5,1,12,true),(6,3,13,false),(7,1,14,false),(8,1,15,false),(9,2,16,false)");
        }
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", url)
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("hibernate.generate_statistics", "true").build();
        try {
            var metadata = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var entity : scanner.findCandidateComponents("com.garyzhangscm.cwms.workorder.model")) {
                metadata.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
            }
            try (SessionFactory factory = metadata.buildMetadata().buildSessionFactory();
                 var session = factory.openSession()) {
                String jpql = AssignedMachineSummaryRepository.class
                        .getMethod("findActiveMachineSummaries", Long.class, Long.class, List.class)
                        .getAnnotation(Query.class).value();
                var statuses = List.of(WorkOrderStatus.PENDING, WorkOrderStatus.INPROCESS,
                        WorkOrderStatus.STAGED, WorkOrderStatus.WORK_IN_PROCESS);
                factory.getStatistics().clear();
                var rows = session.createQuery(jpql, AssignedMachineSummary.class)
                        .setParameter("warehouseId",7L).setParameter("productionLineId",null)
                        .setParameter("statuses", statuses).getResultList();
                assertEquals(3, rows.size());
                assertEquals(List.of(1L,2L,2L), rows.stream().map(AssignedMachineSummary::productionLineId).toList());
                assertEquals(List.of(10L,10L,16L), rows.stream().map(AssignedMachineSummary::workOrderId).toList());
                assertEquals(12L, rows.get(0).producedQuantity());
                assertEquals(1, factory.getStatistics().getPrepareStatementCount());
                assertEquals(0, factory.getStatistics().getEntityLoadCount());
                assertEquals(0, factory.getStatistics().getCollectionLoadCount());
                var single = session.createQuery(jpql, AssignedMachineSummary.class)
                        .setParameter("warehouseId",7L).setParameter("productionLineId",1L)
                        .setParameter("statuses",statuses).getResultList();
                assertEquals(1, single.size());
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}

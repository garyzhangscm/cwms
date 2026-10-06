package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.deletion.WorkOrderDeletionRepository;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderDeletionServiceTest {
    static WorkOrder draft(long id) {
        WorkOrder order = new WorkOrder();
        order.setId(id); order.setWarehouseId(1L); order.setNumber("WO-" + id);
        order.setStatus(WorkOrderStatus.PENDING); order.setProducedQuantity(0L);
        return order;
    }

    @Test void onlyUntouchedPendingDraftsPass() {
        WorkOrder order = draft(1);
        assertDoesNotThrow(() -> WorkOrderDeletionService.validateDraft(order));
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            if (status == WorkOrderStatus.PENDING) continue;
            order.setStatus(status);
            assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        }
        order.setStatus(WorkOrderStatus.PENDING); order.setProducedQuantity(1L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        order.setProducedQuantity(-1L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        order.setProducedQuantity(0L); order.setQcQuantityRequested(1L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
    }

    @Test void materialAndByProductActivityBlockDeletion() {
        WorkOrder order = draft(1);
        WorkOrderLine line = new WorkOrderLine();
        line.setExpectedQuantity(10L); line.setOpenQuantity(10L);
        order.getWorkOrderLines().add(line);
        assertDoesNotThrow(() -> WorkOrderDeletionService.validateDraft(order));
        line.setOpenQuantity(9L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        line.setOpenQuantity(10L); line.setConsumedQuantity(1L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        line.setConsumedQuantity(0L); line.setMaterialWorkOrder(draft(2));
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        order.getWorkOrderLines().clear();
        WorkOrderByProduct product = new WorkOrderByProduct(); product.setProducedQuantity(1L);
        order.getWorkOrderByProducts().add(product);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
        order.getWorkOrderByProducts().clear(); order.setBtoOutboundOrderId(10L);
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.validateDraft(order));
    }

    @Test void malformedIdsAreRejectedAndDuplicatesAreNormalized() {
        assertEquals(new TreeSet<>(List.of(1L, 2L)), WorkOrderDeletionService.parseIds("2,1,2"));
        for (String value : List.of("", "0", "-1", "1,", "1,a", "1 OR 1=1", "9223372036854775808"))
            assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.parseIds(value));
        assertThrows(WorkOrderException.class, () -> WorkOrderDeletionService.parseIds(String.join(",", Collections.nCopies(51, "1"))));
    }

    @Nested class MySqlReferences {
        JdbcTemplate jdbc;
        WorkOrderDeletionRepository repository;
        WorkOrderDeletionService service;
        final List<String> tables = List.of("inventory", "inventory_archive", "pick", "short_allocation",
                "allocation_transaction_history", "production_line_assignment", "work_order_produce_transaction",
                "work_order_line", "work_order_by_product", "work_order_instruction", "cancelled_pick",
                "work_order_line_consume_transaction", "integration_work_order", "integration_work_order_confirmation");

        @BeforeEach void isolatedDatabase() {
            String url = System.getProperty("deletion.test.mysql.url");
            Assumptions.assumeTrue(url != null, "Set deletion.test.mysql.url to an isolated disposable MySQL schema.");
            assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/cwms_delete_test.*"), "Never run destructive fixtures on a production database.");
            DriverManagerDataSource ds = new DriverManagerDataSource(url, "root", "");
            jdbc = new JdbcTemplate(ds);
            jdbc.execute("DROP TABLE IF EXISTS future_work_order_event");
            jdbc.execute("DROP TABLE IF EXISTS work_order");
            jdbc.execute("CREATE TABLE work_order(work_order_id BIGINT PRIMARY KEY,warehouse_id BIGINT,number VARCHAR(64)) ENGINE=InnoDB");
            for (String table : tables) {
                jdbc.execute("DROP TABLE IF EXISTS `" + table + "`");
                jdbc.execute("CREATE TABLE `" + table + "`(work_order_id BIGINT,work_order_line_id BIGINT,work_order_by_product_id BIGINT,material_work_order_id BIGINT,consume_from_work_order_id BIGINT,number VARCHAR(64),warehouse_id BIGINT) ENGINE=InnoDB");
            }
            repository = mock(WorkOrderDeletionRepository.class);
            when(repository.findForDeletion(anyLong())).thenAnswer(call -> {
                Long id = call.getArgument(0);
                assertEquals("SERIALIZABLE", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
                return jdbc.queryForObject("SELECT COUNT(*) FROM work_order WHERE work_order_id=?", Integer.class, id) == 0
                        ? Optional.empty() : Optional.of(draft(id));
            });
            doAnswer(call -> { WorkOrder order = call.getArgument(0); jdbc.update("DELETE FROM work_order WHERE work_order_id=?", order.getId()); return null; })
                    .when(repository).delete(any(WorkOrder.class));
            ProxyFactory proxy = new ProxyFactory(new WorkOrderDeletionService(repository, jdbc));
            proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds), new AnnotationTransactionAttributeSource()));
            service = (WorkOrderDeletionService) proxy.getProxy();
            jdbc.update("INSERT INTO work_order VALUES(1,1,'WO-1'),(2,1,'WO-2')");
        }

        @Test void unusedDraftDeletesButOtherOrderRemains() {
            service.delete(1L, "1");
            assertEquals(0, count(1)); assertEquals(1, count(2));
            verify(repository).flush();
        }

        @Test void everyDirectBusinessReferenceBlocksDeletion() {
            for (String table : List.of("inventory", "inventory_archive", "production_line_assignment", "work_order_produce_transaction", "allocation_transaction_history")) {
                jdbc.update("INSERT INTO `" + table + "`(work_order_id) VALUES(1)");
                assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"), table);
                assertEquals(1, count(1));
                jdbc.update("DELETE FROM `" + table + "`");
            }
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        @Test void materialPicksAndCancelledHistoryBlockDeletion() {
            jdbc.update("INSERT INTO work_order_line(work_order_id,work_order_line_id) VALUES(1,10)");
            for (String table : List.of("pick", "short_allocation", "cancelled_pick")) {
                jdbc.update("INSERT INTO `" + table + "`(work_order_line_id) VALUES(10)");
                assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
                jdbc.update("DELETE FROM `" + table + "`");
            }
            jdbc.update("INSERT INTO work_order_line_consume_transaction(consume_from_work_order_id) VALUES(1)");
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        @Test void integrationHistoryBlocksEvenWithoutAssignedWarehouse() {
            jdbc.update("INSERT INTO integration_work_order(number,warehouse_id) VALUES('WO-1',NULL)");
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        @Test void failedBatchValidationDeletesNothing() {
            jdbc.update("INSERT INTO inventory(work_order_id) VALUES(2)");
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1,2"));
            assertEquals(1, count(1)); assertEquals(1, count(2));
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        @Test void newBusinessTablesAreDiscoveredAndStaleClientStateCannotBypassValidation() {
            jdbc.execute("CREATE TABLE future_work_order_event(work_order_id BIGINT) ENGINE=InnoDB");
            jdbc.update("INSERT INTO future_work_order_event VALUES(1)");
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
            jdbc.update("DELETE FROM future_work_order_event");
            WorkOrder active = draft(1); active.setStatus(WorkOrderStatus.INPROCESS);
            doReturn(Optional.of(active)).when(repository).findForDeletion(1L);
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        @Test void persistenceFailureRollsBackTheWholeBatch() {
            doThrow(new IllegalStateException("simulated storage failure")).when(repository).flush();
            assertThrows(IllegalStateException.class, () -> service.delete(1L, "1,2"));
            assertEquals(1, count(1)); assertEquals(1, count(2));
        }

        @Test void missingSchemaWrongWarehouseAndMissingIdsFailClosed() {
            assertThrows(WorkOrderException.class, () -> service.delete(2L, "1"));
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "999"));
            jdbc.execute("DROP TABLE inventory_archive");
            assertThrows(WorkOrderException.class, () -> service.delete(1L, "1"));
            verify(repository, never()).delete(any(WorkOrder.class));
        }

        int count(long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM work_order WHERE work_order_id=?", Integer.class, id); }
    }
}

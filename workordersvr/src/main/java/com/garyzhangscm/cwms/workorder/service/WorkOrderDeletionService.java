package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.deletion.WorkOrderDeletionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Only untouched drafts may be physically deleted. Any history blocks deletion. */
@Service
public class WorkOrderDeletionService {
    private final WorkOrderDeletionRepository repository;
    private final JdbcTemplate jdbc;

    public WorkOrderDeletionService(WorkOrderDeletionRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public void delete(Long warehouseId, String workOrderIds) {
        List<WorkOrder> orders = validateBatch(warehouseId, workOrderIds);
        orders.forEach(repository::delete);
        repository.flush();
    }

    /** Read-only in effect: runs the same checks and locks, without deleting any data. */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public void check(Long warehouseId, String workOrderIds) {
        validateBatch(warehouseId, workOrderIds);
    }

    private List<WorkOrder> validateBatch(Long warehouseId, String workOrderIds) {
        if (warehouseId == null || warehouseId <= 0) fail("A valid warehouse is required.");
        SortedSet<Long> ids = parseIds(workOrderIds);
        Map<String, Set<String>> schema = loadSchema();
        // Fail closed if cross-service tables cannot be inspected in this database.
        Map<String, String> required = Map.of("inventory", "work_order_id", "inventory_archive", "work_order_id",
                "pick", "work_order_line_id", "short_allocation", "work_order_line_id",
                "allocation_transaction_history", "work_order_id", "production_line_assignment", "work_order_id",
                "work_order_produce_transaction", "work_order_id", "work_order_line", "work_order_id",
                "work_order_by_product", "work_order_id");
        for (Map.Entry<String, String> table : required.entrySet()) {
            if (!schema.getOrDefault(table.getKey(), Set.of()).contains(table.getValue()))
                fail("Deletion is unavailable: business references cannot be verified.");
        }
        List<WorkOrder> orders = new ArrayList<>();
        // Deterministic locking and validation of the entire batch before the first delete.
        for (Long id : ids) {
            WorkOrder order = repository.findForDeletion(id)
                    .orElseThrow(() -> WorkOrderException.raiseException("Work order not found: " + id));
            if (!Objects.equals(warehouseId, order.getWarehouseId())) fail("Work order is outside the selected warehouse.");
            validateDraft(order);
            validateReferences(order, schema);
            orders.add(order);
        }
        return orders;
    }

    public static SortedSet<Long> parseIds(String value) {
        if (value == null || value.isBlank()) fail("Select at least one work order.");
        String[] parts = value.split(",", -1);
        if (parts.length > 50) fail("Delete at most 50 work orders at a time.");
        SortedSet<Long> ids = new TreeSet<>();
        for (String part : parts) {
            try {
                if (!part.trim().matches("[0-9]+")) throw new NumberFormatException();
                long id = Long.parseLong(part.trim());
                if (id <= 0) throw new NumberFormatException();
                ids.add(id);
            } catch (NumberFormatException e) { fail("Invalid work order ID."); }
        }
        return ids;
    }

    static void validateDraft(WorkOrder order) {
        if (order.getStatus() != WorkOrderStatus.PENDING) fail("Only PENDING work orders can be deleted.");
        if (nonzero(order.getProducedQuantity()) || nonzero(order.getQcQuantityRequested()) || nonzero(order.getQcQuantityCompleted()))
            fail("Work order has production or QC activity.");
        if (order.getShortAllocationId() != null || order.getProductionPlanLine() != null || order.getWorkOrderFlowLine() != null
                || order.getBtoOutboundOrderId() != null || order.getBtoCustomerId() != null)
            fail("Work order is linked to another business document.");
        for (WorkOrderLine line : order.getWorkOrderLines()) {
            if (line.getMaterialWorkOrder() != null || nonzero(line.getInprocessQuantity()) || nonzero(line.getConsumedQuantity())
                    || nonzero(line.getDeliveredQuantity()) || nonzero(line.getScrappedQuantity()) || nonzero(line.getReturnedQuantity())
                    || line.getExpectedQuantity() == null || line.getOpenQuantity() == null
                    || !Objects.equals(line.getOpenQuantity(), line.getExpectedQuantity()))
                fail("Work order has allocated or processed material.");
        }
        for (WorkOrderByProduct product : order.getWorkOrderByProducts()) {
            if (nonzero(product.getProducedQuantity())) fail("Work order has produced by-products.");
        }
    }

    private Map<String, Set<String>> loadSchema() {
        Map<String, Set<String>> schema = new TreeMap<>();
        jdbc.query("SELECT c.TABLE_NAME,c.COLUMN_NAME FROM information_schema.columns c "
                + "JOIN information_schema.tables t ON t.TABLE_SCHEMA=c.TABLE_SCHEMA AND t.TABLE_NAME=c.TABLE_NAME "
                + "WHERE c.TABLE_SCHEMA=DATABASE() AND t.TABLE_TYPE='BASE TABLE' "
                + "AND c.COLUMN_NAME IN ('work_order_id','material_work_order_id','consume_from_work_order_id',"
                + "'work_order_line_id','work_order_by_product_id','work_order_number','number','warehouse_id')", rs -> {
            String table = rs.getString(1), column = rs.getString(2);
            if (!table.matches("[a-zA-Z0-9_]+") || !column.matches("[a-zA-Z0-9_]+"))
                fail("Deletion is unavailable: unrecognized schema identifier.");
            schema.computeIfAbsent(table, key -> new TreeSet<>()).add(column);
        });
        return schema;
    }

    private void validateReferences(WorkOrder order, Map<String, Set<String>> schema) {
        for (Map.Entry<String, Set<String>> table : schema.entrySet()) {
            for (String column : table.getValue()) {
                String predicate;
                switch (column) {
                    case "work_order_id":
                        if (Set.of("work_order", "work_order_line", "work_order_instruction", "work_order_by_product").contains(table.getKey())) continue;
                        predicate = "=?"; break;
                    case "material_work_order_id": case "consume_from_work_order_id":
                        predicate = "=?"; break;
                    case "work_order_line_id":
                        if (table.getKey().equals("work_order_line")) continue;
                        predicate = " IN (SELECT work_order_line_id FROM work_order_line WHERE work_order_id=?)"; break;
                    case "work_order_by_product_id":
                        if (table.getKey().equals("work_order_by_product")) continue;
                        predicate = " IN (SELECT work_order_by_product_id FROM work_order_by_product WHERE work_order_id=?)"; break;
                    case "work_order_number":
                        if (hasNumberReference(table.getKey(), column, table.getValue(), order))
                            fail("Work order has related business documents. Deletion is not allowed.");
                        continue;
                    default: continue;
                }
                if (!jdbc.queryForList("SELECT 1 FROM `" + table.getKey() + "` WHERE `" + column + "`" + predicate + " LIMIT 1",
                        Integer.class, order.getId()).isEmpty()) {
                    fail("Work order has related business records. Deletion is not allowed.");
                }
            }
        }
        // Integration documents identify their work order by number rather than its database ID.
        for (String table : List.of("integration_work_order", "integration_work_order_confirmation")) {
            if (schema.containsKey(table)) {
                Set<String> columns = schema.get(table);
                if (!columns.containsAll(Set.of("number", "warehouse_id"))) fail("Integration references cannot be verified.");
                if (hasNumberReference(table, "number", columns, order))
                    fail("Work order has Integration records. Deletion is not allowed.");
            }
        }
    }

    private boolean hasNumberReference(String table, String column, Set<String> columns, WorkOrder order) {
        String sql = "SELECT 1 FROM `" + table + "` WHERE `" + column + "`=?";
        if (columns.contains("warehouse_id")) {
            return !jdbc.queryForList(sql + " AND (warehouse_id=? OR warehouse_id IS NULL) LIMIT 1",
                    Integer.class, order.getNumber(), order.getWarehouseId()).isEmpty();
        }
        return !jdbc.queryForList(sql + " LIMIT 1", Integer.class, order.getNumber()).isEmpty();
    }

    private static boolean nonzero(Long value) { return value != null && value != 0; }
    private static void fail(String message) { throw WorkOrderException.raiseException(message); }
}

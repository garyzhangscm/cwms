package com.garyzhangscm.cwms.outbound.loadlink;

import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class LoadCreationStore {
    private final JdbcTemplate db;
    private final TransactionTemplate transaction;

    public LoadCreationStore(DataSource source, PlatformTransactionManager manager) {
        db = new JdbcTemplate(source);
        transaction = new TransactionTemplate(manager);
    }

    public Map<String, Object> create(long warehouseId, long companyId, String number, String actor) {
        if (warehouseId <= 0 || companyId <= 0 || actor == null || actor.isBlank() || actor.length() > 50) {
            throw new IllegalArgumentException("An authenticated warehouse operator is required.");
        }
        String normalized = number == null ? "" : number.trim();
        if (normalized.isEmpty() || normalized.length() > 100 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Enter a Load number between 1 and 100 characters.");
        }
        return transaction.execute(status -> {
            db.update("INSERT IGNORE INTO quantity_allocation_mutex(warehouse_id) VALUES(?)", warehouseId);
            db.queryForObject("SELECT warehouse_id FROM quantity_allocation_mutex WHERE warehouse_id=? FOR UPDATE", Long.class, warehouseId);
            long existing = db.queryForObject("SELECT COUNT(*) FROM trailer_appointment WHERE warehouse_id=? AND number=?", Long.class, warehouseId, normalized);
            if (existing > 0) throw new IllegalArgumentException("This Load number already exists. Choose another number or open the existing Load.");
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            db.update(connection -> {
                var statement = connection.prepareStatement(
                    "INSERT INTO trailer_appointment(warehouse_id,company_id,number,type,status,created_by,last_modified_by,created_time,last_modified_time) VALUES(?,?,?,'SHIPPING','PLANNED',?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, warehouseId);
                statement.setLong(2, companyId);
                statement.setString(3, normalized);
                statement.setString(4, actor);
                statement.setString(5, actor);
                return statement;
            }, key);
            return Map.of("id", key.getKey().longValue(), "number", normalized, "status", "PLANNED");
        });
    }
}

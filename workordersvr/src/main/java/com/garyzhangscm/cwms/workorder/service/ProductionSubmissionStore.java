package com.garyzhangscm.cwms.workorder.service;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ProductionSubmissionStore {
    private final JdbcTemplate jdbc;

    public ProductionSubmissionStore(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public Long claim(Long warehouseId, String requestKey, String requestedBy, String requestHash) {
        jdbc.update("""
                INSERT INTO work_order_production_submission
                    (warehouse_id, request_key, requested_by, request_hash)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE request_key = request_key
                """, warehouseId, requestKey, requestedBy, requestHash);
        return jdbc.queryForObject("""
                SELECT requested_by, request_hash, transaction_id
                FROM work_order_production_submission
                WHERE warehouse_id = ? AND request_key = ?
                FOR UPDATE
                """, (result, rowNumber) -> {
            if (!Objects.equals(requestedBy, result.getString("requested_by"))
                    || !Objects.equals(requestHash, result.getString("request_hash"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This production submission ID was already used");
            }
            return result.getObject("transaction_id", Long.class);
        }, warehouseId, requestKey);
    }

    public void complete(Long warehouseId, String requestKey, Long transactionId) {
        jdbc.update("""
                UPDATE work_order_production_submission
                SET transaction_id = ?
                WHERE warehouse_id = ? AND request_key = ?
                """, transactionId, warehouseId, requestKey);
    }
}

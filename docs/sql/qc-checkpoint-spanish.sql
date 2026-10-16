-- Run on the target database BEFORE deploying Inventory Service with checkPointEs.
-- Additive and repeatable; no QC results, rules or translations are overwritten.
SET SESSION lock_wait_timeout = 10;
SET @qc_es_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qc_rule_item'
      AND column_name = 'check_point_es'),
  'SELECT ''check_point_es already exists'' AS migration_status',
  'ALTER TABLE qc_rule_item ADD COLUMN check_point_es VARCHAR(1000) CHARACTER SET utf8mb4 NULL, ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE qc_es_statement FROM @qc_es_ddl;
EXECUTE qc_es_statement;
DEALLOCATE PREPARE qc_es_statement;
-- Keep the nullable column on application rollback; older clients ignore it.

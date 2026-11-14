package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

final class LoadPalletCounts {
    private LoadPalletCounts() {}

    // Completed batches preserve the actual LPNs even after inventory moves again.
    // Only report counts when the available records cover every linked Shipment.
    static final String QUERY = """
        WITH linked AS (
          SELECT t.trailer_appointment_id AS load_id, s.shipment_id, s.status,
                 b.completed_at, b.snapshot_json
          FROM trailer_appointment t
          JOIN stop st ON st.trailer_appointment_id=t.trailer_appointment_id AND st.warehouse_id=t.warehouse_id
          JOIN shipment s ON s.stop_id=st.stop_id AND s.warehouse_id=t.warehouse_id
          LEFT JOIN shipment_inventory_batch b ON b.shipment_id=s.shipment_id AND b.warehouse_id=s.warehouse_id
          WHERE t.warehouse_id=? AND t.company_id=? AND t.type='SHIPPING'
        ), amounts AS (
          SELECT a.load_id,a.shipment_id,
                 SUM(CASE WHEN a.status='DISPATCHED' THEN sl.shipped_quantity
                          ELSE sl.inprocess_quantity END) AS expected_quantity
          FROM linked a
          LEFT JOIN shipment_line sl ON sl.shipment_id=a.shipment_id AND sl.warehouse_id=?
          GROUP BY a.load_id,a.shipment_id
        ), pallet_rows AS (
          SELECT a.load_id,a.shipment_id,j.lpn,j.quantity
          FROM linked a
          JOIN JSON_TABLE(CAST(COALESCE(a.snapshot_json,JSON_ARRAY()) AS JSON),'$[*]' COLUMNS(
               lpn VARCHAR(100) PATH '$.lpn', quantity BIGINT PATH '$.quantity')) j
          WHERE a.completed_at IS NOT NULL AND j.quantity>0
          UNION ALL
          SELECT a.load_id,a.shipment_id,i.lpn,i.quantity
          FROM linked a
          JOIN shipment_line sl ON sl.shipment_id=a.shipment_id AND sl.warehouse_id=?
          JOIN pick p ON p.shipment_line_id=sl.shipment_line_id AND p.warehouse_id=sl.warehouse_id
          JOIN inventory i ON i.pick_id=p.pick_id AND i.warehouse_id=p.warehouse_id
          WHERE a.completed_at IS NULL AND i.quantity>0
        ), checked AS (
          SELECT a.load_id,a.shipment_id,
                 a.expected_quantity>0 AND COALESCE(SUM(CASE WHEN NULLIF(TRIM(p.lpn),'') IS NOT NULL
                       THEN p.quantity ELSE 0 END),0)=a.expected_quantity AS verified
          FROM amounts a LEFT JOIN pallet_rows p ON p.load_id=a.load_id AND p.shipment_id=a.shipment_id
          GROUP BY a.load_id,a.shipment_id,a.expected_quantity
        )
        SELECT c.load_id,COUNT(DISTINCT NULLIF(TRIM(p.lpn),'')) AS pallet_count
        FROM checked c LEFT JOIN pallet_rows p ON p.load_id=c.load_id AND p.shipment_id=c.shipment_id
        GROUP BY c.load_id
        HAVING MIN(COALESCE(c.verified,0))=1
        """;

    static List<Map<String,Object>> query(JdbcTemplate db, long warehouseId, long companyId) {
        return db.queryForList(QUERY, warehouseId, companyId, warehouseId, warehouseId);
    }
}

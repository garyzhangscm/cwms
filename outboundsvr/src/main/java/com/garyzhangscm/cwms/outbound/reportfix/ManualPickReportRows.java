package com.garyzhangscm.cwms.outbound.reportfix;

import com.garyzhangscm.cwms.outbound.model.Inventory;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Presentation only. Never change inventory dates, quantities or candidate order. */
public final class ManualPickReportRows {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private ManualPickReportRows() {}

    public static List<String> build(List<Inventory> candidates) {
        List<Inventory> ordered = new ArrayList<>();
        if (candidates != null) {
            for (Inventory inventory : candidates) {
                if (inventory == null) throw new IllegalArgumentException("Missing inventory record in manual pick report");
                ordered.add(inventory);
            }
        }
        // Stable sort: preserve the existing order for equal dates, including unknown dates.
        ordered.sort(Comparator.comparing(Inventory::getInWarehouseDatetime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Map<String, List<Inventory>> groups = new LinkedHashMap<>();
        for (Inventory inventory : ordered) {
            String location = inventory.getLocation() == null ? null : inventory.getLocation().getName();
            if (location == null || location.trim().isEmpty()) {
                location = "Unknown location" + (inventory.getLocationId() == null ? "" : " (ID " + inventory.getLocationId() + ")");
            }
            groups.computeIfAbsent(location, key -> new ArrayList<>()).add(inventory);
        }
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, List<Inventory>> entry : groups.entrySet()) {
            ZonedDateTime earliest = null, latest = null;
            boolean unknown = false;
            for (Inventory inventory : entry.getValue()) {
                ZonedDateTime date = inventory.getInWarehouseDatetime();
                if (date == null) { unknown = true; continue; }
                if (earliest == null || date.isBefore(earliest)) earliest = date;
                if (latest == null || date.isAfter(latest)) latest = date;
            }
            String range = earliest == null ? "Unknown" : DATE.format(earliest) + " ~ " + DATE.format(latest);
            if (earliest != null && unknown) range += " (some dates unknown)";
            rows.add(entry.getKey() + ": " + range);
            for (Inventory inventory : entry.getValue()) {
                rows.add("    # " + value(inventory.getLpn()) + ", " + value(inventory.getQuantity()) + ", "
                        + (inventory.getInWarehouseDatetime() == null ? "Unknown" : DATE.format(inventory.getInWarehouseDatetime())));
            }
        }
        return rows;
    }

    private static String value(Object value) { return value == null ? "Unknown" : value.toString(); }
}

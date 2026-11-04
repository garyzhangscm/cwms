package com.garyzhangscm.cwms.workorder.clients;

import com.garyzhangscm.cwms.workorder.model.Inventory;
import java.util.*;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

final class DeliveredInventoryBatches {
    // Even 64 maximum-length BIGINTs plus URI encoding stay below 2 KiB.
    private static final int BATCH = 64;
    static List<Inventory> load(RestTemplateProxy proxy, Long warehouse, Long location, String csv) {
        Objects.requireNonNull(warehouse, "warehouseId");
        Objects.requireNonNull(location, "locationId");
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        Set<Long> distinct = new LinkedHashSet<>();
        for (String id : csv.split(",", -1)) distinct.add(Long.valueOf(id.trim()));
        List<Long> ids = new ArrayList<>(distinct);
        Map<Long,Inventory> found = new LinkedHashMap<>();
        for (int from = 0; from < ids.size(); from += BATCH) {
            StringJoiner batch = new StringJoiner(",");
            for (Long id : ids.subList(from, Math.min(ids.size(), from + BATCH))) batch.add(id.toString());
            String uri = UriComponentsBuilder.fromUriString("http://apigateway:5555/api/inventory/inventories")
                .queryParam("warehouseId", warehouse).queryParam("locationId", location)
                .queryParam("pickIds", batch.toString()).toUriString();
            List<Inventory> rows = proxy.exchangeList(Inventory.class, uri, HttpMethod.GET, null);
            for (Inventory row : rows) {
                if (found.putIfAbsent(Objects.requireNonNull(row.getId()), row) != null)
                    throw new IllegalStateException("Returnable inventory changed during lookup. Retry.");
            }
        }
        return new ArrayList<>(found.values());
    }
}

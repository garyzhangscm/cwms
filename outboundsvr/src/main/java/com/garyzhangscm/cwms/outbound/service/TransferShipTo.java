package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderCategory;
import com.garyzhangscm.cwms.outbound.model.Report;
import com.garyzhangscm.cwms.outbound.model.Warehouse;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

final class TransferShipTo {
    private TransferShipTo() {}

    private static Warehouse destinationIfBlank(Order order, WarehouseLayoutServiceRestemplateClient layout) {
        if (order.getCategory() != OrderCategory.WAREHOUSE_TRANSFER_ORDER ||
            order.getTransferReceiptWarehouseId() == null ||
            Stream.of(order.getShipToAddressLine1(), order.getShipToAddressLine2(),
                order.getShipToAddressLine3(), order.getShipToAddressCity(), order.getShipToAddressState(),
                order.getShipToAddressPostcode(), order.getShipToAddressCountry(),
                order.getShipToAddressCounty(), order.getShipToAddressDistrict()).anyMatch(StringUtils::hasText)) {
            return null;
        }
        return layout.getWarehouseById(order.getTransferReceiptWarehouseId());
    }

    static void onCreate(Order order, WarehouseLayoutServiceRestemplateClient layout) {
        if (order.getId() != null) return;
        Warehouse warehouse = destinationIfBlank(order, layout);
        if (warehouse == null) return;
        order.setShipToAddressLine1(warehouse.getAddressLine1());
        order.setShipToAddressLine2(warehouse.getAddressLine2());
        order.setShipToAddressCity(warehouse.getAddressCity());
        order.setShipToAddressState(warehouse.getAddressState());
        order.setShipToAddressPostcode(warehouse.getAddressPostcode());
        order.setShipToAddressCountry(warehouse.getAddressCountry());
        order.setShipToAddressCounty(warehouse.getAddressCounty());
        order.setShipToAddressDistrict(warehouse.getAddressDistrict());
    }

    static void onReport(Report report, Order order, WarehouseLayoutServiceRestemplateClient layout) {
        Warehouse warehouse = destinationIfBlank(order, layout);
        if (warehouse == null) return;
        report.addParameter("ship_to_address_line1", warehouse.getAddressLine1());
        String region = Stream.of(warehouse.getAddressState(), warehouse.getAddressPostcode())
            .filter(StringUtils::hasText).collect(Collectors.joining(" "));
        report.addParameter("ship_to_address_city_state_zipcode", Stream.of(warehouse.getAddressCity(), region)
            .filter(StringUtils::hasText).collect(Collectors.joining(", ")));
    }
}

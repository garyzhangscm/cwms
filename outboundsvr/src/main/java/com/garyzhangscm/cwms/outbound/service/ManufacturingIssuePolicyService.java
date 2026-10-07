package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

/** Validates manual manufacturing issues without changing existing reservations. */
@Service
public class ManufacturingIssuePolicyService {
    private final WarehouseLayoutServiceRestemplateClient layoutClient;
    private final PickRepository picks;

    public ManufacturingIssuePolicyService(WarehouseLayoutServiceRestemplateClient layoutClient,
                                           PickRepository picks) {
        this.layoutClient = layoutClient;
        this.picks = picks;
    }

    public void validate(Long warehouseId, Long lineId, Long itemId, Long destinationId,
                         Long sourceId, String lpn) {
        if (warehouseId == null || lineId == null || itemId == null || destinationId == null || sourceId == null
                || Strings.isBlank(lpn)) {
            throw PickingException.raiseException("Missing warehouse, material, production staging location or LPN for manual issue.");
        }
        WarehouseConfiguration configuration = layoutClient.getManufacturingIssueConfiguration(warehouseId);
        // Missing/legacy configuration stays strict; lookup errors propagate instead of bypassing validation.
        boolean requireSource = configuration == null ||
                !Boolean.FALSE.equals(configuration.getManufacturingIssueRequireSourceLocation());
        boolean requireLpn = configuration == null ||
                !Boolean.FALSE.equals(configuration.getManufacturingIssueRequireAllocatedLpn());
        if (!requireSource && !requireLpn) { return; }
        List<Pick> originalPicks = picks.findOpenWorkOrderPicksForIssue(warehouseId, lineId, itemId, destinationId);
        if (originalPicks.isEmpty()) { return; }
        // Both enabled rules must match the same allocation. Location-only allocations have no fixed LPN.
        boolean matches = originalPicks.stream().anyMatch(pick ->
                (!requireSource || Objects.equals(sourceId, pick.getSourceLocationId())) &&
                (!requireLpn || Strings.isBlank(pick.getLpn()) || Objects.equals(lpn, pick.getLpn())));
        if (!matches) {
            throw PickingException.raiseException("Manual material issue does not match an open allocation. " +
                    (requireSource ? "Source Location must match. " : "") +
                    (requireLpn ? "LPN must match when the allocation specifies an LPN. " : "") +
                    "Check Manufacturing material issue in Warehouse Configuration.");
        }
    }
}

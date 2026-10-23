package com.garyzhangscm.cwms.outbound.transit;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransitShippingService {
    @Value("${FAY_TRANSIT_ENABLED:false}") private boolean enabled;
    @Value("${FAY_TRANSIT_COMPANY_ID:0}") private Long companyId;
    @Value("${FAY_TRANSIT_WAREHOUSE_ID:0}") private Long warehouseId;
    @Value("${FAY_TRANSIT_CUSTOMER_IDS:}") private String customerIds;
    @Value("${FAY_TRANSIT_LOCATION_IDS:}") private String locationIds;
    @Value("${FAY_TRANSIT_CONFIG_FILE:/var/lib/cwms-in-transit/rules.json}") private String configFile;
    @Autowired private WarehouseLayoutServiceRestemplateClient layout;
    @Autowired private CommonServiceRestemplateClient common;
    private volatile TransitConfigurationStore store;

    private Set<Long> ids(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptySet();
        try {
            Set<Long> values = Arrays.stream(text.split(",")).map(String::trim).map(Long::valueOf).collect(Collectors.toSet());
            if (values.stream().anyMatch(v -> v <= 0)) throw new IllegalArgumentException();
            return values;
        } catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Invalid in-transit deployment scope"); }
    }
    private synchronized TransitConfigurationStore store() {
        if (store == null) store = new TransitConfigurationStore(Paths.get(configFile));
        return store;
    }
    public boolean applies(Long sourceWarehouse, Long customer) {
        return enabled && Objects.equals(warehouseId, sourceWarehouse) && customer != null && ids(customerIds).contains(customer);
    }
    /** Compatibility hook: every unselected customer executes the exact original location operation. */
    public static Location destination(WarehouseLayoutServiceRestemplateClient legacy, Long warehouse, Order order,
                                       TransitShippingService service) {
        if (service == null || !service.applies(warehouse, order.getShipToCustomerId())) return legacy.createOrderLocation(warehouse, order);
        TransitShippingConfiguration configuration = service.store().read();
        TransitShippingConfiguration.Rule rule = service.findRule(configuration, order.getShipToCustomerId());
        if (rule == null || !rule.enabled) return legacy.createOrderLocation(warehouse, order);
        service.validateWarehouse();
        return service.validateDestination(rule.destinationLocationId);
    }
    private TransitShippingConfiguration.Rule findRule(TransitShippingConfiguration doc, Long customer) {
        List<TransitShippingConfiguration.Rule> matches = doc.rules.stream().filter(r -> r != null && Objects.equals(r.customerId, customer)).collect(Collectors.toList());
        if (matches.size() > 1) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Duplicate customer rule; shipment blocked");
        return matches.isEmpty() ? null : matches.get(0);
    }
    private void validateWarehouse() {
        Warehouse warehouse = layout.getWarehouseById(warehouseId);
        if (warehouse == null || !Objects.equals(companyId, warehouse.getCompanyId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "In-transit source warehouse does not belong to configured company");
    }
    public Location validateDestination(Long id) {
        if (id == null || !ids(locationIds).contains(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an allowed in-transit location");
        Location location = layout.getLocationById(id);
        LocationGroup group = location == null ? null : location.getLocationGroup();
        LocationGroupType type = group == null ? null : group.getLocationGroupType();
        if (location == null || location.getWarehouse() == null || !Objects.equals(warehouseId, location.getWarehouse().getId()) ||
                !Boolean.TRUE.equals(location.getEnabled()) || group == null || type == null ||
                !Boolean.FALSE.equals(group.getPickable()) || !Boolean.FALSE.equals(type.getVirtual()) ||
                !Boolean.TRUE.equals(type.getFourWallInventory())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination must be enabled, non-virtual, non-pickable and inside the source warehouse");
        }
        return location;
    }
    public boolean configuredFor(Long warehouse) { return enabled && Objects.equals(warehouseId, warehouse); }
    public Map<String,Object> metadata(Long warehouse) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("featureEnabled", configuredFor(warehouse));
        if (!configuredFor(warehouse)) return result;
        validateWarehouse();
        List<Map<String,Object>> customers = new ArrayList<>();
        for (Long id : ids(customerIds)) {
            Customer customer = common.getCustomerById(id);
            if (customer == null || !Objects.equals(companyId, customer.getCompanyId()) ||
                    (customer.getWarehouseId() != null && !Objects.equals(warehouseId, customer.getWarehouseId()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer scope changed; check deployment settings");
            }
            Map<String,Object> value = new LinkedHashMap<>(); value.put("id",id); value.put("name",customer.getName()); customers.add(value);
        }
        List<Map<String,Object>> locations = new ArrayList<>();
        for (Long id : ids(locationIds)) { Location location=validateDestination(id); Map<String,Object> value=new LinkedHashMap<>(); value.put("id",id);value.put("name",location.getName());locations.add(value); }
        result.put("customers", customers);result.put("locations",locations);result.put("configuration",store().read());
        return result;
    }
    public TransitShippingConfiguration save(Long warehouse, TransitShippingConfiguration document, String actor) {
        if (!configuredFor(warehouse)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "In-transit shipping is not configured for this warehouse");
        validateWarehouse();
        if (document == null || document.rules == null || document.rules.size()>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid rules");
        Set<Long> seen=new HashSet<>();
        for (TransitShippingConfiguration.Rule rule:document.rules) {
            if (rule == null || rule.customerId == null || !ids(customerIds).contains(rule.customerId) || !seen.add(rule.customerId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select each allowed customer only once");
            Customer customer=common.getCustomerById(rule.customerId);
            if (customer==null || !Objects.equals(companyId,customer.getCompanyId()) || (customer.getWarehouseId()!=null && !Objects.equals(warehouseId,customer.getWarehouseId()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Customer belongs to a different company or warehouse");
            validateDestination(rule.destinationLocationId);
        }
        return store().write(document,actor);
    }
}

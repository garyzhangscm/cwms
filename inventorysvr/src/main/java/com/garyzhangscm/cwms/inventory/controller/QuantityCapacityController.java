package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.ClientRestriction;
import com.garyzhangscm.cwms.inventory.model.ClientValidationEndpoint;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import com.garyzhangscm.cwms.inventory.model.Location;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;

/** Same running candidate selector; no candidate entities cross the service boundary. */
@RestController
public class QuantityCapacityController {
    private final InventoryController delegate;
    @Autowired private DataSource source;
    @Autowired private WarehouseLayoutServiceRestemplateClient layout;
    @Value("${quantity.capacity.scoped-summary:false}") private boolean scopedSummary;
    // Preserve the existing selector's lock semantics (allow_pick=true excludes stock).
    // No Inventory entities, locks or package/UOM collections are hydrated by this query.
    public static final String SCOPED_SQL =
        "SELECT i.location_id, COUNT(*) row_count, SUM(CASE WHEN i.quantity IS NULL THEN 1 ELSE 0 END) null_count, " +
        "SUM(CASE WHEN i.quantity>0 THEN i.quantity ELSE 0 END) physical_quantity " +
        "FROM inventory i JOIN item it ON it.item_id=i.item_id " +
        "JOIN inventory_status st ON st.inventory_status_id=i.inventory_status_id " +
        "WHERE i.warehouse_id=? AND i.item_id=? AND i.inventory_status_id=? AND COALESCE(i.client_id,0)=? " +
        "AND i.pick_id IS NULL AND i.allocated_by_pick_id IS NULL AND i.virtual_inventory=FALSE " +
        "AND i.inbound_qc_required=FALSE AND i.locked_for_adjust=FALSE " +
        "AND NOT EXISTS (SELECT 1 FROM inventory_with_lock il JOIN inventory_lock l " +
        "ON l.inventory_lock_id=il.inventory_lock_id WHERE il.inventory_id=i.inventory_id AND l.allow_pick=TRUE) " +
        "GROUP BY i.location_id";
    public QuantityCapacityController(InventoryController delegate){this.delegate=delegate;}
    @GetMapping("/inventories/pickable/quantity-capacity-v1") @ClientValidationEndpoint
    public Map<String,Object> capacity(@RequestParam Long warehouseId,@RequestParam Long itemId,
            @RequestParam Long inventoryStatusId,@RequestParam(defaultValue="0") Long clientId,
            ClientRestriction restriction) throws java.io.UnsupportedEncodingException {
        validate(warehouseId,itemId,inventoryStatusId,clientId,restriction);
        if(scopedSummary) return scopedCapacity(warehouseId,itemId,inventoryStatusId,clientId);
        // Delegation preserves QC, locks, pickability, enabled locations and all existing candidate rules.
        List<Inventory> candidates=delegate.findPickableInventories(itemId,inventoryStatusId,
                "","","","","","","","","","",null,"",false,100000);
        return summarize(warehouseId,itemId,inventoryStatusId,clientId,candidates);
    }
    private Map<String,Object> scopedCapacity(long warehouse,long item,long status,long client) {
        long started=System.nanoTime(),physical=0,count=0;
        List<Map<String,Object>> groups=new JdbcTemplate(source).queryForList(SCOPED_SQL,warehouse,item,status,client);
        for(Map<String,Object> group:groups) {
            if(group.get("location_id")==null)throw new IllegalArgumentException("Inventory location is missing; review stock before reserving");
            Location location=layout.getLocationById(exact(group.get("location_id")));
            if(location==null||(location.getWarehouse()!=null&&!Objects.equals(location.getWarehouse().getId(),warehouse))||location.getLocationGroup()==null||location.getLocationGroup().getLocationGroupType()==null)
                throw new IllegalArgumentException("Inventory location scope is unavailable; review stock before reserving");
            if(!Boolean.TRUE.equals(location.getEnabled())||!Boolean.TRUE.equals(location.getLocationGroup().getPickable())||!Boolean.TRUE.equals(location.getLocationGroup().getLocationGroupType().getFourWallInventory()))continue;
            if(exact(group.get("null_count"))!=0)throw new IllegalArgumentException("Inventory quantity is missing");
            count=Math.addExact(count,exact(group.get("row_count")));
            physical=Math.addExact(physical,exact(group.get("physical_quantity")));
        }
        if(count>=100000)throw new IllegalArgumentException("Inventory candidate limit requires review");
        org.slf4j.LoggerFactory.getLogger(QuantityCapacityController.class).info("Scoped quantity capacity: warehouse={}, item={}, locations={}, eligible={}, elapsedMs={}",warehouse,item,groups.size(),count,(System.nanoTime()-started)/1000000);
        return Map.of("schema","quantity-capacity-v1","warehouseId",warehouse,"itemId",item,"inventoryStatusId",status,
                "clientId",client,"physicalQuantity",physical,"candidateCount",count,"eligibleCount",count);
    }
    private static long exact(Object value) {
        if(!(value instanceof Number))throw new IllegalArgumentException("Invalid inventory aggregate");
        return new BigDecimal(value.toString()).longValueExact();
    }
    public static void validate(Long warehouse,Long item,Long status,Long client,ClientRestriction restriction) {
        if(warehouse==null||warehouse<=0||item==null||item<=0||status==null||status<=0||client==null||client<0)
            throw new IllegalArgumentException("Invalid quantity capacity scope");
        if(restriction!=null&&Boolean.TRUE.equals(restriction.getThreePartyLogisticsFlag())&&!Boolean.TRUE.equals(restriction.getAllClientAccess())) {
            boolean permitted=client==0?Boolean.TRUE.equals(restriction.getNonClientDataAccessible()):
                restriction.getClientAccesses()!=null&&Arrays.stream(restriction.getClientAccesses().split(",")).anyMatch(value->value.trim().equals(client.toString()));
            if(!permitted)throw new IllegalArgumentException("Client inventory is outside the permitted scope");
        }
    }
    public static Map<String,Object> summarize(long warehouse,long item,long status,long client,List<Inventory> candidates) {
        if(candidates==null||candidates.size()>=100000)throw new IllegalArgumentException("Inventory candidate limit requires review");
        long physical=0;int eligible=0;
        for(Inventory row:candidates) {
            if(row==null)throw new IllegalArgumentException("Invalid inventory candidate");
            if(Objects.equals(row.getWarehouseId(),warehouse)&&row.getItem()!=null&&Objects.equals(row.getItem().getId(),item)
                &&row.getInventoryStatus()!=null&&Objects.equals(row.getInventoryStatus().getId(),status)
                &&(row.getClientId()==null?0:row.getClientId())==client&&row.getPickId()==null&&row.getAllocatedByPickId()==null&&!Boolean.TRUE.equals(row.getVirtual())) {
                if(row.getQuantity()==null)throw new IllegalArgumentException("Inventory quantity is missing");
                physical=Math.addExact(physical,Math.max(0,row.getQuantity()));eligible++;
            }
        }
        return Map.of("schema","quantity-capacity-v1","warehouseId",warehouse,"itemId",item,"inventoryStatusId",status,
                "clientId",client,"physicalQuantity",physical,"candidateCount",candidates.size(),"eligibleCount",eligible);
    }
}

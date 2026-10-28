package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.ClientRestriction;
import com.garyzhangscm.cwms.inventory.model.ClientValidationEndpoint;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Same running candidate selector; no candidate entities cross the service boundary. */
@RestController
public class QuantityCapacityController {
    private final InventoryController delegate;
    public QuantityCapacityController(InventoryController delegate){this.delegate=delegate;}
    @GetMapping("/inventories/pickable/quantity-capacity-v1") @ClientValidationEndpoint
    public Map<String,Object> capacity(@RequestParam Long warehouseId,@RequestParam Long itemId,
            @RequestParam Long inventoryStatusId,@RequestParam(defaultValue="0") Long clientId,
            ClientRestriction restriction) throws java.io.UnsupportedEncodingException {
        validate(warehouseId,itemId,inventoryStatusId,clientId,restriction);
        // Delegation preserves QC, locks, pickability, enabled locations and all existing candidate rules.
        List<Inventory> candidates=delegate.findPickableInventories(itemId,inventoryStatusId,
                "","","","","","","","","","",null,"",false,100000);
        return summarize(warehouseId,itemId,inventoryStatusId,clientId,candidates);
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

package com.garyzhangscm.cwms.outbound.quantity;

import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.persistence.*;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

/** Additive, opt-in quantity ledger. Existing Pick and Inventory records are never migrated. */
@Service
public class QuantityAllocationService {
    @Value("${quantity.allocation.enabled:false}") private boolean enabled;
    @Value("${quantity.allocation.batch-enabled:false}") private boolean batchEnabled;
    @Value("${quantity.allocation.summary-enabled:false}") private boolean summaryEnabled;
    @Value("${quantity.allocation.pick-lpn-only:false}") private boolean pickLpnOnly;
    public boolean pickLpnOnly(){return pickLpnOnly;}
    @Value("${quantity.allocation.default-warehouses:}") private String defaultWarehouses="";
    @PersistenceContext private EntityManager em;
    @Autowired @org.springframework.context.annotation.Lazy private OrderService orders;
    @Autowired @org.springframework.context.annotation.Lazy private PickService picks;
    @Autowired private UserService users;
    @Autowired private RestTemplateProxy rest;
    @Autowired @org.springframework.context.annotation.Lazy private com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient layout;
    @Autowired @org.springframework.context.annotation.Lazy private com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient inventory;
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper();
    // Valid only inside the warehouse-locked strategy invocation; never cached across requests.
    private static class CheckedAllocation {
        final long warehouse,item,status,client; final Long order; long remaining;
        CheckedAllocation(long w,long i,long s,long c,Long o,long q){warehouse=w;item=i;status=s;client=c;order=o;remaining=q;}
    }
    private final ThreadLocal<CheckedAllocation> checkedAllocation=new ThreadLocal<>();
    public void requireManualPickAllowed(Order order) {
        if(!enabled || order.getCategory()!=OrderCategory.SALES_ORDER || !defaultFor(order.getWarehouseId()) || enrolled(order.getId()))return;
        if(!legacyOrder(order.getId()))throw bad("Reserve quantity before picking this order. Open Sales Orders > Allocate, reserve the required quantity, then reload the order on your phone.");
    }
    private final ThreadLocal<Boolean> safeScan=ThreadLocal.withInitial(()->false);

    public QuantityAllocationService(DataSource source, PlatformTransactionManager manager) {
        db = new JdbcTemplate(source); tx = new TransactionTemplate(manager);
    }
    public boolean enabled() { return enabled; }
    public boolean defaultFor(long warehouse) {
        return enabled && Arrays.stream(defaultWarehouses.split(",")).map(String::trim).anyMatch(value->value.equals(Long.toString(warehouse)));
    }
    @Value("${quantity.allocation.strict-mode:false}") private boolean strictMode;
    private boolean legacyOrder(long id) {
        return strictMode ? db.queryForObject("SELECT COUNT(*) FROM quantity_allocation_legacy_order WHERE order_id=?",Long.class,id)>0 : hasShipment(id);
    }
    private boolean hasShipment(long id) {
        return db.queryForObject("SELECT COUNT(*) FROM shipment_line sl JOIN outbound_order_line ol ON sl.outbound_order_line_id=ol.outbound_order_line_id WHERE ol.outbound_order_id=?",Long.class,id)>0;
    }
    /** Deliberate Allocate click: resolve existing work first, never silently downgrade an unsupported new order. */
    public Map<String,Object> entry(long warehouse,long id,ClientRestriction restriction) {
        return locked(warehouse,()->{
            Order value=order(warehouse,id,restriction,true);
            if(enrolled(id))return snapshot(value);
            if(value.getCategory()!=OrderCategory.SALES_ORDER||!defaultFor(warehouse)||legacyOrder(id))return snapshot(value);
            return enable(warehouse,id,restriction);
        });
    }
    public void requireClassicAllocationAllowed(long id) {
        if(!enabled)return;
        Order value=orders.findById(id);
        if(enrolled(id))throw bad("Use Allocate to reserve quantities, then scan an LPN in Manual Pick");
        if(value.getCategory()==OrderCategory.SALES_ORDER && defaultFor(value.getWarehouseId()))locked(value.getWarehouseId(),()->{
            order(value.getWarehouseId(),id,null,true);
            if(enrolled(id)||!legacyOrder(id))throw bad("This warehouse uses quantity reservations by default. Open Allocate from the order list; unsupported orders require review");
            return null;
        });
    }
    public <T>T scanning(Supplier<T> action) {boolean prior=safeScan.get();safeScan.set(true);try{return action.get();}finally{if(prior)safeScan.set(true);else safeScan.remove();}}
    public void requireAllocationPermission(long warehouse) {
        requireEnabled();
        Long company=layout.getWarehouseById(warehouse).getCompanyId();
        User user=users.getCurrentUser(company);
        if(Boolean.TRUE.equals(user.getAdmin()) || Boolean.TRUE.equals(user.getSystemAdmin()))return;
        List<Map> permissions=rest.exchangeList(Map.class,"http://apigateway:5555/api/resource/users/permission/by-web-page?companyId="+company+"&warehouseId="+warehouse+"&webPageUrl=%2Foutbound%2Forder",HttpMethod.GET,null);
        boolean found=false;
        for(Map permission:permissions)if(permission.get("permission") instanceof Map && "allocate-order".equals(((Map)permission.get("permission")).get("name"))) {
            found=true;if(!Boolean.TRUE.equals(permission.get("allowAccess")))throw bad("Allocate Order permission is required");
        }
        if(!found)throw bad("Allocate Order permission is required");
    }
    public boolean enrolled(long orderId) {
        return enabled && db.queryForObject("SELECT COUNT(*) FROM quantity_allocation_order WHERE order_id=?",Long.class,orderId)>0;
    }
    public <T> T locked(long warehouse, Supplier<T> action) {
        requireEnabled();
        return tx.execute(status -> {
            // Acquire the exclusive record lock directly, including concurrent first use.
            // INSERT IGNORE takes shared duplicate-key locks; concurrent S-to-X upgrades deadlock.
            db.update("INSERT INTO quantity_allocation_mutex (warehouse_id) VALUES (?) ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id)",warehouse);
            db.queryForObject("SELECT warehouse_id FROM quantity_allocation_mutex WHERE warehouse_id=? FOR UPDATE",Long.class,warehouse);
            return action.get();
        });
    }
    public static class Update { public Long orderLineId; public Long held; public Long revision; }
    public static class Request { public String requestId; public List<Update> lines; }

    private Order order(long warehouse,long orderId,ClientRestriction restriction,boolean lock) {
        Order order=lock ? em.find(Order.class,orderId,LockModeType.PESSIMISTIC_WRITE) : orders.findById(orderId);
        if(order==null || !Objects.equals(order.getWarehouseId(),warehouse) || !accessible(order.getClientId(),restriction))
            throw bad("Order not found in the selected warehouse");
        if(lock) em.refresh(order);
        return order;
    }
    public void requireQueueOrderAccess(long warehouse,long id,ClientRestriction restriction) {
        requireEnabled();
        List<Map<String,Object>> rows=db.queryForList("SELECT warehouse_id,client_id FROM outbound_order WHERE outbound_order_id=?",id);
        if(rows.size()!=1 || number(rows.get(0),"warehouse_id")!=warehouse || !accessible(rows.get(0).get("client_id")==null?null:((Number)rows.get(0).get("client_id")).longValue(),restriction))
            throw bad("Order not found in the selected warehouse");
    }
    public Map<String,Object> view(long warehouse,long id,ClientRestriction restriction) {
        requireEnabled();
        return tx.execute(status -> snapshot(order(warehouse,id,restriction,false)));
    }
    public Map<String,Object> enable(long warehouse,long id,ClientRestriction restriction) {
        return locked(warehouse,()->{
            Order order=order(warehouse,id,restriction,true);
            if(enrolled(id)) return snapshot(order);
            if(order.getStatus()!=OrderStatus.OPEN || order.getCategory()!=OrderCategory.SALES_ORDER || Boolean.TRUE.equals(order.getCancelRequested()))
                throw bad("Only a fresh, open Sales Order can use quantity allocation");
            if(order.getOrderLines()==null || order.getOrderLines().isEmpty()) throw bad("Add order lines first");
            long planned=db.queryForObject("SELECT COUNT(*) FROM shipment_line sl JOIN outbound_order_line ol ON sl.outbound_order_line_id=ol.outbound_order_line_id WHERE ol.outbound_order_id=?",Long.class,id);
            if(planned>0) throw bad("This order already has shipment work. Keep the existing allocation flow; no automatic migration is allowed");
            Set<Long> items=new HashSet<>();
            for(OrderLine line:order.getOrderLines()) {
                if(Boolean.TRUE.equals(line.getNonAllocatable()) || line.getExpectedQuantity()==null || line.getExpectedQuantity()<=0 ||
                    line.getItemId()==null || line.getInventoryStatusId()==null || !items.add(line.getItemId()))
                    throw bad("Use positive, allocatable lines with a single line per item");
                if(hasRestrictions(line)) throw bad("This first quantity-only mode does not accept receipt or inventory-attribute restrictions. Keep this order on the existing flow");
            }
            db.update("INSERT INTO quantity_allocation_order (order_id,warehouse_id,created_by) VALUES (?,?,?)",id,warehouse,users.getCurrentUserName());
            for(OrderLine line:order.getOrderLines()) db.update("INSERT INTO quantity_allocation_line (order_line_id,order_id,warehouse_id,item_id,inventory_status_id,client_id,demand,held,committed,revision) VALUES (?,?,?,?,?,?,?,0,0,0)",line.getId(),id,warehouse,line.getItemId(),line.getInventoryStatusId(),client(line.getClientId()),line.getExpectedQuantity());
            order.setAllowForManualPick(true);
            em.flush();
            return snapshot(order);
        });
    }
    public Map<String,Object> update(long warehouse,long id,ClientRestriction restriction,Request request) {
        validate(request);
        return locked(warehouse,()->{
            Order order=order(warehouse,id,restriction,true); ensureEditable(order);
            String fingerprint=fingerprint(request);
            List<Map<String,Object>> prior=db.queryForList("SELECT * FROM quantity_allocation_request WHERE request_id=?",request.requestId);
            if(!prior.isEmpty()) {
                Map<String,Object> previous=prior.get(0);
                if(number(previous,"order_id")!=id || number(previous,"warehouse_id")!=warehouse || !fingerprint.equals(previous.get("fingerprint"))) throw bad("Request ID was reused with different allocation details");
                try { return json.readValue((String)previous.get("result_json"),Map.class); } catch(Exception e) {throw bad("Saved allocation result needs review");}
            }
            Map<Long,OrderLine> lines=new HashMap<>();for(OrderLine line:order.getOrderLines()) lines.put(line.getId(),line);
            Map<Long,Map<String,Object>> ledgerRows=new HashMap<>();
            for(Map<String,Object> row:db.queryForList("SELECT * FROM quantity_allocation_line WHERE order_id=?",id))ledgerRows.put(number(row,"order_line_id"),row);
            // Validate every line before changing any; a stale second line must roll back the first.
            for(Update change:request.lines) {
                OrderLine line=lines.get(change.orderLineId); if(line==null) throw bad("Order line does not belong to this order");
                Map<String,Object> row=ledgerRows.get(line.getId()); if(row==null)throw bad("Quantity allocation line was not found"); unchanged(line,row);
                if(number(row,"revision")!=change.revision) throw bad("Allocation changed in another session. Refresh before saving");
                QuantityBalance before=balance(row);
                if(change.held>before.demand-before.committed) throw bad("Reservation exceeds unpicked order demand");
            }
            // Process decreases first, so a multi-line edit never depends on display order.
            List<Update> changes=new ArrayList<>(request.lines);
            changes.sort(Comparator.comparingLong(change->change.held-number(ledgerRows.get(change.orderLineId),"held")));
            Map<String,Long> capacities=batchEnabled?batchCapacities(warehouse,changes,ledgerRows):null;
            Map<String,Long> promised=new HashMap<>();
            if(batchEnabled&&!ledgerRows.isEmpty()) {
                String items=ledgerRows.values().stream().map(row->number(row,"item_id")).distinct().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                for(Map<String,Object> row:db.queryForList("SELECT item_id,inventory_status_id,client_id,SUM(held) held FROM quantity_allocation_line WHERE warehouse_id=? AND item_id IN ("+items+") GROUP BY item_id,inventory_status_id,client_id",warehouse))promised.put(scopeKey(row),number(row,"held"));
            }
            for(Update change:changes) {
                Map<String,Object> row=ledgerRows.get(change.orderLineId); QuantityBalance before=balance(row);
                long available=change.held<=before.held ? before.held : Math.max(0,(capacities==null?capacity(warehouse,number(row,"item_id"),number(row,"inventory_status_id"),number(row,"client_id")):capacities.get(scopeKey(row)))-(batchEnabled?promised.getOrDefault(scopeKey(row),0L):held(warehouse,number(row,"item_id"),number(row,"inventory_status_id"),number(row,"client_id")))+before.held);
                QuantityBalance after=before.target(change.held,available); save(change.orderLineId,after);
                if(batchEnabled)promised.put(scopeKey(row),Math.addExact(promised.getOrDefault(scopeKey(row),0L),change.held-before.held));
            }
            Map<String,Object> result=snapshot(order);
            try {db.update("INSERT INTO quantity_allocation_request (request_id,order_id,warehouse_id,fingerprint,result_json,created_by) VALUES (?,?,?,?,?,?)",request.requestId,id,warehouse,fingerprint,json.writeValueAsString(result),users.getCurrentUserName());}
            catch(org.springframework.dao.DataAccessException e){throw e;}catch(Exception e){throw bad("Could not record allocation result");}
            return result;
        });
    }
    /** Called around the existing strategy bean, in the SAME database transaction as Pick creation. */
    public AllocationResult allocate(AllocationRequest request,Supplier<AllocationResult> proceed) {
        long warehouse=warehouseId(request),item=request.getItem().getId();
        return locked(warehouse,()->{
            long status=request.getInventoryStatus()!=null?request.getInventoryStatus().getId():request.getInventoryStatusId();
            OrderLine line=request.getShipmentLines().size()==1?request.getShipmentLines().get(0).getOrderLine():null;
            for(ShipmentLine shipment:request.getShipmentLines())requireManualPickAllowed(shipment.getOrderLine().getOrder());
            boolean own=line!=null && enrolled(line.getOrder().getId());
            if(own) {
                ensureEditable(line.getOrder());
                if(!safeScan.get())throw bad("Use the current mobile Manual Pick flow with a saved request ID for this quantity-allocated order. Update older clients before picking");
                if(!request.isManualAllocation() || request.getLpn()==null || request.getLpn().trim().isEmpty()) throw bad("Scan an LPN to pick this quantity-allocated order");
                Map<String,Object> row=ledger(line.getId()); unchanged(line,row);
                QuantityBalance before=balance(row); before.convert(request.getQuantity());
                // This flow intentionally accepts a single inventory row per scan, matching the stable split operation.
                List<Inventory> scanned=rest.exchangeList(Inventory.class,"http://apigateway:5555/api/inventory/inventories?warehouseId="+warehouse+"&lpn="+encode(request.getLpn())+"&includeDetails=false",HttpMethod.GET,null);
                if(scanned.size()!=1) throw bad("Scan a single-record LPN; mixed LPNs require the existing supervised flow");
                Inventory stock=scanned.get(0);
                if(!Objects.equals(stock.getWarehouseId(),warehouse) || !Objects.equals(stock.getItem().getId(),item) || stock.getInventoryStatus()==null || !Objects.equals(stock.getInventoryStatus().getId(),status) || !request.getLpn().equals(stock.getLpn()) || stock.getPickId()!=null || stock.getAllocatedByPickId()!=null || client(stock.getClientId())!=number(row,"client_id") || stock.getQuantity()<request.getQuantity()) throw bad("LPN item, owner, warehouse, status or available quantity does not match");
                if(pickLpnOnly&&!db.queryForList("SELECT request_id FROM manual_pick_operation WHERE warehouse_id=? AND source_inventory_id=? AND completed=FALSE AND cancelled=FALSE FOR UPDATE",warehouse,stock.getId()).isEmpty())throw bad("Another saved Pick is using this LPN. Resume that operation or scan another LPN");
                // Reserve checks the entire pool. An enrolled scan consumes only its own held quantity.
                // The selected LPN still passes the existing candidate selector and Inventory's row-locked final guard.
                if(!pickLpnOnly) {
                    long unpromised=capacity(warehouse,item,status,number(row,"client_id"))-held(warehouse,item,status,number(row,"client_id"))+before.held;
                    if(unpromised<request.getQuantity()) throw bad("Available stock fell below existing reservations. Check inventory and release or adjust reservations before picking");
                }
                AllocationResult result=proceed.get();
                long actual=0;for(Pick pick:result.getPicks()) {
                    if(pick.getShipmentLine()==null || !Objects.equals(pick.getShipmentLine().getOrderLine().getId(),line.getId())) throw bad("Generated Pick does not match the reserved order line");
                    actual=Math.addExact(actual,pick.getQuantity());
                }
                if(actual!=request.getQuantity() || !result.getShortAllocations().isEmpty()) throw bad("Stock changed while preparing the pick; refresh and retry after checking the result");
                save(line.getId(),before.convert(actual));em.flush();return result;
            }
            // Classic orders/work orders cannot take inventory promised by quantity-only orders.
            long promised=held(warehouse,item,status,client(line==null?request.getItem().getClientId():line.getClientId()));
            if(promised>0) {
                long free=Math.max(0,capacity(warehouse,item,status,client(line==null?request.getItem().getClientId():line.getClientId()))-promised);
                if(request.getQuantity()>free) throw bad("Stock is reserved for other sales orders. Unreserved quantity: "+free+". Reduce this request or release the other reservation first");
            }
            // Only a single sales-order line can share the checked budget. Other paths keep their guards.
            if(promised<=0 || line==null)return proceed.get();
            CheckedAllocation previous=checkedAllocation.get();
            checkedAllocation.set(new CheckedAllocation(warehouse,item,status,client(line.getClientId()),line.getOrder().getId(),request.getQuantity()));
            try{return proceed.get();}finally{if(previous==null)checkedAllocation.remove();else checkedAllocation.set(previous);}
        });
    }
    public long clamp(Order order,long item,long quantity) {
        if(!enrolled(order.getId())) return quantity;
        Long held=db.queryForObject("SELECT COALESCE(SUM(held),0) FROM quantity_allocation_line WHERE order_id=? AND item_id=?",Long.class,order.getId(),item);
        return Math.min(quantity,held);
    }
    public void restoreCancelled(Pick pick,long quantity) {
        if(quantity<=0 || pick.getShipmentLine()==null)return;
        OrderLine line=pick.getShipmentLine().getOrderLine();if(!enrolled(line.getOrder().getId()))return;
        save(line.getId(),balance(ledger(line.getId())).cancelUnpicked(quantity));
    }
    public void requirePickCancelable(Pick pick) {
        if(db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation WHERE pick_id=? AND completed=FALSE AND cancelled=FALSE",Long.class,pick.getId())>0)
            throw bad("This scan is still awaiting inventory confirmation. Resume the saved mobile operation before cancelling its Pick");
    }
    public Pick lockPick(long id) {
        Pick pick=em.find(Pick.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(pick==null)throw bad("Pick changed or was already cancelled. Refresh before continuing");
        em.refresh(pick);return pick;
    }
    public Map<String,Object> clampAvailability(long orderId,Map<String,Object> original) {
        if(!enrolled(orderId)) {
            Order order=orders.findById(orderId);
            boolean required=order.getCategory()==OrderCategory.SALES_ORDER && defaultFor(order.getWarehouseId()) && !legacyOrder(orderId);
            Map<String,Object> legacy=new LinkedHashMap<>(original);
            legacy.put("allocationMode",required?"RESERVATION_REQUIRED":"CLASSIC");
            legacy.put("reservationRequired",required);
            if(required) {
                List<Map<String,Object>> blocked=new ArrayList<>();
                for(Map<String,Object> source:(List<Map<String,Object>>)original.get("lines")) {Map<String,Object> row=new LinkedHashMap<>(source);row.put("remainingQuantity",0L);blocked.add(row);}
                legacy.put("lines",blocked);legacy.put("remainingQuantity",0L);legacy.put("canPick",false);
            }
            return legacy;
        }
        Map<String,Object> result=new LinkedHashMap<>(original);List<Map<String,Object>> lines=new ArrayList<>();long total=0;
        for(Map<String,Object> source:(List<Map<String,Object>>)original.get("lines")) {
            Map<String,Object> line=new LinkedHashMap<>(source);long held=number(ledger(((Number)line.get("orderLineId")).longValue()),"held");
            long quantity=Math.min(((Number)line.get("remainingQuantity")).longValue(),held);line.put("remainingQuantity",quantity);lines.add(line);total=Math.addExact(total,quantity);
        }
        result.put("lines",lines);result.put("remainingQuantity",total);result.put("canPick",total>0);result.put("allocationMode","ITEM_QUANTITY");return result;
    }
    public void requireNoHolds(long orderId) {
        if(enrolled(orderId) && db.queryForObject("SELECT COALESCE(SUM(held),0) FROM quantity_allocation_line WHERE order_id=?",Long.class,orderId)>0)
            throw bad("Release the remaining quantity allocation before changing or closing this order");
    }
    public boolean hasItemHolds(long warehouse,long item) {return db.queryForObject("SELECT COALESCE(SUM(held),0) FROM quantity_allocation_line WHERE warehouse_id=? AND item_id=?",Long.class,warehouse,item)>0;}
    public void validateOrderSave(Order order) {
        if(order.getId()==null||!enrolled(order.getId()))return;
        if(!Boolean.TRUE.equals(order.getAllowForManualPick()))throw bad("Keep Manual Pick enabled for a quantity-allocated order");
        if(order.getStatus()==OrderStatus.COMPLETE || order.getStatus()==OrderStatus.CANCELLED || Boolean.TRUE.equals(order.getCancelRequested()))requireNoHolds(order.getId());
        long count=db.queryForObject("SELECT COUNT(*) FROM quantity_allocation_line WHERE order_id=?",Long.class,order.getId());
        if(order.getOrderLines()==null||order.getOrderLines().size()!=count)throw bad("Quantity allocation order lines cannot be added or removed after activation");
        for(OrderLine line:order.getOrderLines())unchanged(line,ledger(line.getId()));
    }
    public void validateLineSave(OrderLine line) {
        if(line.getId()==null) {if(line.getOrder()!=null&&enrolled(line.getOrder().getId()))throw bad("Do not add lines to an active quantity allocation order");return;}
        List<Map<String,Object>> rows=db.queryForList("SELECT * FROM quantity_allocation_line WHERE order_line_id=?",line.getId());
        if(rows.isEmpty())return;
        Map<String,Object> saved=rows.get(0);
        if(line.getOrder()==null||!Objects.equals(line.getOrder().getId(),number(saved,"order_id"))||!Objects.equals(line.getWarehouseId(),number(saved,"warehouse_id")))throw bad("Quantity allocation line cannot change order or warehouse");
        unchanged(line,saved);
    }
    public void checkDirectPick(long warehouse,long item,long status,long client,long quantity,Long orderId) {
        if(orderId!=null && enrolled(orderId)) throw bad("Prepare this pick through the LPN scanning flow so its quantity reservation is converted atomically");
        CheckedAllocation checked=checkedAllocation.get();
        if(checked!=null && checked.warehouse==warehouse && checked.item==item && checked.status==status && checked.client==client && Objects.equals(checked.order,orderId)) {
            if(quantity<=0 || quantity>checked.remaining)throw bad("Generated Pick exceeds the quantity checked for this allocation");
            checked.remaining-=quantity;
            return;
        }
        long promised=held(warehouse,item,status,client);
        if(promised>0 && quantity>Math.max(0,capacity(warehouse,item,status,client)-promised))
            throw bad("This stock is reserved for sales orders. The requested pick exceeds unreserved quantity");
    }
    private Map<String,Object> snapshot(Order order) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("enabled",enabled);result.put("orderId",order.getId());result.put("orderNumber",order.getNumber());result.put("mode",enrolled(order.getId())?"ITEM_QUANTITY":"CLASSIC");
        List<Map<String,Object>> rows=new ArrayList<>();
        if(enrolled(order.getId())) for(OrderLine line:order.getOrderLines()) {
            Map<String,Object> row=ledger(line.getId());unchanged(line,row);QuantityBalance value=balance(row);
            Item item=line.getItem()==null?inventory.getItemById(line.getItemId()):line.getItem();
            Map<String,Object> view=new LinkedHashMap<>();view.put("orderLineId",line.getId());view.put("number",line.getNumber());view.put("itemId",line.getItemId());view.put("itemName",item==null?String.valueOf(line.getItemId()):item.getName());
            view.put("demand",value.demand);view.put("held",value.held);view.put("committed",value.committed);view.put("shortage",value.shortage());view.put("revision",number(row,"revision"));rows.add(view);
        }
        result.put("lines",rows);return result;
    }
    private String scopeKey(Map<String,Object> row){return number(row,"item_id")+":"+number(row,"inventory_status_id")+":"+number(row,"client_id");}
    private Map<String,Long> batchCapacities(long warehouse,List<Update> changes,Map<Long,Map<String,Object>> rows) {
        Map<String,Map<String,Long>> scopes=new LinkedHashMap<>();
        for(Update change:changes){Map<String,Object> row=rows.get(change.orderLineId);if(change.held>number(row,"held"))scopes.put(scopeKey(row),Map.of("itemId",number(row,"item_id"),"inventoryStatusId",number(row,"inventory_status_id"),"clientId",number(row,"client_id")));}
        Map<String,Long> result=new HashMap<>();List<Map<String,Long>> values=new ArrayList<>(scopes.values());
        for(int start=0;start<values.size();start+=200) {
            List<Map<String,Long>> chunk=values.subList(start,Math.min(start+200,values.size()));
            List<Map> replies=rest.exchangeList(Map.class,"http://apigateway:5555/api/inventory/inventories/pickable/quantity-capacity-batch-v1?warehouseId="+warehouse,HttpMethod.POST,chunk);
            if(replies==null||replies.size()!=chunk.size())throw bad("Capacity batch is incomplete");
            for(int n=0;n<chunk.size();n++) {
                Map v=replies.get(n);Map<String,Long> expected=chunk.get(n);
                if(!"quantity-capacity-v1".equals(v.get("schema"))||summaryNumber(v,"warehouseId")!=warehouse)throw bad("Capacity batch scope mismatch");
                for(String key:expected.keySet())if(summaryNumber(v,key)!=expected.get(key))throw bad("Capacity batch scope mismatch");
                long physical=summaryNumber(v,"physicalQuantity"),count=summaryNumber(v,"candidateCount"),eligible=summaryNumber(v,"eligibleCount");
                if(physical<0||count<0||count>=100000||eligible<0||eligible>count)throw bad("Capacity batch cannot be verified");
                result.put(expected.get("itemId")+":"+expected.get("inventoryStatusId")+":"+expected.get("clientId"),physical);
            }
        }
        // Same conservative open-Pick subtraction, aggregated once per request.
        Map<Long,Long> hard=new HashMap<>();
        if(!scopes.isEmpty()) {
            String ids=scopes.values().stream().map(v->v.get("itemId")).distinct().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            for(Map<String,Object> row:db.queryForList("SELECT item_id,SUM(quantity-picked_quantity) hard FROM pick WHERE warehouse_id=? AND item_id IN ("+ids+") AND quantity>0 AND picked_quantity<quantity GROUP BY item_id",warehouse))hard.put(number(row,"item_id"),number(row,"hard"));
        }
        for(Map.Entry<String,Map<String,Long>> scope:scopes.entrySet())result.compute(scope.getKey(),(k,v)->Math.max(0,v-hard.getOrDefault(scope.getValue().get("itemId"),0L)));
        return result;
    }
    private long capacity(long warehouse,long item,long status,long client) {
        long physical=physicalCapacity(warehouse,item,status,client);
        long hard=0;
        // Conservative: subtract every open Pick for the item, including LPN-bound Picks omitted above.
        // This can understate free stock; it cannot count a reservation twice as available.
        for(Pick pick:picks.getOpenPicksByItemId(item,false)) if(Objects.equals(pick.getWarehouseId(),warehouse)) hard=Math.addExact(hard,Math.max(0,pick.getQuantity()-pick.getPickedQuantity()));
        return Math.max(0,physical-hard);
    }
    private long physicalCapacity(long warehouse,long item,long status,long client) {
        if(summaryEnabled) {
            long start=System.nanoTime();
            try {
                Map<String,Object> value=rest.exchange(Map.class,"http://apigateway:5555/api/inventory/inventories/pickable/quantity-capacity-v1?warehouseId="+warehouse+"&itemId="+item+"&inventoryStatusId="+status+"&clientId="+client,HttpMethod.GET,null);
                if(value==null||!"quantity-capacity-v1".equals(value.get("schema"))||summaryNumber(value,"warehouseId")!=warehouse||summaryNumber(value,"itemId")!=item||summaryNumber(value,"inventoryStatusId")!=status||summaryNumber(value,"clientId")!=client)throw bad("Inventory capacity response does not match the reservation scope");
                long physical=summaryNumber(value,"physicalQuantity"),count=summaryNumber(value,"candidateCount"),eligible=summaryNumber(value,"eligibleCount");
                if(physical<0||count<0||count>=100000||eligible<0||eligible>count)throw bad("Inventory capacity response cannot be verified");
                org.slf4j.LoggerFactory.getLogger(QuantityAllocationService.class).info("Quantity capacity summary: warehouse={}, item={}, candidates={}, elapsedMs={}",warehouse,item,count,(System.nanoTime()-start)/1000000);
                return physical;
            } catch(org.springframework.web.client.HttpClientErrorException.NotFound missing) {
                org.slf4j.LoggerFactory.getLogger(QuantityAllocationService.class).warn("Quantity capacity summary endpoint unavailable; using original verified candidate read");
            }
        }
        // Explicit high cap with rejection on truncation; never silently promise a partial candidate set.
        String url="http://apigateway:5555/api/inventory/inventories/pickable?includeDetails=false&itemId="+item+"&inventoryStatusId="+status+"&lpnLimit=100000";
        List<Inventory> stock=rest.exchangeList(Inventory.class,url,HttpMethod.GET,null);
        if(stock.size()>=100000) throw bad("Too many inventory candidates; quantity allocation needs a summary endpoint for this item");
        long physical=0;
        for(Inventory row:stock) if(Objects.equals(row.getWarehouseId(),warehouse) && row.getItem()!=null && Objects.equals(row.getItem().getId(),item) && row.getInventoryStatus()!=null && Objects.equals(row.getInventoryStatus().getId(),status) && client(row.getClientId())==client && row.getPickId()==null && row.getAllocatedByPickId()==null && !Boolean.TRUE.equals(row.getVirtual())) physical=Math.addExact(physical,Math.max(0,row.getQuantity()));
        return physical;
    }
    private static long summaryNumber(Map<String,Object> value,String key) {
        Object number=value.get(key);
        if(number instanceof Byte||number instanceof Short||number instanceof Integer||number instanceof Long)return ((Number)number).longValue();
        if(number instanceof java.math.BigInteger)try{return ((java.math.BigInteger)number).longValueExact();}catch(ArithmeticException invalid){throw bad("Inventory capacity number overflow");}
        throw bad("Inventory capacity response is missing an integer field: "+key);
    }
    private long held(long warehouse,long item,long status,long client) {return db.queryForObject("SELECT COALESCE(SUM(held),0) FROM quantity_allocation_line WHERE warehouse_id=? AND item_id=? AND inventory_status_id=? AND client_id=?",Long.class,warehouse,item,status,client);}
    private Map<String,Object> ledger(long line) {List<Map<String,Object>> rows=db.queryForList("SELECT * FROM quantity_allocation_line WHERE order_line_id=?",line);if(rows.size()!=1)throw bad("Quantity allocation line was not found");return rows.get(0);}
    private QuantityBalance balance(Map<String,Object> row) {return new QuantityBalance(number(row,"demand"),number(row,"held"),number(row,"committed"));}
    private void save(long line,QuantityBalance value) {db.update("UPDATE quantity_allocation_line SET held=?,committed=?,revision=revision+1 WHERE order_line_id=?",value.held,value.committed,line);}
    private void unchanged(OrderLine line,Map<String,Object> row) {if(!Objects.equals(line.getExpectedQuantity(),number(row,"demand")) || !Objects.equals(line.getItemId(),number(row,"item_id")) || !Objects.equals(line.getInventoryStatusId(),number(row,"inventory_status_id")) || client(line.getClientId())!=number(row,"client_id") || hasRestrictions(line)) throw bad("Order line changed after quantity allocation was enabled. Supervisor reconciliation is required");}
    private void ensureEditable(Order order) {if(!enrolled(order.getId()) || (order.getStatus()!=OrderStatus.OPEN && order.getStatus()!=OrderStatus.INPROCESS) || Boolean.TRUE.equals(order.getCancelRequested()))throw bad("This order cannot change quantity allocation in its current state");}
    private void requireEnabled(){if(!enabled)throw bad("Quantity allocation is not activated on this server");}
    private static long client(Long value){return value==null?0:value;}
    private static long warehouseId(AllocationRequest request) {
        if(request.getWarehouse()!=null&&request.getWarehouse().getId()!=null)return request.getWarehouse().getId();
        if(request.getWorkOrder()!=null&&request.getWorkOrder().getWarehouseId()!=null)return request.getWorkOrder().getWarehouseId();
        if(!request.getShipmentLines().isEmpty()&&request.getShipmentLines().get(0).getWarehouseId()!=null)return request.getShipmentLines().get(0).getWarehouseId();
        throw bad("Allocation warehouse is missing. Refresh the order before allocating");
    }
    private static long number(Map<String,Object> row,String field){return ((Number)row.get(field)).longValue();}
    private static boolean hasRestrictions(OrderLine line){return Arrays.asList(line.getColor(),line.getProductSize(),line.getStyle(),line.getAllocateByReceiptNumber(),line.getInventoryAttribute1(),line.getInventoryAttribute2(),line.getInventoryAttribute3(),line.getInventoryAttribute4(),line.getInventoryAttribute5()).stream().anyMatch(value->value!=null&&!value.trim().isEmpty());}
    private static boolean accessible(Long client,ClientRestriction restriction){if(restriction==null || !Boolean.TRUE.equals(restriction.getThreePartyLogisticsFlag()) || Boolean.TRUE.equals(restriction.getAllClientAccess()))return true;if(client==null)return Boolean.TRUE.equals(restriction.getNonClientDataAccessible());return restriction.getClientAccesses()!=null&&Arrays.stream(restriction.getClientAccesses().split(",")).anyMatch(value->value.trim().equals(client.toString()));}
    private static void validate(Request request){try{if(request==null)throw new IllegalArgumentException();UUID.fromString(request.requestId);if(request.lines==null||request.lines.isEmpty()||request.lines.size()>500)throw new IllegalArgumentException();Set<Long> seen=new HashSet<>();for(Update line:request.lines)if(line==null||line.orderLineId==null||line.orderLineId<=0||!seen.add(line.orderLineId)||line.held==null||line.held<0||line.revision==null||line.revision<0)throw new IllegalArgumentException();}catch(Exception e){throw bad("Invalid quantity allocation request");}}
    private String fingerprint(Request request){try{List<Update> lines=new ArrayList<>(request.lines);lines.sort(Comparator.comparingLong(line->line.orderLineId));byte[] digest=MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(lines).getBytes(StandardCharsets.UTF_8));StringBuilder text=new StringBuilder();for(byte value:digest)text.append(String.format("%02x",value));return text.toString();}catch(Exception e){throw bad("Could not validate allocation request");}}
    private static String encode(String value){try{return java.net.URLEncoder.encode(value,"UTF-8");}catch(Exception e){throw bad("Invalid LPN");}}
    private static OrderOperationException bad(String message){return OrderOperationException.raiseException(message);}
}

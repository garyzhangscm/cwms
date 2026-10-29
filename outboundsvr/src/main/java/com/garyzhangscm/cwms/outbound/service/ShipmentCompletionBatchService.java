package com.garyzhangscm.cwms.outbound.service;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.transit.TransitShippingService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpMethod;
import javax.persistence.*;
import java.util.*;
@Service
public class ShipmentCompletionBatchService {
 @Value("${SHIPMENT_COMPLETION_BATCH_ENABLED:false}") private boolean enabled;
 @Autowired private RestTemplateProxy rest;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @Autowired private PickService picks;
 @Autowired private ShortAllocationService shorts;
 @Autowired private OrderLineService orderLines;
 @Autowired private ShipmentLineService shipmentLines;
 @Autowired private com.garyzhangscm.cwms.outbound.repository.ShipmentRepository shipments;
 @Autowired private BillableActivityService billing;
 @Autowired(required=false) private TransitShippingService transit;
 @PersistenceContext private EntityManager em;
 public boolean applies(Shipment s){return enabled&&s.getOrder()!=null&&(transit==null||!transit.applies(s.getWarehouseId(),s.getOrder().getShipToCustomerId()));}
 static long n(Object o){if(!(o instanceof Number))throw new IllegalArgumentException("Invalid inventory batch quantity or identifier");return ((Number)o).longValue();}
 static void require(boolean b,String m){if(!b)throw new IllegalArgumentException(m);}
 @SuppressWarnings("unchecked") static Map<String,Object> type(Map row){Map location=(Map)row.get("location");require(location!=null,"Inventory location missing");Map group=(Map)location.get("locationGroup");require(group!=null,"Inventory location group missing");Map t=(Map)group.get("locationGroupType");require(t!=null,"Inventory location type missing");return t;}
 @Transactional(rollbackFor=Exception.class)
 public void complete(Shipment shipment){
  long started=System.nanoTime();em.lock(shipment,LockModeType.PESSIMISTIC_WRITE);em.refresh(shipment);
  if(shipment.getStatus()==ShipmentStatus.DISPATCHED)return;
  require(shipment.getStatus()!=ShipmentStatus.CANCELLED,"Shipment is cancelled");
  List<Pick> work=picks.getPicksByShipment(shipment.getId());Map<Long,Pick> byPick=new HashMap<>();
  for(Pick p:work){require(p.getQuantity()!=null&&p.getPickedQuantity()!=null&&p.getQuantity().equals(p.getPickedQuantity()),"Shipment has unfinished Picks");byPick.put(p.getId(),p);}
  require(shorts.findByShipment(shipment).isEmpty(),"Shipment has short allocations");
  String url="http://apigateway:5555/api/inventory/shipment-inventory-batch/"+shipment.getId()+"?warehouseId="+shipment.getWarehouseId();
  List<Map> snapshot=rest.exchangeList(Map.class,url,HttpMethod.GET,null);require(snapshot!=null,"Inventory snapshot missing");
  List<Map<String,Object>> staged=new ArrayList<>();Set<Long> ids=new HashSet<>();
  Map<Long,Long> pickQuantities=new HashMap<>();
  Map<Long,Long> quantities=new HashMap<>();Map<Long,ShipmentLine> lines=new HashMap<>();
  for(Map row:snapshot){require(ids.add(n(row.get("id"))),"Duplicate inventory in snapshot");Map<String,Object> t=type(row);
   require(!Boolean.TRUE.equals(t.get("fourWallInventory"))||Boolean.TRUE.equals(t.get("shippingStage"))||Boolean.TRUE.equals(t.get("dock"))||Boolean.TRUE.equals(t.get("yard")),"Inventory has not been staged");
   if(!Boolean.TRUE.equals(t.get("shippingStage")))continue;
   long pickId=n(row.get("pickId")),quantity=n(row.get("quantity"));Pick p=byPick.get(pickId);require(p!=null&&quantity>0&&Objects.equals(p.getItemId(),n(row.get("itemId"))),"Inventory does not match shipment Pick");
   ShipmentLine line=p.getShipmentLine();require(line!=null&&line.getShipment()!=null&&Objects.equals(line.getShipment().getId(),shipment.getId()),"Pick belongs to a different shipment");
   pickQuantities.merge(pickId,quantity,Math::addExact);require(pickQuantities.get(pickId)<=p.getPickedQuantity(),"Inventory exceeds picked quantity");
   quantities.merge(line.getId(),quantity,Math::addExact);lines.put(line.getId(),line);staged.add(row);
  }
  Map<Long,Long> orderQuantities=new HashMap<>();Map<Long,OrderLine> orderMap=new HashMap<>();
  for(Long id:new TreeSet<>(lines.keySet())){ShipmentLine line=lines.get(id);em.lock(line,LockModeType.PESSIMISTIC_WRITE);em.refresh(line);long q=quantities.get(id);require(line.getInprocessQuantity()!=null&&line.getInprocessQuantity()>=q,"Shipment line quantity changed");OrderLine ol=line.getOrderLine();orderMap.put(ol.getId(),ol);orderQuantities.merge(ol.getId(),q,Math::addExact);}
  for(Long id:new TreeSet<>(orderMap.keySet())){OrderLine ol=orderMap.get(id);em.lock(ol,LockModeType.PESSIMISTIC_WRITE);em.refresh(ol);require(ol.getInprocessQuantity()!=null&&ol.getInprocessQuantity()>=orderQuantities.get(id),"Order remaining quantity changed");}
  long prepared=System.nanoTime();
  if(!staged.isEmpty()){
   Location destination=layout.createOrderLocation(shipment.getWarehouseId(),shipment.getOrder());
   Map<String,Object> request=new LinkedHashMap<>();request.put("destinationId",destination.getId());request.put("rows",staged);
   Map result=rest.exchange(Map.class,url,HttpMethod.POST,request);require(result!=null&&"shipment-inventory-batch-v1".equals(result.get("schema")),"Invalid batch response");
   List<Map> actual=(List<Map>)result.get("rows");require(actual!=null&&actual.size()==staged.size(),"Incomplete batch result");Map<Long,Map> submitted=new HashMap<>();for(Map r:staged)submitted.put(n(r.get("id")),r);
   for(Map r:actual){Map expected=submitted.remove(n(r.get("id")));require(expected!=null,"Unexpected batch inventory");for(String k:Arrays.asList("quantity","pickId","itemId","locationId"))require(n(expected.get(k))==n(r.get(k)),"Batch result quantity/scope mismatch");}require(submitted.isEmpty(),"Batch result missing inventory");
  }
  long shipped=System.nanoTime();
  for(Long id:new TreeSet<>(lines.keySet())){ShipmentLine line=lines.get(id);long q=quantities.get(id);line.setInprocessQuantity(Math.subtractExact(line.getInprocessQuantity(),q));line.setShippedQuantity(Math.addExact(line.getShippedQuantity(),q));line.setStatus(ShipmentLineStatus.DISPATCHED);shipmentLines.save(line);}
  for(Long id:new TreeSet<>(orderMap.keySet())){OrderLine line=orderMap.get(id);long q=orderQuantities.get(id);line.setInprocessQuantity(Math.subtractExact(line.getInprocessQuantity(),q));line.setShippedQuantity(Math.addExact(line.getShippedQuantity(),q));orderLines.saveOrUpdate(line);}
  // Preserve per-inventory billing/operation events, with original quantities.
  for(Map row:staged){OrderLine line=byPick.get(n(row.get("pickId"))).getShipmentLine().getOrderLine();billing.sendBillableActivity(line.getWarehouse()!=null?line.getWarehouse().getCompanyId():null,line.getWarehouseId(),line.getOrder().getClientId(),n(row.get("quantity")),line.getOrderNumber(),line.getItem()!=null?line.getItem().getName():null,BillableCategory.SHIPPING_CHARGE_BY_QUANTITY);}
  shipment.setStatus(ShipmentStatus.DISPATCHED);shipment.setCompleteTime(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));shipments.save(shipment);layout.releaseLocations(shipment.getWarehouseId(),shipment);
  org.slf4j.LoggerFactory.getLogger(getClass()).info("shipment-complete-batch shipment={} inventoryRows={} shipmentLines={} prepareMs={} shipMs={} finalizeMs={}",shipment.getId(),staged.size(),lines.size(),(prepared-started)/1000000,(shipped-prepared)/1000000,(System.nanoTime()-shipped)/1000000);
 }
}

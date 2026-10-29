import com.garyzhangscm.cwms.outbound.quantity.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import javax.persistence.EntityManager;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual production candidate service against transactional H2/MySQL syntax, no business APIs. */
public class QuantityAllocationChecks {
 static int passed;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;}
 static void rejects(Runnable action,String name){try{action.run();throw new AssertionError("Allowed: "+name);}catch(AssertionError e){throw e;}catch(RuntimeException expected){passed++;}}
 static void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 static class Orders extends OrderService {Map<Long,Order> rows=new ConcurrentHashMap<>();@Override public Order findById(Long id){return rows.get(id);}}
 static class Picks extends PickService {List<Pick> rows=new CopyOnWriteArrayList<>();int created;
  @Override public List<Pick> getOpenPicksByItemId(Long id,boolean details){return new ArrayList<>(rows);}
  @Override public Pick generatePick(ShipmentLine line,Inventory stock,Long quantity,ItemUnitOfMeasure unit,boolean whole){created++;Pick pick=new Pick();pick.setWarehouseId(1L);pick.setItemId(stock.getItem().getId());pick.setQuantity(quantity);pick.setPickedQuantity(0L);pick.setShipmentLine(line);return pick;}
 }
 static class Users extends UserService {@Override public String getCurrentUserName(){return "regression";}}
 static class Rest extends RestTemplateProxy {
  List<Inventory> rows=new CopyOnWriteArrayList<>();AtomicInteger writes=new AtomicInteger();AtomicInteger summaries=new AtomicInteger();AtomicInteger legacyCapacity=new AtomicInteger();String malformed="";boolean missingSummary=false;Map<String,Object> summaryOverride=new HashMap<>();
  @Override public <T> List<T> exchangeList(Class<T> type,String uri,org.springframework.http.HttpMethod method,Object payload){
   if(method!=org.springframework.http.HttpMethod.GET){writes.incrementAndGet();throw new AssertionError("Unexpected business write");}
   if(uri.contains("/inventories/pickable?"))legacyCapacity.incrementAndGet();
   return (List<T>)(List<?>)new ArrayList<>(rows);
  }
  @Override public <T>T exchange(Class<T> type,String uri,org.springframework.http.HttpMethod method,Object payload){
   if(method!=org.springframework.http.HttpMethod.GET)throw new AssertionError("Summary must be read only");
   summaries.incrementAndGet();if(missingSummary)throw org.springframework.web.client.HttpClientErrorException.create(org.springframework.http.HttpStatus.NOT_FOUND,"missing",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null);org.springframework.web.util.UriComponents parts=org.springframework.web.util.UriComponentsBuilder.fromUriString(uri).build();
   long w=Long.parseLong(parts.getQueryParams().getFirst("warehouseId")),i=Long.parseLong(parts.getQueryParams().getFirst("itemId")),s=Long.parseLong(parts.getQueryParams().getFirst("inventoryStatusId")),c=Long.parseLong(parts.getQueryParams().getFirst("clientId"));
   long physical=0;int eligible=0;
   for(Inventory row:rows)if(Objects.equals(row.getWarehouseId(),w)&&row.getItem()!=null&&Objects.equals(row.getItem().getId(),i)&&row.getInventoryStatus()!=null&&Objects.equals(row.getInventoryStatus().getId(),s)&&(row.getClientId()==null?0:row.getClientId())==c&&row.getPickId()==null&&row.getAllocatedByPickId()==null&&!Boolean.TRUE.equals(row.getVirtual())){physical=Math.addExact(physical,Math.max(0,row.getQuantity()));eligible++;}
   Map<String,Object> response=new HashMap<>(Map.of("schema","quantity-capacity-v1","warehouseId",w,"itemId",i,"inventoryStatusId",s,"clientId",c,"physicalQuantity",physical,"candidateCount",rows.size(),"eligibleCount",eligible));
   if(!malformed.isEmpty())response.put(malformed,"invalid");
   response.putAll(summaryOverride);return (T)response;
  }

 }
 static Order order(long id,long lineId,long itemId,long demand){
  Order order=new Order();order.setId(id);order.setNumber("TEST-"+id);order.setWarehouseId(1L);order.setCategory(OrderCategory.SALES_ORDER);order.setStatus(OrderStatus.OPEN);order.setAllowForManualPick(false);
  OrderLine line=new OrderLine();line.setId(lineId);line.setNumber("1");line.setOrder(order);line.setWarehouseId(1L);line.setItemId(itemId);line.setExpectedQuantity(demand);line.setInventoryStatusId(1L);
  Item item=new Item();item.setId(itemId);item.setName("TEST-ITEM-"+itemId);line.setItem(item);order.setOrderLines(new ArrayList<>(List.of(line)));return order;
 }
 static Inventory stock(long itemId,long quantity,String lpn){Inventory row=new Inventory();row.setId(itemId);row.setLpn(lpn);row.setWarehouseId(1L);row.setLocationId(100L);row.setQuantity(quantity);Item item=new Item();item.setId(itemId);row.setItem(item);InventoryStatus status=new InventoryStatus();status.setId(1L);row.setInventoryStatus(status);return row;}
 static QuantityAllocationService.Request request(long line,long held,long revision){QuantityAllocationService.Request r=new QuantityAllocationService.Request();r.requestId=UUID.randomUUID().toString();QuantityAllocationService.Update u=new QuantityAllocationService.Update();u.orderLineId=line;u.held=held;u.revision=revision;r.lines=List.of(u);return r;}
 static Map row(Map<String,Object> snapshot){return (Map)((List)snapshot.get("lines")).get(0);}
 static long value(Map row,String key){return ((Number)row.get(key)).longValue();}
 static AllocationRequest allocation(Order order,long quantity,boolean manual){AllocationRequest request=new AllocationRequest();Warehouse warehouse=new Warehouse();warehouse.setId(1L);request.setWarehouse(warehouse);request.setItem(order.getOrderLines().get(0).getItem());InventoryStatus status=new InventoryStatus();status.setId(1L);request.setInventoryStatus(status);request.setQuantity(quantity);request.setManualAllocation(manual);request.setLpn(manual?"LPN-A":"");ShipmentLine shipment=new ShipmentLine();shipment.setOrderLine(order.getOrderLines().get(0));request.setShipmentLines(List.of(shipment));return request;}
 static AllocationResult result(AllocationRequest request,long quantity){AllocationResult result=new AllocationResult();Pick pick=new Pick();pick.setId(88L);pick.setWarehouseId(1L);pick.setItemId(request.getItem().getId());pick.setQuantity(quantity);pick.setPickedQuantity(0L);pick.setShipmentLine(request.getShipmentLines().get(0));result.addPick(pick);return result;}
 public static class Strategy implements AllocationStrategy {
  PickService picks;int called;
  public AllocationStrategyType getType(){return AllocationStrategyType.DEFAULT;}
  public boolean isDefault(){return true;}
  public AllocationResult allocate(AllocationRequest request){return allocate(request,null);}
  public AllocationResult allocate(AllocationRequest request,Location location){called++;AllocationResult result=new AllocationResult();result.addPick(picks.generatePick(request.getShipmentLines().get(0),stock(request.getItem().getId(),100,"LPN-A"),request.getQuantity(),null,false));return result;}
 }
 static <T>T proxy(T target,QuantityAllocationAspect aspect){AspectJProxyFactory factory=new AspectJProxyFactory(target);factory.setProxyTargetClass(true);factory.addAspect(aspect);return (T)factory.getProxy();}
 public static void main(String[] args)throws Exception {
  QuantityBalance b=new QuantityBalance(1000,0,0).target(1000,500);check(b.held==500&&b.shortage()==500,"partial reservation");
  b=b.convert(300);check(b.held==200&&b.committed==300,"conversion, no double count");b=b.cancelUnpicked(100);check(b.held==300&&b.committed==200,"cancel returns to soft hold");b=b.target(0,0);check(b.held==0&&b.committed==200,"release preserves committed");
  rejects(()->new QuantityBalance(10,11,0),"overcommitted invariant");rejects(()->new QuantityBalance(10,5,0).convert(6),"overpick");rejects(()->new QuantityBalance(10,5,0).convert(0),"zero pick");rejects(()->new QuantityBalance(10,5,0).cancelUnpicked(1),"cancel more than committed");
  JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:quantity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
  JdbcTemplate db=new JdbcTemplate(source);db.execute("CREATE TABLE warehouse(warehouse_id BIGINT PRIMARY KEY)");db.update("INSERT INTO warehouse VALUES (1)");db.execute("CREATE TABLE outbound_order_line(outbound_order_line_id BIGINT PRIMARY KEY,outbound_order_id BIGINT)");db.execute("CREATE TABLE shipment_line(outbound_order_line_id BIGINT)");
  for(String statement:Files.readString(Path.of(args[0])).replaceAll("(?m)^--.*$","").split(";"))if(!statement.trim().isEmpty())db.execute(statement);
  Orders orders=new Orders();Picks picks=new Picks();Rest rest=new Rest();rest.rows.add(stock(9,100,"LPN-A"));orders.rows.put(1L,order(1,11,9,100));orders.rows.put(2L,order(2,22,9,100));
  QuantityAllocationService service=new QuantityAllocationService(source,new DataSourceTransactionManager(source));field(service,"enabled",true);field(service,"summaryEnabled",true);field(service,"orders",orders);field(service,"picks",picks);field(service,"rest",rest);field(service,"users",new Users());
  EntityManager em=(EntityManager)Proxy.newProxyInstance(QuantityAllocationChecks.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{if(m.getName().equals("find"))return orders.rows.get(a[1]);if(List.of("refresh","flush","close").contains(m.getName()))return null;if(m.getName().equals("isOpen"))return true;throw new AssertionError(m.getName());});field(service,"em",em);
  check(!service.defaultFor(1),"default disabled without warehouse config");
  field(service,"defaultWarehouses"," 1, 7 ");check(service.defaultFor(1)&&service.defaultFor(7)&&!service.defaultFor(2),"warehouse scoped default");
  orders.rows.put(1003L,order(1003,33,9,100));
  check("ITEM_QUANTITY".equals(service.entry(1,1003,null).get("mode")),"Allocate entry automatically enrolls fresh order");
  check(((Number)row(service.view(1,1003,null)).get("held")).longValue()==0,"opening Allocate does not reserve stock");
  check("ITEM_QUANTITY".equals(service.entry(1,1003,null).get("mode")),"repeated Allocate entry is idempotent");
  rejects(()->service.requireClassicAllocationAllowed(1003),"enrolled order cannot use old automatic endpoint");
  orders.rows.put(1004L,order(1004,44,9,100));
  db.update("INSERT INTO outbound_order_line VALUES(44,1004)");db.update("INSERT INTO shipment_line VALUES(44)");
  check("CLASSIC".equals(service.entry(1,1004,null).get("mode"))&&!service.enrolled(1004),"existing shipment stays classic");
  service.requireClassicAllocationAllowed(1004);check(true,"existing shipment automatic allocation retained");
  orders.rows.put(1007L,order(1007,77,9,100));orders.rows.get(1007L).setCategory(OrderCategory.WAREHOUSE_TRANSFER_ORDER);
  check("CLASSIC".equals(service.entry(1,1007,null).get("mode")),"warehouse transfer retains classic mode");
  orders.rows.put(1005L,order(1005,55,9,100));orders.rows.get(1005L).getOrderLines().get(0).setAllocateByReceiptNumber("RESTRICTED");
  rejects(()->service.entry(1,1005,null),"unsupported new order does not silently fall back");check(!service.enrolled(1005),"unsupported entry rolled back");
  rejects(()->service.requireClassicAllocationAllowed(1005),"old API cannot bypass default for fresh orders");
  rejects(()->service.entry(2,1003,null),"wrong warehouse entry rejected");
  field(service,"defaultWarehouses","");orders.rows.put(1006L,order(1006,66,9,100));
  check("CLASSIC".equals(service.entry(1,1006,null).get("mode")),"unconfigured warehouse retains classic mode");
  Method capacityRead=QuantityAllocationService.class.getDeclaredMethod("physicalCapacity",long.class,long.class,long.class,long.class);capacityRead.setAccessible(true);
  java.util.function.Supplier<Long> readCapacity=()->{try{return (Long)capacityRead.invoke(service,1L,9L,1L,0L);}catch(InvocationTargetException failure){throw new RuntimeException(failure.getCause());}catch(Exception failure){throw new RuntimeException(failure);}};
  for(String key:List.of("schema","warehouseId","itemId","inventoryStatusId","clientId","physicalQuantity","candidateCount","eligibleCount")){rest.malformed=key;rejects(()->readCapacity.get(),"malformed summary "+key);}rest.malformed="";
  for(Map<String,Object> invalid:List.<Map<String,Object>>of(Map.of("warehouseId",2L),Map.of("physicalQuantity",-1L),Map.of("physicalQuantity",1.5),Map.of("candidateCount",100000),Map.of("eligibleCount",999999),Map.of("physicalQuantity",java.math.BigInteger.ONE.shiftLeft(80)))){rest.summaryOverride.putAll(invalid);rejects(()->readCapacity.get(),"summary validation fails closed");rest.summaryOverride.clear();}
  rest.missingSummary=true;check(readCapacity.get()==100,"404 falls back to original verified read");rest.missingSummary=false;
  field(service,"summaryEnabled",false);check(readCapacity.get()==100,"disabled summary keeps original behavior");field(service,"summaryEnabled",true);
  check("CLASSIC".equals(service.view(1,1,null).get("mode")),"classic default");service.enable(1,1,null);service.enable(1,2,null);check(Boolean.TRUE.equals(orders.rows.get(1L).getAllowForManualPick()),"enable manual scanning");
  check(!row(service.view(1,1,null)).containsKey("lpn")&&!row(service.view(1,1,null)).containsKey("sourceLocationId"),"reservation contains no LPN or source");
  ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);List<Future<?>> futures=new ArrayList<>();for(long id:List.of(1L,2L)){long key=id;futures.add(workers.submit(()->{try{start.await();service.update(1,key,null,request(key==1?11:22,70,0));}catch(Exception e){throw new RuntimeException(e);}}));}start.countDown();for(Future<?> f:futures)f.get();workers.shutdown();
  long first=value(row(service.view(1,1,null)),"held"),second=value(row(service.view(1,2,null)),"held");check(first+second==100,"concurrent orders cannot reserve more than inventory");check(Math.min(first,second)==30&&Math.max(first,second)==70,"partial concurrent reservation");
  rejects(()->service.update(1,1,null,request(11,90,0)),"stale revision");rejects(()->service.view(2,1,null),"cross warehouse");rejects(()->service.update(1,1,null,request(22,10,1)),"cross-order line");
  QuantityAllocationService.Request release=request(11,0,1);Map<String,Object> released=service.update(1,1,null,release);Map<String,Object> replay=service.update(1,1,null,release);check(value(row(released),"revision")==value(row(replay),"revision"),"same request does not mutate twice");release.lines.get(0).held=1L;rejects(()->service.update(1,1,null,release),"request ID payload mismatch");
  QuantityAllocationService.Request reserve=request(11,40,2);service.update(1,1,null,reserve);long held=value(row(service.view(1,1,null)),"held");check(held>0,"reserve after release");
  AllocationRequest scan=allocation(orders.rows.get(1L),held,true);long before=value(row(service.view(1,1,null)),"held");service.scanning(()->service.allocate(scan,()->result(scan,scan.getQuantity())));Map after=row(service.view(1,1,null));check(value(after,"held")==0&&value(after,"committed")==before,"atomic soft to Pick conversion");
  rejects(()->service.scanning(()->service.allocate(scan,()->result(scan,scan.getQuantity()))),"repeated pick cannot consume released hold");
  rejects(()->service.allocate(allocation(orders.rows.get(2L),1,false),()->new AllocationResult()),"enrolled order cannot use old automatic path");
  Pick cancelled=result(scan,before).getPicks().get(0);service.locked(1,()->{service.restoreCancelled(cancelled,before);return null;});check(value(row(service.view(1,1,null)),"held")==before,"cancelled Pick restores hold");
  Order legacy=order(3,33,9,100);AllocationRequest classic=allocation(legacy,100,false);AtomicInteger called=new AtomicInteger();rejects(()->service.allocate(classic,()->{called.incrementAndGet();return new AllocationResult();}),"legacy cannot take promised quantity");check(called.get()==0,"legacy rejected before business writes");
  Map snap=row(service.view(1,1,null));long revision=value(snap,"revision"),heldBefore=value(snap,"held");AllocationRequest bad=allocation(orders.rows.get(1L),1,true);rejects(()->service.scanning(()->service.allocate(bad,()->result(bad,0))),"partial failed preparation");check(value(row(service.view(1,1,null)),"held")==heldBefore&&value(row(service.view(1,1,null)),"revision")==revision,"failed conversion rolls back reservation");
  rest.rows.get(0).setItem(new Item());rejects(()->service.scanning(()->service.allocate(bad,()->result(bad,1))),"wrong item LPN");rest.rows.clear();rest.rows.add(stock(9,100,"LPN-A"));rest.rows.add(stock(9,5,"LPN-A"));rejects(()->service.scanning(()->service.allocate(bad,()->result(bad,1))),"mixed LPN rejected");rest.rows.remove(1);rest.rows.get(0).setWarehouseId(2L);rejects(()->service.scanning(()->service.allocate(bad,()->result(bad,1))),"wrong warehouse LPN");rest.rows.get(0).setWarehouseId(1L);
  orders.rows.put(4L,order(4,44,9,100));orders.rows.get(4L).getOrderLines().get(0).setColor("RED");rejects(()->service.enable(1,4,null),"attribute restrictions not silently dropped");orders.rows.put(5L,order(5,55,9,100));db.update("INSERT INTO outbound_order_line VALUES (55,5)");db.update("INSERT INTO shipment_line VALUES (55)");rejects(()->service.enable(1,5,null),"existing shipment not migrated");
  rejects(()->service.requireNoHolds(1),"cannot strand hold on terminal operation");
  // Execute the actual aspect pointcuts with real Spring AOP proxies, including nested Pick creation.
  for(long id:List.of(1L,2L)){Map current=row(service.view(1,id,null));service.update(1,id,null,request(id==1?11:22,0,value(current,"revision")));}
  orders.rows.put(6L,order(6,66,9,100));service.enable(1,6,null);service.update(1,6,null,request(66,20,0));
  QuantityAllocationAspect aspect=new QuantityAllocationAspect();field(aspect,"quantities",service);field(aspect,"orders",orders);field(aspect,"picks",picks);
  Picks proxiedPicks=proxy(picks,aspect);Strategy strategy=new Strategy();strategy.picks=proxiedPicks;Strategy proxiedStrategy=proxy(strategy,aspect);
  AllocationRequest classicHuge=allocation(legacy,90,false);int callsBefore=strategy.called;rejects(()->proxiedStrategy.allocate(classicHuge),"strategy AOP protects classic request");check(strategy.called==callsBefore,"strategy rejection before creation");
  AllocationRequest own=allocation(orders.rows.get(6L),10,true);service.scanning(()->proxiedStrategy.allocate(own));check(value(row(service.view(1,6,null)),"held")==10&&value(row(service.view(1,6,null)),"committed")==10,"real AOP converts once across nested strategy and Pick calls");
  int createsBefore=picks.created;rejects(()->proxiedPicks.generatePick(own.getShipmentLines().get(0),stock(9,100,"LPN-A"),1L,null,false),"direct Pick creation cannot bypass own ledger");check(picks.created==createsBefore,"direct bypass rejected before Pick is made");
  rejects(()->proxiedPicks.generatePick(classicHuge.getShipmentLines().get(0),stock(9,100,"LPN-A"),95L,null,false),"configuration path cannot take reserved stock");
  service.checkDirectPick(1,9,1,0,80,3L);check(true,"classic allocation can still use genuinely unreserved stock");
  AllocationRequest thin=allocation(legacy,1,false);thin.setWarehouse(null);thin.getShipmentLines().get(0).setWarehouseId(1L);service.allocate(thin,AllocationResult::new);check(true,"thin shipment DTO resolves scalar warehouse ID");
  WorkOrder work=new WorkOrder();work.setWarehouseId(1L);thin.setShipmentLines(List.of());thin.setWorkOrder(work);service.allocate(thin,AllocationResult::new);check(true,"thin work-order DTO resolves scalar warehouse ID");
  thin.setWorkOrder(null);rejects(()->service.allocate(thin,AllocationResult::new),"missing warehouse returns a business error before stock work");
  field(service,"enabled",false);proxiedStrategy.allocate(classicHuge);check(strategy.called>callsBefore,"feature disabled preserves original path");field(service,"enabled",true);
  check(rest.writes.get()==0,"regression never writes production APIs");
  check(rest.summaries.get()>0&&rest.legacyCapacity.get()==2,"fast capacity avoids legacy candidate transfer");
  System.out.println("PASS "+passed+" quantity allocation checks, real JDBC transactions, concurrent reservations, no business network");
 }
}

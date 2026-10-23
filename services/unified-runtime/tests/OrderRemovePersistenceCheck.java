import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import javax.persistence.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Real deployed Hibernate mappings on isolated H2; no production or remote calls. */
public class OrderRemovePersistenceCheck {
 static EntityManager em; static int cases;
 static void inject(Object o,String n,Object v)throws Exception {Class<?> c=o.getClass();while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(o,v);return;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new AssertionError(n);}
 static class Picks extends PickService {boolean blocked;public List<Pick> findByOrder(Order o){return blocked?Arrays.asList(new Pick()):Collections.emptyList();}}
 static class Shorts extends ShortAllocationService {boolean blocked;public List<ShortAllocation> findByOrder(Order o){return blocked?Arrays.asList(new ShortAllocation()):Collections.emptyList();}}
 static class Shipments extends ShipmentService {int removed;int failAt;public void removeShipment(Long id){removed++;if(removed==failAt)throw new IllegalStateException("fixture deletion failure");em.remove(em.find(Shipment.class,id));em.flush();}}
 static class Orders extends OrderService {
  public Order findById(Long id,boolean details){return em.find(Order.class,id);}
  public Order findById(Long id){return em.find(Order.class,id);}
  public Order changeAssignedStageLocations(Order o,Long group,Long loc){return em.merge(o);}
 }
 static class Fixture {Long id;List<Long> shipments=new ArrayList<>();}
 static Fixture fixture(int count){
  Fixture f=new Fixture();Order o=new Order();o.setNumber("REMOVE-"+UUID.randomUUID());o.setWarehouseId(1L);o.setStatus(OrderStatus.OPEN);em.persist(o);
  OrderLine line=new OrderLine();line.setOrder(o);line.setNumber("1");line.setWarehouseId(1L);line.setItemId(1L);line.setExpectedQuantity(5L);line.setOpenQuantity(count==0?5L:0L);line.setInprocessQuantity(5L*count);line.setShippedQuantity(0L);o.getOrderLines().add(line);em.persist(line);
  for(int i=0;i<count;i++){Shipment s=new Shipment();s.setNumber("PLAN-"+UUID.randomUUID());s.setWarehouseId(1L);s.setStatus(ShipmentStatus.INPROCESS);em.persist(s);ShipmentLine sl=new ShipmentLine();sl.setShipment(s);sl.setOrderLine(line);sl.setWarehouseId(1L);sl.setNumber("1");sl.setQuantity(5L);sl.setOpenQuantity(5L);sl.setInprocessQuantity(0L);sl.setShippedQuantity(0L);sl.setLoadedQuantity(0L);em.persist(sl);s.getShipmentLines().add(sl);line.getShipmentLines().add(sl);f.shipments.add(s.getId());}
  f.id=o.getId();em.flush();em.clear();return f;
 }
 static Orders service(Picks p,Shorts a,Shipments s)throws Exception {
  Orders o=new Orders();SimpleJpaRepository<Order,Long> repo=new SimpleJpaRepository<>(Order.class,em);
  OrderRepository proxy=(OrderRepository)Proxy.newProxyInstance(OrderRepository.class.getClassLoader(),new Class[]{OrderRepository.class},(x,m,args)->{try{return repo.getClass().getMethod(m.getName(),m.getParameterTypes()).invoke(repo,args);}catch(InvocationTargetException e){throw e.getCause();}});
  inject(o,"orderRepository",proxy);inject(o,"manualPickEntityManager",em);inject(o,"pickService",p);inject(o,"shortAllocationService",a);inject(o,"shipmentService",s);return o;
 }
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static void run(String mode)throws Exception {
  em.getTransaction().begin();Fixture f=fixture(mode.equals("empty")?0:2);em.getTransaction().commit();
  em.getTransaction().begin();Picks p=new Picks();Shorts a=new Shorts();Shipments s=new Shipments();Orders o=service(p,a,s);Order order=em.find(Order.class,f.id);
  if(mode.equals("pick"))p.blocked=true;
  if(mode.equals("short"))a.blocked=true;
  if(mode.equals("complete"))order.setStatus(OrderStatus.COMPLETE);
  if(mode.equals("allocating"))order.setStatus(OrderStatus.ALLOCATING);
  if(mode.equals("shipped"))order.getOrderLines().get(0).setShippedQuantity(1L);
  if(mode.equals("dispatched"))em.find(Shipment.class,f.shipments.get(0)).setStatus(ShipmentStatus.DISPATCHED);
  if(mode.equals("line-shipped"))order.getOrderLines().get(0).getShipmentLines().get(0).setShippedQuantity(1L);
  if(mode.equals("shared")){Order other=new Order();other.setNumber("OTHER");other.setWarehouseId(1L);other.setStatus(OrderStatus.OPEN);em.persist(other);OrderLine ol=new OrderLine();ol.setOrder(other);ol.setExpectedQuantity(1L);ol.setOpenQuantity(1L);ol.setShippedQuantity(0L);other.getOrderLines().add(ol);em.persist(ol);Shipment ship=em.find(Shipment.class,f.shipments.get(0));ShipmentLine sl=new ShipmentLine();sl.setShipment(ship);sl.setOrderLine(ol);sl.setQuantity(1L);em.persist(sl);ship.getShipmentLines().add(sl);ol.getShipmentLines().add(sl);}
  if(mode.equals("rollback"))s.failAt=2;
  boolean success=mode.equals("empty")||mode.equals("duplicate");Throwable failure=null;
  try{o.removeOrder(f.id);em.flush();}catch(Throwable e){failure=e;}
  if(success){if(failure!=null)throw new AssertionError(mode,failure);em.getTransaction().commit();em.clear();check(em.find(Order.class,f.id)==null,"Order survived");for(Long id:f.shipments)check(em.find(Shipment.class,id)==null,"Shipment survived");}
  else{check(failure!=null,"Guard did not reject "+mode);if(!mode.equals("rollback"))check(s.removed==0,"Guard ran after deletion");em.getTransaction().rollback();em.clear();check(em.find(Order.class,f.id)!=null,"Rollback lost order");for(Long id:f.shipments)check(em.find(Shipment.class,id)!=null,"Rollback lost shipment");}
  cases++;System.out.println("RESULT "+mode+" passed");em.clear();
 }
 public static void main(String[] args)throws Exception {
  Configuration c=new Configuration().setProperty("hibernate.dialect","org.hibernate.dialect.H2Dialect").setProperty("hibernate.connection.driver_class","org.h2.Driver").setProperty("hibernate.connection.url","jdbc:h2:mem:remove;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE,KEY,USER").setProperty("hibernate.hbm2ddl.auto","create-drop").setProperty("hibernate.show_sql","false");
  Path root=Paths.get(args[0]);try(java.util.stream.Stream<Path> paths=Files.walk(root.resolve("com/garyzhangscm/cwms/outbound/model"))){paths.filter(p->p.toString().endsWith(".class")).forEach(p->{try{Class<?> type=Class.forName(root.relativize(p).toString().replace('/','.').replace(".class",""),false,OrderRemovePersistenceCheck.class.getClassLoader());if(type.isAnnotationPresent(Entity.class))c.addAnnotatedClass(type);}catch(Exception e){throw new RuntimeException(e);}});}
  try(SessionFactory sf=c.buildSessionFactory()){em=sf.createEntityManager();for(String mode:args.length>1?new String[]{args[1]}:new String[]{"empty","duplicate","pick","short","complete","allocating","shipped","dispatched","line-shipped","shared","rollback"})run(mode);em.close();}
  System.out.println("RESULT "+cases+" real Hibernate removal cases passed; production writes=0");
 }
}

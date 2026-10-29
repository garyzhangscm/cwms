import com.garyzhangscm.cwms.inventory.controller.QuantityCapacityController;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.*;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.*;import java.util.*;
public class ScopedCapacityChecks {
 static int checks;static void check(boolean x,String m){if(!x)throw new AssertionError(m);checks++;}
 static void field(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static class Layout extends WarehouseLayoutServiceRestemplateClient {Map<Long,Location> rows=new HashMap<>();int calls;public Location getLocationById(Long id){calls++;return rows.get(id);}}
 static Location location(long id,boolean enabled,boolean pickable,boolean wall){Location x=new Location();x.setId(id);Warehouse w=new Warehouse();w.setId(1L);x.setWarehouse(w);x.setEnabled(enabled);LocationGroup g=new LocationGroup();g.setPickable(pickable);LocationGroupType t=new LocationGroupType();t.setFourWallInventory(wall);g.setLocationGroupType(t);x.setLocationGroup(g);return x;}
 static boolean zero(Boolean x){return Boolean.FALSE.equals(x);}
 static void reject(Runnable r,String m){try{r.run();throw new AssertionError(m);}catch(AssertionError e){throw e;}catch(RuntimeException e){checks++;}}
 public static void main(String[] args)throws Exception{
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:scoped;MODE=MySQL;DB_CLOSE_DELAY=-1");JdbcTemplate db=new JdbcTemplate(ds);
  for(String sql:List.of("CREATE TABLE item(item_id BIGINT PRIMARY KEY)","CREATE TABLE inventory_status(inventory_status_id BIGINT PRIMARY KEY)","CREATE TABLE inventory(inventory_id BIGINT PRIMARY KEY,warehouse_id BIGINT,item_id BIGINT,inventory_status_id BIGINT,client_id BIGINT,location_id BIGINT,quantity BIGINT,pick_id BIGINT,allocated_by_pick_id BIGINT,virtual_inventory BOOLEAN,inbound_qc_required BOOLEAN,locked_for_adjust BOOLEAN)","CREATE TABLE inventory_lock(inventory_lock_id BIGINT PRIMARY KEY,allow_pick BOOLEAN)","CREATE TABLE inventory_with_lock(inventory_id BIGINT,inventory_lock_id BIGINT)"))db.execute(sql);
  db.update("INSERT INTO item VALUES(9),(10)");db.update("INSERT INTO inventory_status VALUES(1),(2)");db.update("INSERT INTO inventory_lock VALUES(1,TRUE),(2,FALSE),(3,NULL)");
  Layout layout=new Layout();layout.rows.put(1L,location(1,true,true,true));layout.rows.put(2L,location(2,false,true,true));layout.rows.put(3L,location(3,true,false,true));layout.rows.put(4L,location(4,true,true,false));
  QuantityCapacityController c=new QuantityCapacityController(null);field(c,"source",ds);field(c,"layout",layout);field(c,"scopedSummary",true);
  Random random=new Random(4087);long[] expected=new long[2];int[] counts=new int[2];
  for(long id=1;id<=1500;id++){
   long w=random.nextInt(5)==0?2:1,item=random.nextInt(4)==0?10:9,status=random.nextInt(4)==0?2:1,loc=1+random.nextInt(4),qty=random.nextInt(200)-20;Long client=random.nextBoolean()?null:4L,pick=random.nextInt(5)==0?7L:null,allocated=random.nextInt(5)==0?8L:null;Boolean v=random.nextInt(9)==0?null:random.nextInt(5)==0,qc=random.nextInt(9)==0?null:random.nextInt(5)==0,adjust=random.nextInt(9)==0?null:random.nextInt(5)==0;int lock=random.nextInt(4);
   db.update("INSERT INTO inventory VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,w,item,status,client,loc,qty,pick,allocated,v,qc,adjust);if(lock>0)db.update("INSERT INTO inventory_with_lock VALUES(?,?)",id,lock);
   if(w==1&&item==9&&status==1&&loc==1&&pick==null&&allocated==null&&zero(v)&&zero(qc)&&zero(adjust)&&lock!=1){int at=client==null?0:1;expected[at]+=Math.max(0,qty);counts[at]++;}
  }
  for(int k=0;k<2;k++){layout.calls=0;Map<String,Object> r=c.capacity(1L,9L,1L,k==0?0L:4L,null);check(((Number)r.get("physicalQuantity")).longValue()==expected[k],"legacy eligibility/owner arithmetic");check(((Number)r.get("eligibleCount")).longValue()==counts[k],"eligible rows");check(layout.calls<=4,"one location read per distinct location");check(r.size()==8,"scalar contract");}
  check(((Number)c.capacity(1L,10L,2L,99L,null).get("physicalQuantity")).longValue()==0,"empty owner");
  reject(()->{try{c.capacity(1L,9L,1L,0L,new ClientRestriction(true,false,false,"4"));}catch(Exception e){throw new RuntimeException(e);}},"client ACL");
  db.update("DELETE FROM inventory_with_lock");db.update("DELETE FROM inventory");
  db.update("INSERT INTO inventory VALUES(1,1,9,1,NULL,1,NULL,NULL,NULL,FALSE,FALSE,FALSE)");reject(()->{try{c.capacity(1L,9L,1L,0L,null);}catch(Exception e){throw new RuntimeException(e);}},"null quantity");
  db.update("UPDATE inventory SET quantity=5,location_id=99");reject(()->{try{c.capacity(1L,9L,1L,0L,null);}catch(Exception e){throw new RuntimeException(e);}},"missing location");
  db.update("UPDATE inventory SET location_id=1,quantity=?",Long.MAX_VALUE);db.update("INSERT INTO inventory VALUES(2,1,9,1,NULL,1,1,NULL,NULL,FALSE,FALSE,FALSE)");reject(()->{try{c.capacity(1L,9L,1L,0L,null);}catch(Exception e){throw new RuntimeException(e);}},"overflow");
  System.out.println("PASS "+checks+" scoped SQL checks across 1500 randomized rows: scopes, QC, locks, virtual, locations, null, overflow, ACL and bounded payload");
 }
}

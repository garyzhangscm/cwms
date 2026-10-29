import com.garyzhangscm.cwms.outbound.quantity.*;
import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import org.h2.jdbcx.JdbcDataSource;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.datasource.DataSourceTransactionManager;import java.util.*;import java.lang.reflect.*;
public class BatchAllocationChecks {
 public static void main(String[] args)throws Exception{
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:batch;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=PICK;DB_CLOSE_DELAY=-1");JdbcTemplate db=new JdbcTemplate(ds);db.execute("CREATE TABLE pick(item_id BIGINT,warehouse_id BIGINT,quantity BIGINT,picked_quantity BIGINT)");db.update("INSERT INTO pick VALUES(9,1,30,10),(9,2,100,0),(10,1,5,5)");
  QuantityAllocationService service=new QuantityAllocationService(ds,new DataSourceTransactionManager(ds));int[] calls={0};
  RestTemplateProxy rest=new RestTemplateProxy(){public <T> List<T> exchangeList(Class<T> type,String url,org.springframework.http.HttpMethod method,Object payload){calls[0]++;List<Map> result=new ArrayList<>();for(Map<String,Long> scope:(List<Map<String,Long>>)payload){Map v=new HashMap(scope);v.putAll(Map.of("schema","quantity-capacity-v1","warehouseId",1L,"physicalQuantity",100L,"candidateCount",2L,"eligibleCount",2L));result.add(v);}return (List<T>)(List)result;}};
  Field f=QuantityAllocationService.class.getDeclaredField("rest");f.setAccessible(true);f.set(service,rest);
  Map<Long,Map<String,Object>> rows=new HashMap<>();List<QuantityAllocationService.Update> changes=new ArrayList<>();
  for(long n=1;n<=3;n++){rows.put(n,new HashMap<>(Map.of("item_id",n==3?10L:9L,"inventory_status_id",1L,"client_id",0L,"held",0L)));QuantityAllocationService.Update change=new QuantityAllocationService.Update();change.orderLineId=n;change.held=10L;changes.add(change);}
  Method m=QuantityAllocationService.class.getDeclaredMethod("batchCapacities",long.class,List.class,Map.class);m.setAccessible(true);Map<String,Long> result=(Map)m.invoke(service,1L,changes,rows);
  if(calls[0]!=1||result.size()!=2||result.get("9:1:0")!=80||result.get("10:1:0")!=100)throw new AssertionError(result);
  System.out.println("PASS batch allocation: deduplicated scopes, single capacity request, open Pick subtraction, warehouse scope, completed Pick exclusion");
 }
}

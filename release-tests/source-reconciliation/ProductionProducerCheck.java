import com.fasterxml.jackson.databind.*;
import com.garyzhangscm.cwms.allocationwire.CandidateWireCodec;
import com.garyzhangscm.cwms.inventory.*;
import com.garyzhangscm.cwms.inventory.controller.*;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
public class ProductionProducerCheck {
 public static void main(String[] args)throws Exception {
  ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.ERROR);
  ObjectMapper mapper=new InventoryServerApplication().getObjMapper();
  List<Inventory> candidates=mapper.readerForListOf(Inventory.class).readValue(Files.readAllBytes(Path.of(args[0])));
  InventoryController original=new InventoryController();List<Object[]> calls=new ArrayList<>();
  InventoryService service=mock(InventoryService.class,invocation->{if(invocation.getMethod().getName().equals("findPickableInventories")){calls.add(invocation.getArguments().clone());return candidates;}return RETURNS_DEFAULTS.answer(invocation);});
  Field f=InventoryController.class.getDeclaredField("inventoryService");f.setAccessible(true);f.set(original,service);
  CompactCandidateController compact=new CompactCandidateController(original,mapper);
  MockMvc mvc=MockMvcBuilders.standaloneSetup(original,compact).setControllerAdvice(new BaseGlobalResponseBodyAdvice()).setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
  JsonNode[] outputs=new JsonNode[2];int i=0;
  for(String route:new String[]{"/inventories/pickable","/inventories/pickable/compact-v1"}){
   MvcResult result=mvc.perform(get(route).param("itemId","1").param("inventoryStatusId","1").param("color","Blue%20Sky").param("includeDetails","false").param("lpnLimit","500").param("skipLocationIds","3,4").param("attribute1","A1").param("receiptNumber","R-1")).andReturn();
   if(result.getResponse().getStatus()!=200||result.getResolvedException()!=null)throw new AssertionError("MVC route failed: "+route+" "+result.getResolvedException());
   JsonNode body=mapper.readTree(result.getResponse().getContentAsByteArray());if(body.path("result").asInt(-1)!=0)throw new AssertionError("Response advice rejected route");outputs[i++]=body.get("data");
  }
  if(calls.size()!=2||!Arrays.deepEquals(calls.get(0),calls.get(1)))throw new AssertionError("HTTP query binding or controller filters changed");
  List<Inventory> restored=new CandidateWireCodec(mapper,Inventory.class).decode(outputs[1],Inventory.class);
  if(!outputs[0].equals(mapper.readTree(mapper.writeValueAsBytes(restored))))throw new AssertionError("HTTP response fields or candidate order changed");
  // Feed both exact MVC response bodies to the consumer check.
  Files.write(Path.of(args[0]),mapper.writeValueAsBytes(outputs[0]));
  Files.write(Path.of(args[1]),mapper.writeValueAsBytes(outputs[1]));
  System.out.println("PASS exact-live inventory JAR: additive endpoint, real Spring MVC request binding, global response envelope, all filter arguments and ordered field equivalence.");
 }
}

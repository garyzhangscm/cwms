import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.garyzhangscm.cwms.outbound.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.model.Inventory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.*;
import org.springframework.web.client.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import static org.mockito.Mockito.*;
public class ProductionAspectCheck {
 static void inject(Object o,String name,Object v)throws Exception {Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);}
 public static void main(String[] args)throws Throwable {
  ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.ERROR);
  ObjectMapper mapper=new OutboundServerApplication().getObjMapper();JsonNode old=mapper.readTree(Files.readAllBytes(Path.of(args[0]))),compact=mapper.readTree(Files.readAllBytes(Path.of(args[1])));
  final String[] mode={"ok"};List<String> calls=new ArrayList<>();
  RestTemplate http=mock(RestTemplate.class,invocation->{if(invocation.getMethod().getName().equals("exchange")){
   String uri=invocation.getArgument(0);calls.add(uri);
   if(uri.contains("/compact-v1")){
    if(mode[0].equals("404"))throw HttpClientErrorException.create(HttpStatus.NOT_FOUND,"Missing",HttpHeaders.EMPTY,new byte[0],null);
    if(mode[0].equals("500"))throw HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,"Failure",HttpHeaders.EMPTY,new byte[0],null);
    ObjectNode body=mapper.createObjectNode();body.put("result",0);body.set("data",mode[0].equals("bad")?mapper.createObjectNode():compact);return ResponseEntity.ok(body);
   }
   return ResponseEntity.ok(new ResponseBodyWrapper<>(0,"",mapper.convertValue(old,List.class)));
  }return RETURNS_DEFAULTS.answer(invocation);});
  RestTemplateProxy target=new RestTemplateProxy();inject(target,"restTemplate",http);inject(target,"objectMapper",mapper);
  CompactCandidateReadAspect aspect=new CompactCandidateReadAspect();inject(aspect,"proxy",target);inject(aspect,"mapper",mapper);
  AspectJProxyFactory factory=new AspectJProxyFactory(target);factory.addAspect(aspect);RestTemplateProxy advised=factory.getProxy();
  InventoryServiceRestemplateClient client=new InventoryServiceRestemplateClient();inject(client,"restTemplateProxy",advised);
  String uri="http://apigateway:5555/api/inventory/inventories/pickable?includeDetails=false&itemId=1&inventoryStatusId=1&lpnLimit=500&color=Blue%2520Sky&skipLocationIds=3,4";
  List<Inventory> before=advised.exchangeList(Inventory.class,uri,HttpMethod.GET,null);
  if(calls.size()!=1||!calls.get(0).equals(uri))throw new AssertionError("Default-off read changed");
  calls.clear();inject(aspect,"enabled",true);
  List<Inventory> after=advised.exchangeList(Inventory.class,uri,HttpMethod.GET,null);
  if(!mapper.valueToTree(before).equals(mapper.valueToTree(after)))throw new AssertionError("AOP read changed candidates");
  if(calls.size()!=1||!calls.get(0).replace("/compact-v1", "").equals(uri))throw new AssertionError("AOP query changed");
  calls.clear();List<Inventory> fromActualClient=client.getPickableInventory(1L,1L,null,"","Blue%20Sky","XL","S","a1","a2","a3","a4","a5","R-1","3,4");
  if(calls.size()!=1||!calls.get(0).contains("/compact-v1?")||!mapper.valueToTree(fromActualClient).equals(mapper.valueToTree(before)))throw new AssertionError("Exact running client did not use additive hook");
  calls.clear();mode[0]="404";advised.exchangeList(Inventory.class,uri,HttpMethod.GET,null);if(calls.size()!=2||!calls.get(1).equals(uri))throw new AssertionError("404 fallback changed");
  for(String failure:new String[]{"500","bad"}){calls.clear();mode[0]=failure;boolean rejected=false;try{advised.exchangeList(Inventory.class,uri,HttpMethod.GET,null);}catch(RuntimeException expected){rejected=true;}if(!rejected||calls.size()!=1)throw new AssertionError("Unsafe retry: "+failure);}
  calls.clear();mode[0]="ok";String other=uri.replace("lpnLimit=500","lpnLimit=100");advised.exchangeList(Inventory.class,other,HttpMethod.GET,null);if(calls.size()!=1||!calls.get(0).equals(other))throw new AssertionError("Other inventory requests intercepted");
  System.out.println("PASS exact-live outbound JAR: real Spring AOP interception, unchanged running client, default off, exact query preservation, 404-only fallback and failure safety.");
 }
}

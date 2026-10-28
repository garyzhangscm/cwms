import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import com.garyzhangscm.cwms.outbound.service.ManualPickQueueService;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.lang.reflect.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
public class QueueHttpTimeoutChecks {
 public static void main(String[] args)throws Exception{
  RestTemplateProxy proxy=new RestTemplateProxy();RestTemplate normal=new RestTemplate();Field field=RestTemplateProxy.class.getDeclaredField("restTemplate");field.setAccessible(true);field.set(proxy,normal);
  if(proxy.getRestTemplate()!=normal)throw new AssertionError("ordinary request changed");
  Field actor=ManualPickQueueService.class.getDeclaredField("workerActor");actor.setAccessible(true);ThreadLocal<String> context=(ThreadLocal<String>)actor.get(null);context.set("operator");
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{try{Thread.sleep(300);}catch(Exception e){}exchange.close();});server.start();
  try{
   RestTemplate worker=(RestTemplate)proxy.getRestTemplate();if(worker==normal)throw new AssertionError("worker has no bounded client");
   SimpleClientHttpRequestFactory factory=(SimpleClientHttpRequestFactory)worker.getRequestFactory();
   Field connect=SimpleClientHttpRequestFactory.class.getDeclaredField("connectTimeout"),read=SimpleClientHttpRequestFactory.class.getDeclaredField("readTimeout");connect.setAccessible(true);read.setAccessible(true);
   if(connect.getInt(factory)!=3000||read.getInt(factory)!=15000)throw new AssertionError("wrong production timeout");
   factory.setReadTimeout(40);long start=System.nanoTime();
   try{worker.getForObject("http://127.0.0.1:"+server.getAddress().getPort(),String.class);throw new AssertionError("hung response accepted");}catch(ResourceAccessException expected){if((System.nanoTime()-start)/1000000>2500)throw new AssertionError("unbounded wait");}
   context.remove();if(proxy.getRestTemplate()!=normal)throw new AssertionError("worker client leaked into ordinary request");
   System.out.println("PASS queued HTTP timeout, real stalled response, ordinary client preservation and actor cleanup");
  }finally{context.remove();server.stop(0);}
 }
}

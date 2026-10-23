import com.garyzhangscm.cwms.outbound.transit.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.model.*;
import org.springframework.web.client.RestOperations;
import org.springframework.http.ResponseEntity;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

public class TransitAccessRegression {
 static class Resource extends ResourceServiceRestemplateClient {
  User user=new User();String actor;
  public User getUserByUsername(Long company,String name){actor=name;return user;}
 }
 public static void main(String[] args)throws Exception {
  TransitRuleAccess access=new TransitRuleAccess();Resource resource=new Resource();
  resource.user.setCompanyId(1L);resource.user.setUsername("verified-admin");resource.user.setEnabled(true);resource.user.setLocked(false);resource.user.setAdmin(true);
  TransitRegression.set(access,"layout",new TransitRegression.Layout());TransitRegression.set(access,"resource",resource);TransitRegression.set(access,"authUrl","http://auth-test");
  RestOperations auth=(RestOperations)Proxy.newProxyInstance(RestOperations.class.getClassLoader(),new Class[]{RestOperations.class},(p,m,a)->{
   if(!m.getName().equals("exchange"))throw new AssertionError("Unexpected Auth call");
   TransitRegression.check(a[0].toString().contains("companyId=1"),"Auth checked against warehouse company");
   Map<String,Object> body=new HashMap<>();body.put("result",0);body.put("data","verified-admin");return ResponseEntity.ok(body);
  });
  TransitRegression.set(access,"auth",auth);
  HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
   if(m.getName().equals("getHeader"))return "Authorization".equals(a[0])?"Bearer test-token":"forged-admin";return null;
  });
  TransitRegression.check("verified-admin".equals(access.requireAdmin(request,1L)) && "verified-admin".equals(resource.actor),"Verified token identity used; caller identity ignored");
  resource.user.setAdmin(false);TransitRegression.rejects(()->access.requireAdmin(request,1L),403,"Non-admin denied");resource.user.setAdmin(true);
  resource.user.setLocked(true);TransitRegression.rejects(()->access.requireAdmin(request,1L),403,"Locked account denied");resource.user.setLocked(false);
  resource.user.setCompanyId(2L);TransitRegression.rejects(()->access.requireAdmin(request,1L),403,"Other company denied");resource.user.setCompanyId(1L);
  HttpServletRequest anonymous=(HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->null);
  TransitRegression.rejects(()->access.requireAdmin(anonymous,1L),401,"Missing token denied");
  TransitRegression.set(access,"auth",(RestOperations)Proxy.newProxyInstance(RestOperations.class.getClassLoader(),new Class[]{RestOperations.class},(p,m,a)->{throw new RuntimeException("Unavailable");}));
  TransitRegression.rejects(()->access.requireAdmin(request,1L),401,"Auth outage fails closed");
  System.out.println("PASS: "+TransitRegression.checks+" authorization checks; no external requests.");
 }
}

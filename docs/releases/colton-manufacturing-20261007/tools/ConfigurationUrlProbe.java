import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.util.UriComponentsBuilder;
public class ConfigurationUrlProbe {
 public static void main(String[] args)throws Exception {
  String template=WarehouseLayoutServiceRestemplateClient.class.getDeclaredField("manufacturingIssueConfigurationUrl").getAnnotation(Value.class).value();
  String resolved=new StandardEnvironment().resolveRequiredPlaceholders(template);
  String uri=UriComponentsBuilder.fromUriString(resolved).buildAndExpand(1L).toUriString();
  if(!uri.equals("http://apigateway:5555/api/layout/warehouse-configuration/by-warehouse/1"))throw new AssertionError("Incorrect formal configuration endpoint");
  System.out.println("Actual release JAR: default fresh-configuration URL resolves correctly.");
 }
}

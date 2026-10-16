import com.fasterxml.jackson.databind.*;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.QCRuleService;
import com.garyzhangscm.cwms.inventory.repository.QCRuleRepository;
import java.lang.reflect.*;
import java.util.*;

public class QCSpanishRegression {
 static final ObjectMapper json = new ObjectMapper();
 static QCRuleItem item(String fields) throws Exception {
  return json.readValue("{\"id\":10,\"checkPoint\":\"Original question\",\"expectedValue\":\"true\""+fields+"}",QCRuleItem.class);
 }
 static QCRule rule(QCRuleItem item) {
  QCRule r=new QCRule();r.setId(1L);r.setWarehouseId(1L);r.setQcRuleItems(new ArrayList<>(List.of(item)));return r;
 }
 static QCRuleService service(QCRule existing) throws Exception {
  QCRuleRepository repo=(QCRuleRepository)Proxy.newProxyInstance(QCRuleRepository.class.getClassLoader(),new Class[]{QCRuleRepository.class},(p,m,a)->{
   if(m.getName().equals("findById"))return Optional.of(existing);
   if(m.getName().equals("save"))return a[0];
   throw new UnsupportedOperationException(m.getName());
  });
  QCRuleService service=new QCRuleService();
  Field f=QCRuleService.class.getDeclaredField("qcRuleRepository");f.setAccessible(true);f.set(service,repo);return service;
 }
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args) throws Exception {
  QCRuleItem old=item("");require(!old.isCheckPointEsProvided(),"Missing field must be distinguishable");
  QCRule existing=rule(item(",\"checkPointEs\":\"¿Cumple con el estándar?\""));
  QCRuleService service=service(existing);
  service.addQCRule(rule(old));require("¿Cumple con el estándar?".equals(old.getCheckPointEs()),"Older client must preserve translation");
  QCRuleItem cleared=item(",\"checkPointEs\":null");service.changeQCRule(1L,rule(cleared));require(cleared.getCheckPointEs()==null,"Explicit null clears");
  QCRuleItem blank=item(",\"checkPointEs\":\"   \"");service.addQCRule(rule(blank));require(blank.getCheckPointEs()==null,"Blank clears");
  QCRuleItem edited=item(",\"checkPointEs\":\"  Nueva pregunta  \"");service.addQCRule(rule(edited));require("Nueva pregunta".equals(edited.getCheckPointEs()),"Edit and trim");
  require("Original question".equals(edited.getCheckPoint())&&"true".equals(edited.getExpectedValue()),"Standards unchanged");
  JsonNode data=json.valueToTree(edited);require(!data.has("checkPointEsProvided"),"Internal presence flag not exposed");
  require("Nueva pregunta".equals(data.get("checkPointEs").asText()),"API exposes Spanish");
  require(QCRuleItem.class.getDeclaredField("checkPointEs").getAnnotation(jakarta.persistence.Column.class).name().equals("check_point_es"),"Column mapping");
  System.out.println("PASS: old-client preservation, explicit clear, blank clear, edit, original standard, JSON and column mapping");
 }
}

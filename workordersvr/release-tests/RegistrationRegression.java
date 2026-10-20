import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;
import com.garyzhangscm.cwms.workorder.service.WorkOrderQCSampleRegistrationListener;
import com.garyzhangscm.cwms.workorder.service.UserService;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import jakarta.persistence.*;
import org.springframework.context.support.GenericApplicationContext;
import java.time.*;
import java.util.*;

public class RegistrationRegression {
 static int checks;
 static class Identity extends UserService {
  String name;
  public String getCurrentUserName(){return name;}
 }
 static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
 public static void main(String[] args) throws Exception {
  Identity identity=new Identity();
  try(GenericApplicationContext context=new GenericApplicationContext()) {
   context.registerBean(UserService.class,()->identity);
   context.registerBean(WorkOrderQCSampleRegistrationListener.class);
   context.refresh();
   var listener=context.getBean(WorkOrderQCSampleRegistrationListener.class);
   for(String user:List.of("LYDIA"," Blanca ","Jose")) {
    identity.name=user;
    var sample=new WorkOrderQCSample();sample.setCreatedBy("CWMS-USER");sample.setNumber("SAMPLE");
    var timestamp=ZonedDateTime.parse("2026-10-19T14:53:53Z");sample.setCreatedTime(timestamp);
    listener.recordRegistrant(sample);
    check(sample.getCreatedBy().equals(user.trim()));check(sample.getCreatedTime().equals(timestamp));
    check(sample.getNumber().equals("SAMPLE"));
   }
   for(String user:Arrays.asList(null,""," ","CWMS-USER","cwms-user","anonymousUser","Anonymous")) {
    identity.name=user;
    try {listener.recordRegistrant(new WorkOrderQCSample());throw new AssertionError("Missing identity accepted");}
    catch(WorkOrderException expected){checks++;}
   }
   var callback=WorkOrderQCSampleRegistrationListener.class.getMethod("recordRegistrant",WorkOrderQCSample.class);
   check(callback.isAnnotationPresent(PrePersist.class));
   check(!callback.isAnnotationPresent(PreUpdate.class));
   check(Arrays.asList(WorkOrderQCSample.class.getDeclaredAnnotation(EntityListeners.class).value()).contains(WorkOrderQCSampleRegistrationListener.class));
   // The existing inherited update callback must preserve an earlier person's registration.
   var old=new WorkOrderQCSample();old.setId(123L);old.setCreatedBy("LYDIA");
   old.setCreatedTime(ZonedDateTime.parse("2026-10-19T14:53:53Z"));old.saveCreatedTime();
   old.setCreatedBy("Blanca");old.setLastModifiedBy("Blanca");old.onPreUpdate();
   check(old.getCreatedBy().equals("LYDIA"));
   check(old.getCreatedTime().equals(ZonedDateTime.parse("2026-10-19T14:53:53Z")));
   old.setCreatedBy("CWMS-USER");old.saveCreatedTime();old.onPreUpdate();
   check(old.getCreatedBy().equals("CWMS-USER"));
  }
  System.out.println("PASS: "+checks+" actual-model/listener checks; Spring injection, current registrant, identity rejection, creation-only callback and historical audit preservation. No database/network calls.");
 }
}

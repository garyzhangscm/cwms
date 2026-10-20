import java.nio.file.*;import java.util.*;import jakarta.persistence.*;
import org.hibernate.cfg.Configuration;import org.springframework.context.support.GenericApplicationContext;import org.springframework.orm.hibernate5.SpringBeanContainer;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;import com.garyzhangscm.cwms.workorder.service.*;
public class RegistrationJpaRegression {
 static class Identity extends UserService {String name="LYDIA";public String getCurrentUserName(){return name;}}
 public static void main(String[] a)throws Exception{
  var identity=new Identity();try(var spring=new GenericApplicationContext()){
   spring.registerBean(UserService.class,()->identity);spring.registerBean(WorkOrderQCSampleRegistrationListener.class);spring.refresh();
   var cfg=new Configuration();cfg.setProperty("hibernate.connection.driver_class","org.h2.Driver");cfg.setProperty("hibernate.connection.url","jdbc:h2:mem:registration;MODE=MySQL;NON_KEYWORDS=VALUE,KEY,USER,ORDER;DB_CLOSE_DELAY=-1");cfg.setProperty("hibernate.hbm2ddl.auto","create-drop");cfg.setProperty("hibernate.show_sql","false");cfg.getProperties().put("hibernate.resource.beans.container",new SpringBeanContainer(spring.getBeanFactory()));
   var path=Path.of(a[0]);try(var files=Files.walk(path)){for(var f:files.filter(p->p.toString().endsWith(".class")&&!p.toString().contains("$" )).toList()){var name=path.relativize(f).toString().replace('/','.').replace(".class","");if(!name.contains(".model."))continue;Class<?> c=Class.forName(name,false,RegistrationJpaRegression.class.getClassLoader());if(c.isAnnotationPresent(Entity.class))cfg.addAnnotatedClass(c);}}
   try(var factory=cfg.buildSessionFactory()){
    Long id;try(var session=factory.openSession()){var tx=session.beginTransaction();var sample=new WorkOrderQCSample();sample.setNumber("OFFLINE-ONLY");sample.setWarehouseId(1L);sample.setCreatedBy("CWMS-USER");session.persist(sample);session.flush();if(!"LYDIA".equals(sample.getCreatedBy()))throw new AssertionError("registrant missing");id=sample.getId();tx.commit();}
    identity.name="Blanca";try(var session=factory.openSession()){var tx=session.beginTransaction();var sample=session.find(WorkOrderQCSample.class,id);sample.setNumber("OFFLINE-EDIT");sample.setCreatedBy("Blanca");sample.setLastModifiedBy("Blanca");tx.commit();}
    try(var session=factory.openSession()){if(!"LYDIA".equals(session.find(WorkOrderQCSample.class,id).getCreatedBy()))throw new AssertionError("history changed");}
    identity.name="CWMS-USER";boolean rejected=false;try(var session=factory.openSession()){var tx=session.beginTransaction();try{var sample=new WorkOrderQCSample();sample.setNumber("REJECT");session.persist(sample);session.flush();}catch(RuntimeException e){rejected=true;}finally{tx.rollback();}}if(!rejected)throw new AssertionError("unidentified sample accepted");
   }
  }System.out.println("PASS: actual Hibernate persistence and Spring listener injection; current registrant saved, historical registrant preserved, generic identity rejected. In-memory H2 only.");
 }
}

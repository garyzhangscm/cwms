import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import javax.persistence.EntityManager;
import java.util.Properties;
import qatest.TransactionProbe;

/** Verify the actual Spring JPA manager shares its JDBC transaction with the new ledger. */
public class JpaJdbcAtomicChecks {
 public static void main(String[] args) {
  JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:atomic;MODE=MySQL;DB_CLOSE_DELAY=-1");
  LocalContainerEntityManagerFactoryBean factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);factory.setPackagesToScan("qatest");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
  Properties properties=new Properties();properties.setProperty("hibernate.hbm2ddl.auto","create-drop");properties.setProperty("hibernate.dialect","org.hibernate.dialect.H2Dialect");factory.setJpaProperties(properties);factory.afterPropertiesSet();
  try {
   JpaTransactionManager manager=new JpaTransactionManager(factory.getObject());manager.afterPropertiesSet();
   if(manager.getDataSource()!=source)throw new AssertionError("JDBC is not enlisted in JPA transaction");
   EntityManager em=SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());JdbcTemplate db=new JdbcTemplate(source);db.execute("CREATE TABLE qa_test_hold (id BIGINT PRIMARY KEY,held BIGINT NOT NULL)");db.update("INSERT INTO qa_test_hold VALUES (1,100)");
   TransactionTemplate tx=new TransactionTemplate(manager);
   try {tx.execute(status->{db.update("UPDATE qa_test_hold SET held=held-30 WHERE id=1");em.persist(new TransactionProbe(1,30));em.flush();throw new IllegalStateException("Injected failure after Pick creation");});throw new AssertionError("Expected failure");}catch(IllegalStateException expected){}
   if(db.queryForObject("SELECT held FROM qa_test_hold WHERE id=1",Long.class)!=100 || db.queryForObject("SELECT COUNT(*) FROM qa_test_pick",Long.class)!=0)throw new AssertionError("Partial conversion committed");
   tx.execute(status->{db.update("UPDATE qa_test_hold SET held=held-30 WHERE id=1");em.persist(new TransactionProbe(2,30));em.flush();return null;});
   if(db.queryForObject("SELECT held FROM qa_test_hold WHERE id=1",Long.class)!=70 || db.queryForObject("SELECT SUM(quantity) FROM qa_test_pick",Long.class)!=30)throw new AssertionError("Conversion did not commit together");
   System.out.println("PASS JPA/JDBC shared transaction: rollback and commit preserve soft-to-hard quantity exactly");
  } finally {factory.destroy();}
 }
}

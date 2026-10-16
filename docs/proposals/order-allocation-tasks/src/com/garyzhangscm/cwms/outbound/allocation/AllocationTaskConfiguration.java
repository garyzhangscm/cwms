package com.garyzhangscm.cwms.outbound.allocation;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@Configuration
public class AllocationTaskConfiguration {
 @Bean(name="orderAllocationExecutor") public ThreadPoolTaskExecutor executor() {
  ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(2);e.setMaxPoolSize(2);e.setQueueCapacity(20);e.setThreadNamePrefix("order-allocation-");e.setWaitForTasksToCompleteOnShutdown(true);e.setAwaitTerminationSeconds(30);return e;
 }
 @Bean public AllocationTaskService allocationTaskService(DataSource ds,PlatformTransactionManager manager,ProductionAllocationGateway gateway,@Qualifier("orderAllocationExecutor") ThreadPoolTaskExecutor executor) {
  return new AllocationTaskService(new AllocationTaskStore(new JdbcTemplate(ds)),gateway,new TransactionTemplate(manager),executor,Clock.systemUTC());
 }
 @Bean public RecoverySweep allocationRecoverySweep(AllocationTaskService service){return new RecoverySweep(service);}
 public static class RecoverySweep {
  private final AllocationTaskService service;
  RecoverySweep(AllocationTaskService service){this.service=service;}
  @Scheduled(fixedDelay=30000) public void check(){service.recoverStaleTasks();}
 }
}

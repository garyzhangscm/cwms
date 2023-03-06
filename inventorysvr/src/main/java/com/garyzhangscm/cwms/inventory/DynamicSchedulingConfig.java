package com.garyzhangscm.cwms.inventory;

import com.garyzhangscm.cwms.inventory.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshotConfiguration;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotConfigurationService;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotBatchService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamically schedule the background job. we will use this class to
 * read the inventory snapshot configuration and generate the inventory
 * snapshot dynamically
 */
@Configuration
@EnableScheduling
public class DynamicSchedulingConfig implements SchedulingConfigurer, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSchedulingConfig.class);

    // Key: warehouse id
    // string cron express
    // we save the last cron express so that when
    // the express is changed, we will start the task with the new cron express
    Map<Long, String> lastInventorySnapshotCronExpressions = new HashMap<>();
    Map<Long, String> lastLocationUtilizationSnapshotCronExpressions = new HashMap<>();

    @Autowired
    private InventorySnapshotService inventorySnapshotService;
    @Autowired
    private LocationUtilizationSnapshotBatchService locationUtilizationSnapshotBatchService;

    @Autowired
    AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    ScheduledTaskRegistrar scheduledTaskRegistrar;

    Map<String, ScheduledFuture> scheduledTaskMap = new HashMap<>();

/**
    @Bean
    public ScheduledExecutorService executor(){
        return executor;
    }
 **/

    public ScheduledTaskRegistrar getCurrentScheduledTaskRegistrar(){
        return scheduledTaskRegistrar;
    }

    @Bean
    public TaskScheduler poolScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        scheduler.setPoolSize(1);
        scheduler.initialize();
        return scheduler;
    }

    @Autowired
    @Qualifier("oauth2ClientContext")
    OAuth2ClientContext oauth2ClientContext;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        logger.debug("Start to configure scheduled tasks");
        if (scheduledTaskRegistrar == null) {
            scheduledTaskRegistrar = taskRegistrar;
        }
        if (taskRegistrar.getScheduler() == null) {
            taskRegistrar.setScheduler(poolScheduler());
        }

        // clear all the jobs first
        clearScheduledTasks();
        // Loop through each configuration and
        // generate the inventory snapshot
        inventorySnapshotConfigurationService.findAll(null).forEach(

                    inventorySnapshotConfiguration -> {

                        logger.debug("Start to process inventorySnapshotConfiguration: \n{}",
                                inventorySnapshotConfiguration);
                        if (Strings.isNotBlank(inventorySnapshotConfiguration.getCron())) {

                            setupInventorySnapshotTask(inventorySnapshotConfiguration);
                        }

                        if (Strings.isNotBlank(inventorySnapshotConfiguration.getLocationUtilizationSnapshotCron())) {

                            setupLocationUtilizationSnapshotTask(inventorySnapshotConfiguration);
                        }
                    }
            );
            logger.debug("Now we have {} scheduled task ", taskRegistrar.getTriggerTaskList());
    }

    private void setupLocationUtilizationSnapshotTask(

            InventorySnapshotConfiguration inventorySnapshotConfiguration) {

        // register the tasks for location utilization snapshot,
        // based on the cron configured
        Runnable runnableTaskForLocationUtilizationSnapshot = () -> {
            logger.debug("# auto run inventorySnapshotConfiguration:   >>>>  \n {}",
                    inventorySnapshotConfiguration);

            try {

                setupOAuth2Context();
                logger.debug("> generate location utilization snapshot");
                locationUtilizationSnapshotBatchService.generateLocationUtilizationSnapshotBatch(
                        inventorySnapshotConfiguration.getWarehouseId()
                );
            } catch (IOException e) {

                e.printStackTrace();
            }
        };

        Trigger locationUtilizationSnapshotTrigger = triggerContext -> {

            /***
            String newCronExpression =
                    inventorySnapshotConfiguration.getLocationUtilizationSnapshotCron();
            String lastCronExpression =
                    lastLocationUtilizationSnapshotCronExpressions.getOrDefault(
                            inventorySnapshotConfiguration.getWarehouseId(),
                            inventorySnapshotConfiguration.getLocationUtilizationSnapshotCron()
                    );
            logger.debug("Location Utilization Snapshot: lastCronExpression: {}, newCronExpression: {}",
                    lastCronExpression, newCronExpression);

            lastLocationUtilizationSnapshotCronExpressions.put(
                    inventorySnapshotConfiguration.getWarehouseId(),
                    newCronExpression
            );

            // if the cron schedule is changed, restart the
            // task
            if (!StringUtils.equalsIgnoreCase(
                    newCronExpression, lastCronExpression)) {

                logger.debug("Location Utilization Snapshot: lastCronExpression: {}, newCronExpression: {} DOESN'T MATCH!",
                        lastCronExpression, newCronExpression);
                taskRegistrar.setTriggerTasksList(new ArrayList<>());

                configureTasks(taskRegistrar); // calling recursively.

                taskRegistrar.destroy(); // destroys previously scheduled tasks.

                taskRegistrar.setScheduler(executor);

                taskRegistrar.afterPropertiesSet(); // this will schedule the task with new cron changes.

                return null; // return null when the cron changed so the trigger will stop.

            }
            logger.debug("Inventory Snapshot: SETUP NEXT run time: {}",
                    newCronExpression);
            CronTrigger crontrigger = new CronTrigger(newCronExpression);
             **/
            logger.debug("Location Utilization Snapshot: SETUP NEXT run time: {}",
                    inventorySnapshotConfiguration.getLocationUtilizationSnapshotCron());
            CronTrigger crontrigger = new CronTrigger( inventorySnapshotConfiguration.getLocationUtilizationSnapshotCron());

            return crontrigger.nextExecutionTime(triggerContext);
        };
        // taskRegistrar.addTriggerTask(runnableTaskForLocationUtilizationSnapshot, locationUtilizationSnapshotTrigger);
        String taskName = inventorySnapshotConfiguration.getWarehouseId() + "-" + "location-utilization-snapshot";
        addTask(taskName, runnableTaskForLocationUtilizationSnapshot, locationUtilizationSnapshotTrigger);

    }


    private void setupInventorySnapshotTask(InventorySnapshotConfiguration inventorySnapshotConfiguration) {

        // register the tasks for inventory snapshot,
        // based on the cron configured
        logger.debug("Start to setup run task for inventorySnapshotConfiguration: >>>> \n {}",
                inventorySnapshotConfiguration);
        Runnable runnableTaskForInventorySnapshot = () -> {
            logger.debug("# auto run inventorySnapshotConfiguration:   >>>>  \n {}",
                    inventorySnapshotConfiguration);

            try {

                setupOAuth2Context();
                logger.debug("> generate inventory snapshot");
                inventorySnapshotService.generateInventorySnapshot(
                        inventorySnapshotConfiguration.getWarehouseId()
                );
            } catch (IOException e) {

                e.printStackTrace();
            }
        };
        Trigger inventorySnapshotTrigger = triggerContext -> {

            /**
            String newCronExpression =
                    inventorySnapshotConfiguration.getCron();
            String lastCronExpression =
                    lastInventorySnapshotCronExpressions.getOrDefault(
                            inventorySnapshotConfiguration.getWarehouseId(),
                            inventorySnapshotConfiguration.getCron()
                    );
            logger.debug("Inventory Snapshot: lastCronExpression: {}, newCronExpression: {}",
                    lastCronExpression, newCronExpression);

            lastInventorySnapshotCronExpressions.put(
                    inventorySnapshotConfiguration.getWarehouseId(),
                    newCronExpression
            );

            // if the cron schedule is changed, restart the
            // task
            if (!StringUtils.equalsIgnoreCase(
                    newCronExpression, lastCronExpression)) {

                logger.debug("Inventory Snapshot: lastCronExpression: {}, newCronExpression: {} DOESN'T MATCH!",
                        lastCronExpression, newCronExpression);
                taskRegistrar.setTriggerTasksList(new ArrayList<>());

                configureTasks(taskRegistrar); // calling recursively.

                taskRegistrar.destroy(); // destroys previously scheduled tasks.

                taskRegistrar.setScheduler(executor);

                taskRegistrar.afterPropertiesSet(); // this will schedule the task with new cron changes.

                return null; // return null when the cron changed so the trigger will stop.

            }
            logger.debug("Inventory Snapshot: SETUP NEXT run time: {}",
                    newCronExpression);
            CronTrigger crontrigger = new CronTrigger(newCronExpression);
             **/
            logger.debug("Inventory Snapshot: SETUP NEXT run time: {}",
                    inventorySnapshotConfiguration.getCron());
            CronTrigger crontrigger = new CronTrigger(inventorySnapshotConfiguration.getCron());

            return crontrigger.nextExecutionTime(triggerContext);
        };
        // taskRegistrar.addTriggerTask(runnableTaskForInventorySnapshot, inventorySnapshotTrigger);
        String taskName = inventorySnapshotConfiguration.getWarehouseId() + "-" + "inventory-snapshot";
        addTask(taskName, runnableTaskForInventorySnapshot, inventorySnapshotTrigger);
    }

    @Override
    public void destroy() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }


    /**
     * Remove all current scheduled task
     */
    private void clearScheduledTasks() {
        Iterator<Map.Entry<String, ScheduledFuture>> taskIterator = scheduledTaskMap.entrySet().iterator();
        while(taskIterator.hasNext()) {
            Map.Entry<String, ScheduledFuture> taskEntry = taskIterator.next();
            ScheduledFuture future = taskEntry.getValue();
            future.cancel(true);
            taskIterator.remove();
        }
    }
    private boolean addTask(String taskName, Runnable runnable, Trigger trigger) {
        if (scheduledTaskMap.containsKey(taskName)) {
            return false;
        }

        ScheduledFuture future =
                scheduledTaskRegistrar.getScheduler()
                        .schedule(runnable, trigger);

        // configureTasks(scheduledTaskRegistrar);
        scheduledTaskMap.put(taskName, future);
        return true;
    }

    private boolean removeTask(String name) {
        if (!scheduledTaskMap.containsKey(name)) {
            return false;
        }
        ScheduledFuture future = scheduledTaskMap.get(name);
        future.cancel(true);
        scheduledTaskMap.remove(name);
        return true;
    }

    /**
     * Setup the OAuth2 token for the background job
     * OAuth2 token will be setup automatically in a web request context
     * but for a separate thread outside the web context, we will need to
     * setup the OAuth2 manually
     * @throws IOException
     */
    private void setupOAuth2Context() throws IOException {

        // Setup the request context so we can utilize the OAuth
        // as if we were in a web request context
        RequestContextHolder.setRequestAttributes(new CustomRequestScopeAttr());

        // Get token. We will use a default user to login and get
        // the OAuth2 token by the default user
        String token = authServiceRestemplateClient.getCurrentLoginUser().getToken();
        // logger.debug("# start to setup the oauth2 token for background job: {}", token);
        // Setup the access toke for the current thread
        // oauth2ClientContext is a scope = request bean that hold
        // the Oauth2 token
        oauth2ClientContext.setAccessToken(new DefaultOAuth2AccessToken(token));

    }


}
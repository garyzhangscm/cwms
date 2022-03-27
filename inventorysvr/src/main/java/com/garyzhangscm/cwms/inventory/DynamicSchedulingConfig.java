package com.garyzhangscm.cwms.inventory;

import com.garyzhangscm.cwms.inventory.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotConfigurationService;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Dynamically schedule the background job. we will use this class to
 * read the invenotry snapshot configuration and generate the inventory
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
    Map<Long, String> lastCronExpressions = new HashMap<>();
    @Autowired
    private InventorySnapshotService inventorySnapshotService;
    @Autowired
    private LocationUtilizationSnapshotService locationUtilizationSnapshotService;

    @Autowired
    AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    @Autowired
    @Qualifier("oauth2ClientContext")
    OAuth2ClientContext oauth2ClientContext;


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Loop through each configuration and
        // generate the inventory snapshot

            inventorySnapshotConfigurationService.findAll(null).forEach(

                    inventorySnapshotConfiguration -> {
                        logger.debug("Start to setup run task for inventorySnapshotConfiguration: >>>> \n {}",
                                inventorySnapshotConfiguration);
                        Runnable runnableTask = () -> {
                            logger.debug("# auto run inventorySnapshotConfiguration:   >>>>  \n {}",
                                    inventorySnapshotConfiguration);

                            try {

                                setupOAuth2Context();
                                inventorySnapshotService.generateInventorySnapshot(
                                        inventorySnapshotConfiguration.getWarehouseId()
                                );
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        };

                        Trigger trigger = triggerContext -> {

                            String newCronExpression =
                                    inventorySnapshotConfiguration.getCron();
                            String lastCronExpression =
                                    lastCronExpressions.getOrDefault(
                                            inventorySnapshotConfiguration.getWarehouseId(),
                                            inventorySnapshotConfiguration.getCron()
                                    );
                            logger.debug("lastCronExpression: {}, newCronExpression: {}",
                                    lastCronExpression, newCronExpression);

                            lastCronExpressions.put(
                                    inventorySnapshotConfiguration.getWarehouseId(),
                                    newCronExpression
                            );

                            // if the cron schedule is changed, restart the
                            // task
                            if (!StringUtils.equalsIgnoreCase(
                                    newCronExpression, lastCronExpression)) {

                                logger.debug("lastCronExpression: {}, newCronExpression: {} DOESN'T MATCH!",
                                        lastCronExpression, newCronExpression);
                                taskRegistrar.setTriggerTasksList(new ArrayList<>());

                                configureTasks(taskRegistrar); // calling recursively.

                                taskRegistrar.destroy(); // destroys previously scheduled tasks.

                                taskRegistrar.setScheduler(executor);

                                taskRegistrar.afterPropertiesSet(); // this will schedule the task with new cron changes.

                                return null; // return null when the cron changed so the trigger will stop.

                            }

                            logger.debug("SETUP NEXT run time: {}",
                                    newCronExpression);
                            CronTrigger crontrigger = new CronTrigger(newCronExpression);

                            return crontrigger.nextExecutionTime(triggerContext);
                        };
                        taskRegistrar.addTriggerTask(runnableTask, trigger);
                    }
            );
    }

    @Override
    public void destroy() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
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
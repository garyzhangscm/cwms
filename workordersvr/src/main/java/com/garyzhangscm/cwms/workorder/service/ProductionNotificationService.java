package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.KafkaSender;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.Alert;
import com.garyzhangscm.cwms.workorder.model.AlertType;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;

/** Best-effort change notifications; no entity or transaction crosses threads. */
@Service
public class ProductionNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionNotificationService.class);
    private final KafkaSender kafkaSender;
    private final WarehouseLayoutServiceRestemplateClient layout;
    private final ExecutorService executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(256), runnable -> {
                Thread thread = new Thread(runnable, "production-notification");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public ProductionNotificationService(KafkaSender kafkaSender, WarehouseLayoutServiceRestemplateClient layout) {
        this.kafkaSender = kafkaSender;
        this.layout = layout;
    }

    public void afterCommit(Long warehouseId, String number, int lineCount, String username) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() ||
                !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Production notification requires a transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    executor.execute(() -> send(warehouseId, number, lineCount, username));
                } catch (RuntimeException error) {
                    // The quantity has committed. Notification failure must not
                    // turn a successful report into a retryable production error.
                    logger.error("Production committed but notification could not be queued: {}", number, error);
                }
            }
        });
    }

    private void send(Long warehouseId, String number, int lineCount, String username) {
        try {
            Long companyId = layout.getWarehouseById(warehouseId).getCompanyId();
            kafkaSender.send(new Alert(companyId, AlertType.MODIFY_WORK_ORDER,
                    "MODIFY-WORK-ORDER-" + companyId + "-" + warehouseId + "-" + number,
                    "Work Order " + number + " is changed, by " + username, "",
                    "number=" + number + "&lineCount=" + lineCount));
        } catch (Exception error) {
            logger.error("Production notification failed for committed work order {}", number, error);
        }
    }

    @PreDestroy public void close() { executor.shutdown(); }
}

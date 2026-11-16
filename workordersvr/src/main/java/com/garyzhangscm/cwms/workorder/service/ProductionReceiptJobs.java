package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.Inventory;
import com.garyzhangscm.cwms.workorder.model.InventoryStatus;
import com.garyzhangscm.cwms.workorder.model.Item;
import com.garyzhangscm.cwms.workorder.model.ItemPackageType;
import com.garyzhangscm.cwms.workorder.model.Location;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.RF;
import com.garyzhangscm.cwms.workorder.model.ReportType;
import com.garyzhangscm.cwms.workorder.model.ReportHistory;
import com.garyzhangscm.cwms.workorder.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.model.WorkOrderProduceTransaction;
import com.garyzhangscm.cwms.workorder.model.WorkOrderProducedInventory;
import com.garyzhangscm.cwms.workorder.model.WorkOrderProducedInventoryResult;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderProduceTransactionRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderProducedInventoryResultRepository;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductionReceiptJobs {
    private static final Logger logger = LoggerFactory.getLogger(ProductionReceiptJobs.class);
    private static final int MAX_ATTEMPTS = 5;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;
    private final WorkOrderProduceTransactionRepository productionTransactions;
    private final WorkOrderProducedInventoryResultRepository results;
    private final InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    private final WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    private final ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    private final WorkOrderService workOrderService;

    public ProductionReceiptJobs(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PlatformTransactionManager platformTransactionManager, EntityManager entityManager, WorkOrderProduceTransactionRepository workOrderProduceTransactionRepository, WorkOrderProducedInventoryResultRepository workOrderProducedInventoryResultRepository, InventoryServiceRestemplateClient inventoryServiceRestemplateClient, WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient, ResourceServiceRestemplateClient resourceServiceRestemplateClient, @Lazy WorkOrderService workOrderService) {
        this.jdbc = jdbcTemplate;
        this.mapper = objectMapper;
        this.transactions = new TransactionTemplate(platformTransactionManager);
        this.entityManager = entityManager;
        this.productionTransactions = workOrderProduceTransactionRepository;
        this.results = workOrderProducedInventoryResultRepository;
        this.inventoryServiceRestemplateClient = inventoryServiceRestemplateClient;
        this.warehouseLayoutServiceRestemplateClient = warehouseLayoutServiceRestemplateClient;
        this.resourceServiceRestemplateClient = resourceServiceRestemplateClient;
        this.workOrderService = workOrderService;
    }

    public void enqueue(WorkOrderProducedInventory detail, WorkOrderProduceTransaction transaction, String string) {
        Inventory inventory = detail.createInventory(transaction.getWorkOrder(), transaction);
        ReceiptPayload payload = new ReceiptPayload(inventory.getLpn(), inventory.getQuantity(), inventory.getLocation(), inventory.getItem(), inventory.getItemPackageType(), inventory.getInventoryStatus(), inventory.getWarehouseId(), inventory.getWorkOrderId(), inventory.getReasonCodeId());
        try {
            this.jdbc.update("INSERT INTO work_order_production_receipt_job\n    (detail_id, transaction_id, payload, rf_code, print_label)\nVALUES (?, ?, ?, ?, ?)\n", new Object[]{detail.getId(), transaction.getId(), this.mapper.writeValueAsString(payload), string, !Boolean.TRUE.equals(transaction.getSuppressLabelPrinting())});
        }
        catch (JsonProcessingException jsonProcessingException) {
            throw new IllegalStateException("Unable to save production receipt job", jsonProcessingException);
        }
    }

    @Scheduled(fixedDelayString="${production.receipt.poll-ms:1000}")
    public void processDueJobs() {
        for (Long l : this.jdbc.queryForList("SELECT detail_id FROM work_order_production_receipt_job\nWHERE next_attempt_at <= CURRENT_TIMESTAMP(6)\nORDER BY next_attempt_at, detail_id LIMIT 10\n", Long.class)) {
            try {
                this.transactions.executeWithoutResult(transactionStatus -> process(l));
            }
            catch (RuntimeException runtimeException) {
                logger.error("Could not complete production receipt job {}", l, runtimeException);
            }
        }
    }

    private void process(Long l) {
        boolean bl;
        Inventory inventory;
        Job job = this.jdbc.query("SELECT transaction_id, payload, rf_code, print_label, attempts\nFROM work_order_production_receipt_job\nWHERE detail_id = ? AND next_attempt_at <= CURRENT_TIMESTAMP(6)\nFOR UPDATE\n", (resultSet, n) -> new Job(resultSet.getLong("transaction_id"), resultSet.getString("payload"), resultSet.getString("rf_code"), resultSet.getBoolean("print_label"), resultSet.getInt("attempts")), new Object[]{l}).stream().findFirst().orElse(null);
        if (job == null) {
            return;
        }
        WorkOrderProduceTransaction transaction = (WorkOrderProduceTransaction)this.productionTransactions.findById(job.transactionId()).orElseThrow();
        WorkOrder workOrder = transaction.getWorkOrder();
        WorkOrderProducedInventory detail = (WorkOrderProducedInventory)this.entityManager.getReference(WorkOrderProducedInventory.class, l);
        try {
            inventory = ((ReceiptPayload)this.mapper.readValue(job.payload(), ReceiptPayload.class)).toInventory(job.transactionId());
            bl = job.printLabel() && this.inventoryServiceRestemplateClient.findInventoryByLPN(workOrder.getWarehouseId(), inventory.getLpn()).isEmpty();
            inventory = this.inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(workOrder, inventory, l);
            if (inventory == null || inventory.getId() == null) {
                throw new IllegalStateException("Inventory service did not return a receipt");
            }
        }
        catch (JsonProcessingException | RuntimeException throwable) {
            int attempts = job.attempts() + 1;
            if (attempts < MAX_ATTEMPTS) {
                this.jdbc.update("UPDATE work_order_production_receipt_job\nSET attempts = ?, next_attempt_at = TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP(6))\nWHERE detail_id = ?\n", new Object[]{attempts, 5 * attempts, l});
                logger.warn("Production receipt job {} failed on attempt {}", new Object[]{l, attempts, throwable});
                return;
            }
            String string = Objects.toString(throwable.getMessage(), "Production inventory receipt failed");
            this.results.save(new WorkOrderProducedInventoryResult(workOrder.getWarehouseId(), detail, false, string.substring(0, Math.min(string.length(), 255))));
            this.jdbc.update("DELETE FROM work_order_production_receipt_job WHERE detail_id = ?", new Object[]{l});
            return;
        }
        this.results.save(new WorkOrderProducedInventoryResult(workOrder.getWarehouseId(), detail, true, ""));
        this.jdbc.update("DELETE FROM work_order_production_receipt_job WHERE detail_id = ?", new Object[]{l});
        if (bl) {
            try {
                printNEWLPNLabel(inventory, workOrder, transaction.getProductionLine(), job.rfCode());
            }
            catch (JsonProcessingException | RuntimeException throwable) {
                logger.warn("Inventory was received but label printing failed for production detail {}", l, throwable);
            }
        }
    }

    private void printNEWLPNLabel(Inventory inventory, WorkOrder workOrder, ProductionLine productionLine, String string) throws JsonProcessingException {
        RF rf;
        WarehouseConfiguration warehouseConfiguration = this.warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(workOrder.getWarehouseId());
        if (Objects.isNull(warehouseConfiguration) || !Boolean.TRUE.equals(warehouseConfiguration.getNewLPNPrintLabelAtProducingFlag())) {
            logger.debug("The warehouse is configured to not print LPN label for new LPN from work order");
            return;
        }
        String printerName = "";
        if (Strings.isNotBlank(string)) {
            logger.debug("RF code {} is passed, let's see if there's print setup for this RF", string);
            rf = this.resourceServiceRestemplateClient.getRFByCode(workOrder.getWarehouseId(), string);
            if (Objects.nonNull(rf)) {
                printerName = rf.getPrinterName();
                logger.debug("We got printer {} from the RF {}", printerName, string);
            }
        }
        if (Strings.isBlank(printerName)) {
            printerName = getPrinterNameFromProductionLine(productionLine);
        }
        if (Strings.isBlank(printerName)) {
            logger.debug("No printer is setup for production line {}, we will not print labels", productionLine.getName());
            return;
        }
        logger.debug("We will print LPN label for new LPN from printer {}", printerName);
        ReportHistory label = this.workOrderService.generatePrePrintLPNLabel(workOrder, inventory.getLpn(), inventory.getQuantity(), productionLine.getName(), "", printerName);
        logger.debug("Get the label files {}, let's print it from printer {}", label.getFileName(), printerName);
        Long l = this.warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrder.getWarehouseId()).getCompanyId();
        String printResult = this.resourceServiceRestemplateClient.printReport(l, workOrder.getWarehouseId(), ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL, label.getFileName(), printerName, 2);
        logger.debug("print label result: {}", printResult);
    }

    private String getPrinterNameFromProductionLine(ProductionLine productionLine) {
        return productionLine.getLabelPrinterName();
    }

    private record ReceiptPayload(String lpn, Long quantity, Location location, Item item, ItemPackageType itemPackageType, InventoryStatus inventoryStatus, Long warehouseId, Long workOrderId, Long reasonCodeId) {
        Inventory toInventory(long l) {
            Inventory inventory = new Inventory();
            inventory.setLpn(this.lpn);
            inventory.setQuantity(this.quantity);
            inventory.setLocation(this.location);
            inventory.setLocationId(this.location.getId());
            inventory.setItem(this.item);
            inventory.setItemPackageType(this.itemPackageType);
            inventory.setInventoryStatus(this.inventoryStatus);
            inventory.setWarehouseId(this.warehouseId);
            inventory.setWorkOrderId(this.workOrderId);
            inventory.setReasonCodeId(this.reasonCodeId);
            inventory.setCreateInventoryTransactionId(Long.valueOf(l));
            inventory.setVirtual(Boolean.valueOf(false));
            return inventory;
        }
    }

    private record Job(long transactionId, String payload, String rfCode, boolean printLabel, int attempts) {
    }
}

package com.garyzhangscm.cwms.outbound.allocation;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
public class AllocationTask {
 public String id, requestId, state, errorCode, message;
 public long warehouseId, orderId;
 public boolean requiresReview;
 public Instant createdAt, startedAt, finishedAt, heartbeatAt;
 @JsonIgnore public String originalOrderState, activeOrderKey;
 public boolean active() { return "QUEUED".equals(state) || "RUNNING".equals(state); }
}

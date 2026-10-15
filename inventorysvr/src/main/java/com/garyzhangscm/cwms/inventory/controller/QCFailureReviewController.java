package com.garyzhangscm.cwms.inventory.controller;
import com.garyzhangscm.cwms.inventory.model.QCInspectionRequest;
import com.garyzhangscm.cwms.inventory.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class QCFailureReviewController {
 private final QCFailureReviewService service;
 private final QCReviewPreparationService preparation;
 public QCFailureReviewController(QCFailureReviewService service, QCReviewPreparationService preparation) {this.service=service;this.preparation=preparation;}
 @PostMapping("/qc-inspection-requests/confirmed")
 public List<QCInspectionRequest> submit(@RequestParam Long warehouseId,
  @RequestParam(defaultValue="false") boolean failureAcknowledged,
  @RequestParam(defaultValue="") String rfCode,@RequestBody List<QCInspectionRequest> requests) {
  return service.submit(warehouseId,requests,failureAcknowledged,rfCode);
 }
 @PostMapping("/qc-inspection-request/{id}/prepare-review")
 public QCInspectionRequest prepare(@RequestParam Long warehouseId,@PathVariable Long id) {
  return preparation.prepare(warehouseId,id);
 }
}

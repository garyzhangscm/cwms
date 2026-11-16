package com.garyzhangscm.cwms.workorder.qcproduction;
import static com.garyzhangscm.cwms.workorder.qcproduction.QcProductionStore.*;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.sql.DataSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/qc-production")
public class QcProductionController {
 private final QcProductionAccess access;private final QcProductionStore store;
 @Value("${qc.production.preview-token:}") String token;
 public QcProductionController(DataSource ds,PlatformTransactionManager tm,QcProductionAccess access){this.access=access;store=new QcProductionStore(ds,tm);}
 private String check(long w,HttpServletRequest r){String h=r.getHeader("X-MES-QC-Production");require(token!=null&&token.length()>=32&&h!=null&&MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),h.getBytes(StandardCharsets.UTF_8)),"QC production confirmation is available only on port 4200");return access.actor(w);}
 @GetMapping("/capabilities") public Map<String,Object> capabilities(@RequestParam long warehouseId,HttpServletRequest r){String actor=check(warehouseId,r);return Map.of("enabled",true,"actor",actor,"enabledAt",store.enabledAt(warehouseId).toInstant().toString());}
 @GetMapping("/lpn") public Map<String,Object> view(@RequestParam long warehouseId,@RequestParam String lpn,HttpServletRequest r){check(warehouseId,r);return store.view(warehouseId,lpn);}
 public static class Scan {public String requestId,lpn,reason;public Long version;}
 @PostMapping("/{action:confirm|exception|resolve}") public Map<String,Object> scan(@PathVariable String action,@RequestParam long warehouseId,@RequestBody Scan body,HttpServletRequest r){String actor=check(warehouseId,r);return store.act(warehouseId,actor,action,body.lpn,body.requestId,body.reason,body.version,()->access.stock(warehouseId,QcProductionStore.lpn(body.lpn)));}
 @GetMapping("/exceptions") public List<Map<String,Object>> exceptions(@RequestParam long warehouseId,HttpServletRequest r){check(warehouseId,r);return store.exceptions(warehouseId);}
 @GetMapping("/history") public List<Map<String,Object>> history(@RequestParam long warehouseId,@RequestParam String lpn,HttpServletRequest r){check(warehouseId,r);return store.history(warehouseId,lpn);}
 @GetMapping("/report") public Map<String,Object> report(@RequestParam long warehouseId,@RequestParam String start,@RequestParam String end,HttpServletRequest r){check(warehouseId,r);return store.report(warehouseId,Instant.parse(start),Instant.parse(end));}
}

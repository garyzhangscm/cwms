package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.service.ReportHistoryService;
import com.garyzhangscm.cwms.resources.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** QC preprinted labels: bounded history and range reprints of stored labels. */
@RestController
@RequestMapping("/defective-labels")
public class DefectiveLabelController {
    @PersistenceContext private EntityManager entityManager;
    @Autowired private ReportHistoryService histories;
    @Autowired private ReportService reports;

    @GetMapping
    public Map<String, Object> history(@RequestParam("warehouseId") Long warehouseId,
            @RequestParam(name="beforeId", required=false) Long beforeId,
            @RequestParam(name="batch", defaultValue="") String batch) {
        String filter = batch.trim();
        if (filter.length() > 80) throw bad("Invalid batch number");
        String jpql = "select h from ReportHistory h where h.warehouseId=:warehouse and h.type=:type";
        if (beforeId != null) jpql += " and h.id < :before";
        if (!filter.isEmpty()) jpql += " and (h.description=:batch or h.description like :prefix)";
        TypedQuery<ReportHistory> query = entityManager.createQuery(jpql + " order by h.id desc", ReportHistory.class)
                .setParameter("warehouse", warehouseId).setParameter("type", ReportType.DEFECTIVE_LPN_LABEL);
        if (beforeId != null) query.setParameter("before", beforeId);
        if (!filter.isEmpty()) {
            if (!filter.matches("[A-Za-z0-9-]+")) throw bad("Invalid batch number");
            query.setParameter("batch", filter).setParameter("prefix", filter + " | %");
        }
        List<ReportHistory> rows = query.setMaxResults(11).getResultList();
        boolean hasMore = rows.size() > 10;
        return Map.of("items", rows.subList(0, Math.min(10, rows.size())), "hasMore", hasMore);
    }

    @PostMapping("/{warehouseId}/generate")
    public ReportHistory generate(@PathVariable("warehouseId") Long warehouseId, @RequestBody List<String> lpns) throws Exception {
        if (lpns == null || lpns.isEmpty() || lpns.size() > 100 || new HashSet<>(lpns).size() != lpns.size()
                || lpns.stream().anyMatch(lpn -> lpn == null || !lpn.matches("[A-Za-z0-9-]{1,12}")))
            throw bad("Invalid LPN batch");
        // Stable across retries; uniqueness follows warehouse + allocated first LPN.
        String batch = "QC-" + warehouseId + "-" + lpns.get(0);
        List<Map<String,Object>> data = new ArrayList<>();
        for (int i=0; i<lpns.size(); i++) data.add(Map.of("lpn", lpns.get(i), "batch", batch,
                "sequence", (i+1) + " / " + lpns.size()));
        Report report = new Report();
        report.setData(data);
        ReportHistory result = reports.generateReport(warehouseId, ReportType.DEFECTIVE_LPN_LABEL, report, "en", "");
        if (result == null) throw bad("Warehouse not found");
        result.setDescription(batch + " | " + lpns.size());
        return histories.save(result);
    }

    @GetMapping("/{id}/details")
    public Map<String,Object> details(@PathVariable("id") Long id, @RequestParam("warehouseId") Long warehouseId) throws Exception {
        ReportHistory row = scoped(id, warehouseId);
        return Map.of("count", labels(row).size());
    }

    @PostMapping("/{id}/range")
    public Map<String,Object> range(@PathVariable("id") Long id, @RequestParam("warehouseId") Long warehouseId,
            @RequestParam("from") int from, @RequestParam("to") int to) throws Exception {
        ReportHistory row = scoped(id, warehouseId);
        List<String> labels = labels(row);
        if (from < 1 || to < from || to > labels.size()) throw bad("Invalid label range");
        if (from == 1 && to == labels.size()) return Map.of("fileName", row.getFileName());
        Path original = histories.getReportFile(id).toPath();
        String name = original.getFileName().toString().replaceFirst("\\.lbl$", "") + "_range_" + from + "_" + to + ".lbl";
        Path target = original.resolveSibling(name);
        // Copy original ZPL blocks, keeping original LPN, batch and sequence intact.
        Path temp = Files.createTempFile(original.getParent(), "qc-range-", ".tmp");
        try {
            Files.writeString(temp, String.join("\n", labels.subList(from-1, to)), StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temp); }
        return Map.of("fileName", name);
    }

    private ReportHistory scoped(Long id, Long warehouseId) {
        ReportHistory row = histories.findById(id, false);
        if (!Objects.equals(row.getWarehouseId(), warehouseId) || row.getType() != ReportType.DEFECTIVE_LPN_LABEL)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Label batch not found in warehouse");
        return row;
    }
    private List<String> labels(ReportHistory row) throws Exception {
        Path file = histories.getReportFile(row.getId()).toPath();
        if (Files.size(file) > 2_000_000) throw bad("Label file too large");
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\\^XA.*?\\^XZ", Pattern.DOTALL).matcher(text);
        List<String> labels = new ArrayList<>();
        while (matcher.find()) labels.add(matcher.group());
        if (labels.isEmpty() || labels.size() > 100 || !text.replaceAll("(?s)\\^XA.*?\\^XZ", "").trim().isEmpty())
            throw bad("Invalid stored label file");
        return labels;
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}

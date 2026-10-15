package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.exception.QCException;
import java.util.*;

/** Uses trusted persisted rules, never the standards supplied by the browser. */
public final class QCFailureReviewPolicy {
    private QCFailureReviewPolicy() { }
    public static void apply(QCInspectionRequest stored, QCInspectionRequest input, boolean acknowledged) {
        if (stored.getQcInspectionResult() != QCInspectionResult.PENDING)
            reject("This inspection has already been submitted. Reload before continuing.");
        Map<Long, QCInspectionRequestItemOption> received = new HashMap<>();
        if (input.getQcInspectionRequestItems() == null) reject("Inspection checks are required.");
        for (QCInspectionRequestItem group : input.getQcInspectionRequestItems()) {
            if (group.getQcInspectionRequestItemOptions() == null) reject("Inspection checks are required.");
            for (QCInspectionRequestItemOption option : group.getQcInspectionRequestItemOptions())
                if (option.getId() == null || received.put(option.getId(), option) != null) reject("Duplicate or missing check identifier.");
        }
        boolean failed = false;
        for (QCInspectionRequestItem group : stored.getQcInspectionRequestItems()) {
            List<QCInspectionResult> results = new ArrayList<>();
            for (QCInspectionRequestItemOption option : group.getQcInspectionRequestItemOptions()) {
                QCInspectionRequestItemOption answer = received.remove(option.getId());
                if (answer == null) reject("The inspection checks changed. Reload before continuing.");
                // Preserve explicitly excluded checks; current rule enabled flags do not rewrite a request.
                QCInspectionResult old = option.getQcInspectionResult();
                if (old == QCInspectionResult.SKIPPED || old == QCInspectionResult.CANCELLED) {
                    results.add(old); continue;
                }
                option.setBooleanValue(answer.getBooleanValue());
                option.setDoubleValue(answer.getDoubleValue());
                option.setStringValue(answer.getStringValue());
                QCInspectionResult result = answer.getQcInspectionResult() == QCInspectionResult.NOT_APPLY
                        ? QCInspectionResult.NOT_APPLY : evaluate(option);
                option.setQcInspectionResult(result);
                String notes = answer.getExceptionNotes() == null ? "" : answer.getExceptionNotes().trim();
                if (notes.length() > 2000) reject("Exception notes must not exceed 2000 characters.");
                if (result == QCInspectionResult.FAIL && notes.isEmpty()) reject("Exception notes are required for every failed check.");
                option.setExceptionNotes(result == QCInspectionResult.FAIL ? notes : null);
                failed |= result == QCInspectionResult.FAIL;
                results.add(result);
            }
            group.setQcInspectionResult(aggregate(results));
        }
        if (!received.isEmpty()) reject("Unknown inspection check.");
        if (failed && !acknowledged) reject("Confirm the nonconforming results before submitting.");
        List<QCInspectionResult> groups = new ArrayList<>();
        stored.getQcInspectionRequestItems().forEach(g -> groups.add(g.getQcInspectionResult()));
        stored.setQcInspectionResult(aggregate(groups));
    }
    public static QCInspectionResult aggregate(List<QCInspectionResult> results) {
        if (results.contains(QCInspectionResult.FAIL)) return QCInspectionResult.FAIL;
        if (results.isEmpty() || results.contains(null) || results.contains(QCInspectionResult.PENDING)) return QCInspectionResult.PENDING;
        return QCInspectionResult.PASS;
    }
    public static QCInspectionResult evaluate(QCInspectionRequestItemOption option) {
        QCRuleItem rule = option.getQcRuleItem();
        if (rule == null || rule.getQcRuleItemType() == null || rule.getQcRuleItemComparator() == null || rule.getExpectedValue() == null)
            throw QCException.raiseException("The acceptance standard is incomplete. Contact a QC administrator.");
        String expected = rule.getExpectedValue();
        boolean pass;
        switch (rule.getQcRuleItemType()) {
            case YESNO:
                if (option.getBooleanValue() == null) return QCInspectionResult.PENDING;
                if (rule.getQcRuleItemComparator() != QCRuleItemComparator.EQUAL ||
                    !(expected.equalsIgnoreCase("true") || expected.equalsIgnoreCase("false")))
                    throw QCException.raiseException("Invalid Yes/No acceptance standard.");
                pass = option.getBooleanValue() == Boolean.parseBoolean(expected); break;
            case NUMBER:
                if (option.getDoubleValue() == null) return QCInspectionResult.PENDING;
                double actual = option.getDoubleValue(), target;
                try { target = Double.parseDouble(expected); } catch (NumberFormatException ex) { throw QCException.raiseException("Invalid numeric acceptance standard."); }
                if (!Double.isFinite(actual) || !Double.isFinite(target)) throw QCException.raiseException("A finite measurement is required.");
                switch (rule.getQcRuleItemComparator()) {
                    case EQUAL: pass = actual == target; break;
                    case GREAT_THAN: pass = actual > target; break;
                    case LESS_THAN: pass = actual < target; break;
                    case GREAT_OR_EQUAL: pass = actual >= target; break;
                    case LESS_OR_EQUAL: pass = actual <= target; break;
                    default: throw QCException.raiseException("Invalid numeric comparator.");
                } break;
            case STRING:
                if (option.getStringValue() == null || option.getStringValue().trim().isEmpty()) return QCInspectionResult.PENDING;
                if (rule.getQcRuleItemComparator() == QCRuleItemComparator.EQUAL) pass = option.getStringValue().equals(expected);
                else if (rule.getQcRuleItemComparator() == QCRuleItemComparator.LIKE)
                    pass = expected.trim().toLowerCase(Locale.ROOT).contains(option.getStringValue().trim().toLowerCase(Locale.ROOT));
                else throw QCException.raiseException("Invalid text comparator.");
                break;
            default: throw QCException.raiseException("Unsupported inspection type.");
        }
        return pass ? QCInspectionResult.PASS : QCInspectionResult.FAIL;
    }
    private static void reject(String message) { throw QCException.raiseException(message); }
}

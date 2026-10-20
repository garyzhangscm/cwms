package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Component;

/** Capture the actual registrant after the inherited auditing listener.
 * Only new samples are stamped; edits must retain the original registrant.
 * UserService uses the application's existing gateway identity convention.
 */
@Component
public class WorkOrderQCSampleRegistrationListener {
    private final UserService userService;

    public WorkOrderQCSampleRegistrationListener(UserService userService) {
        this.userService = userService;
    }

    @PrePersist
    public void recordRegistrant(WorkOrderQCSample sample) {
        String username = userService.getCurrentUserName();
        if (username == null || username.trim().isEmpty()
                || "CWMS-USER".equalsIgnoreCase(username.trim())
                || "anonymousUser".equalsIgnoreCase(username.trim())
                || "Anonymous".equalsIgnoreCase(username.trim())) {
            throw WorkOrderException.raiseException(
                "The QC sample registrant could not be identified. Please sign in again.");
        }
        sample.setCreatedBy(username.trim());
    }
}

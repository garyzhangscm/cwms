package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public String getCurrentUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User getCurrentUser(Long companyId) {
        return resourceServiceRestemplateClient.getUserByUsername(companyId, getCurrentUserName());
    }
}

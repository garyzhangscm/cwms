package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.Role;
import com.garyzhangscm.cwms.inventory.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public String getCurrentUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    public User getCurrentUser() {
        return resourceServiceRestemplateClient.getUserByUsername(getCurrentUserName());
    }
}

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.Objects;

@Service
public class UserService {

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Autowired
    HttpServletRequest request;

    public String getCurrentUserName() {
        if (Objects.nonNull(request) && Strings.isNotBlank(request.getHeader("username"))) {

            return request.getHeader("username");
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User getCurrentUser(Long companyId) {
        return resourceServiceRestemplateClient.getUserByUsername(companyId, getCurrentUserName());
    }
}

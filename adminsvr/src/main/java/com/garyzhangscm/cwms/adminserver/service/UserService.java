package com.garyzhangscm.cwms.adminserver.service;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Service
public class UserService {

    @Autowired
    HttpServletRequest request;

    public String getCurrentUserName() {
        if (Objects.nonNull(request) && Strings.isNotBlank(request.getHeader("username"))) {

            return request.getHeader("username");
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}

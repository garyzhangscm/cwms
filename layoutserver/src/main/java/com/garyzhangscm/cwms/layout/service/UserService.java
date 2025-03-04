package com.garyzhangscm.cwms.layout.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Service
public class UserService {
    @Autowired
    HttpServletRequest request;

    public String getCurrentUserName() {
        if (Objects.isNull(request)) {
            return "ANONYMOUS";
        }
        return request.getHeader("username");
    }
}

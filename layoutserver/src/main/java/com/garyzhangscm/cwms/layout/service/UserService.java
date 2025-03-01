package com.garyzhangscm.cwms.layout.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class UserService {
    @Autowired
    HttpServletRequest request;

    public String getCurrentUserName() {
        return request.getHeader("username");
    }
}

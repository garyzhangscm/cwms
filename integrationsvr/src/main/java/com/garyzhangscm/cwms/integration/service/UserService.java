package com.garyzhangscm.cwms.integration.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Service
public class UserService {

    public String getCurrentUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

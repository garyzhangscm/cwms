package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.KafkaReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static Map<String, ServletRequestAttributes> userServletRequestAttributes = new HashMap<>();

    public String getCurrentUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public void addUserServletRequestAttribute(String token, ServletRequestAttributes servletRequestAttributes) {
        userServletRequestAttributes.put(token, servletRequestAttributes);
        logger.debug("add token {} to the map", token);
        logger.debug("Now we have keys  in the map");
        for (Map.Entry<String, ServletRequestAttributes> stringServletRequestAttributesEntry : userServletRequestAttributes.entrySet()) {
            logger.debug(">> 1 {}", stringServletRequestAttributesEntry.getKey());
        }

    }
    public ServletRequestAttributes getUserServletRequestAttribute(String token) {
        logger.debug("start to get key {} out of the map", token);
        for (Map.Entry<String, ServletRequestAttributes> stringServletRequestAttributesEntry : userServletRequestAttributes.entrySet()) {
            logger.debug(">> 2 {}", stringServletRequestAttributesEntry.getKey());
        }
        return userServletRequestAttributes.get(token);
    }
}

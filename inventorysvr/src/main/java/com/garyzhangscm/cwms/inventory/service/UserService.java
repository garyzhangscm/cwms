package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.User;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    HttpServletRequest request;


    private static Map<String, ServletRequestAttributes> userServletRequestAttributes = new HashMap<>();


    public String getCurrentUserName() {
        if (Objects.nonNull(request) && Strings.isNotBlank(request.getHeader("username"))) {

            return request.getHeader("username");
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
    public User getCurrentUser(Long companyId) {
        return resourceServiceRestemplateClient.getUserByUsername(companyId, getCurrentUserName());
    }
     **/
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

    public User getCurrentUser(Long companyId) {
        return resourceServiceRestemplateClient.getUserByUsername(companyId, getCurrentUserName());
    }

}

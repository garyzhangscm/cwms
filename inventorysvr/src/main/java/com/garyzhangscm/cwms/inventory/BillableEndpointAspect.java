package com.garyzhangscm.cwms.inventory;

import com.garyzhangscm.cwms.inventory.clients.KafkaSender;
import com.garyzhangscm.cwms.inventory.model.BillableRequest;
import com.garyzhangscm.cwms.inventory.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Aspect // indicate the component is used for aspect
@Component
public class BillableEndpointAspect   {
    private static Logger logger = LoggerFactory
            .getLogger(BillableEndpointAspect.class);

    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private UserService userService;

    @Autowired
    private KafkaSender kafkaSender;

    // aspect method who have the annotation @Delegate
    @Before(value = "@annotation(com.garyzhangscm.cwms.inventory.model.BillableEndpoint)")
    public void handle(JoinPoint joinPoint) throws Exception {

        // Disable the billable request for now. We will do nothing
        // createBillableRequest(httpServletRequest);
        logger.debug("We will disable the billable request handler at this moment");

    }

    private void createBillableRequest(HttpServletRequest httpServletRequest) throws IOException {

        // get the request's parameters,
        // normally we will have warehouse id or company id in the url
        Enumeration<String> enumeration = httpServletRequest.getParameterNames();
        Map<String, String> parametersMap = new HashMap<>();
        while(enumeration.hasMoreElements()) {
            String parameter = enumeration.nextElement();
            parametersMap.put(parameter, httpServletRequest.getParameter(parameter));

        }



        String requestBody = "";
        /**
         if ("POST".equalsIgnoreCase(httpServletRequest.getMethod()) ||
         "PUT".equalsIgnoreCase(httpServletRequest.getMethod()))
         {
         requestBody = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
         }
         **/

        String authorization = httpServletRequest.getHeader("Authorization");

        Long companyId = null;
        try {
            companyId =
                    parametersMap.containsKey("companyId") ?
                            Long.parseLong(parametersMap.get("companyId")) :
                            (Objects.isNull(httpServletRequest.getHeader("companyId")) ||
                                    Strings.isBlank(httpServletRequest.getHeader("companyId"))) ?
                                    null : Long.parseLong(httpServletRequest.getHeader("companyId"));
        }
        catch (Exception ex) {
            logger.debug("error while get company id for billable request: {}",
                    ex.getMessage());

        }
        Long warehouseId = null;
        try {
            warehouseId = parametersMap.containsKey("warehouseId") ?
                        Long.parseLong(parametersMap.get("warehouseId")) :
                        (Objects.isNull(httpServletRequest.getHeader("warehouseId")) ||
                            Strings.isBlank(httpServletRequest.getHeader("warehouseId"))) ?
                            null : Long.parseLong(httpServletRequest.getHeader("warehouseId"));
        }
        catch (Exception ex) {
            logger.debug("error while get warehouse id for billable request: {}",
                    ex.getMessage());

        }
        if (authorization.startsWith("Bearer")) {
            authorization = authorization.substring(7).trim();
        }
        BillableRequest billableRequest = new BillableRequest(
                companyId,
                warehouseId,
                "inventory_service", //serviceName
                httpServletRequest.getRequestURI(), //webAPIEndpoint
                httpServletRequest.getMethod(), // method
                parametersMap.toString(), // parameters
                requestBody, // request body
                userService.getCurrentUserName(),
                httpServletRequest.getHeader("gzcwms-correlation-id"),
                1.0,
                authorization
        );
        // logger.debug("  ===============  Billble Request   ===========");
        // logger.debug(billableRequest.toString());

        kafkaSender.send(billableRequest);
    }
}
package com.garyzhangscm.cwms.outbound;

import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.ClientRestriction;
import com.garyzhangscm.cwms.outbound.model.User;
import com.garyzhangscm.cwms.outbound.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.outbound.service.UserService;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Aspect // indicate the component is used for aspect
@Component
public class ClientValidationEndpointAspect {
    private static Logger logger = LoggerFactory
            .getLogger(ClientValidationEndpointAspect.class);

    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private UserService userService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    // aspect method who have the annotation @Delegate
    @Around(value = "@annotation(com.garyzhangscm.cwms.outbound.model.ClientValidationEndpoint)")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {


        Object[] arguments = joinPoint.getArgs();

        for(int i = 0; i < arguments.length; i++) {

            if (arguments[i] instanceof ClientRestriction) {
                arguments[i] = getClientAccessRestriction();
                logger.debug("will apply client access restriction: {}",
                        arguments[i]);
            }
        }

        return joinPoint.proceed(arguments);
    }

    /**
     * Setup the clients that the user has access
     */
    private ClientRestriction getClientAccessRestriction()  {

        logger.debug("httpServletRequest.getRequestURL(): {}", httpServletRequest.getRequestURL());

        // let's check if the current user can access the non 3pl data
        Long warehouseId = Strings.isNotBlank(httpServletRequest.getHeader("warehouseId")) ?
                Long.parseLong(httpServletRequest.getHeader("warehouseId")) :
                    Strings.isNotBlank(httpServletRequest.getParameter("warehouseId")) ?
                        Long.parseLong(httpServletRequest.getParameter("warehouseId")) :
                            null;

        logger.debug("get warehouse id {}", warehouseId);

        Long companyId = Strings.isNotBlank(httpServletRequest.getHeader("companyId")) ?
                Long.parseLong(httpServletRequest.getHeader("companyId")) :
                Strings.isNotBlank(httpServletRequest.getParameter("companyId")) ?
                        Long.parseLong(httpServletRequest.getParameter("companyId")) : null;

        logger.debug("get company id {}", warehouseId);

        // for some reason we can't get the warehouse id from the http request,
        // then we will allow the user to access non 3pl data(client id is null)

        // check if 3pl is enabled for the warehouse. By default it is disabled
        boolean threePartyLogisticsFlag = false;
        if (Objects.nonNull(warehouseId)) {
            WarehouseConfiguration warehouseConfiguration = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);

            if (Objects.isNull(warehouseConfiguration) || Boolean.TRUE.equals(warehouseConfiguration.getThreePartyLogisticsFlag())) {
                threePartyLogisticsFlag = true;
            }
        }


        if (!threePartyLogisticsFlag) {
            // in a non 3pl logistics environment, the user will always have access to
            // all information regardless of the client id configuration
            // other restriction will still be applied
            // return new ClientRestriction(false, true, true, "");
            return null;

        }

        // let's get all roles for the current user

        // if company id is not passed in, we won't be able to query
        // the user so we don't know whether the user has access to any client's data
        String accessibleClientIds = "";
        boolean nonClientDataAccessible = true;
        boolean allClientAccessible = true;
        if(Objects.nonNull(companyId)) {


            User user = userService.getCurrentUser(companyId);
            if (Boolean.TRUE.equals(user.getAdmin()) ||
                    Boolean.TRUE.equals(user.getSystemAdmin())) {
                // user is admin, admin has full access to everything inside the company
                // null restriction means there's no restriction
                return null;

            }
            if (Objects.nonNull(user)) {
                accessibleClientIds = user.getRoles().stream().filter(
                        role -> Boolean.TRUE.equals(role.getEnabled())
                ).map(
                                                    role -> role.getClientAccesses()
                                            )
                                            .flatMap(List::stream)
                                                    .map(roleClientAccess -> roleClientAccess.getClientId())
                                                    .map(String::valueOf)
                                                    .collect(Collectors.joining(","));

                // the user has access to the non client data as long as one role has the access
                nonClientDataAccessible = user.getRoles().stream().filter(
                        role -> Boolean.TRUE.equals(role.getEnabled())
                ).anyMatch(role -> Boolean.TRUE.equals(role.getNonClientDataAccessible()));

                // the user has access to the all client data as long as one role has the access
                allClientAccessible = user.getRoles().stream().filter(
                        role -> Boolean.TRUE.equals(role.getEnabled())
                ).anyMatch(role -> Boolean.TRUE.equals(role.getAllClientAccess()));
            }
        }


        return new ClientRestriction(true, nonClientDataAccessible, allClientAccessible, accessibleClientIds);


    }
}
/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.zuulsvr.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.zuulsvr.ResponseBodyWrapper;
import com.garyzhangscm.cwms.zuulsvr.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.exception.SystemFatalException;
import com.garyzhangscm.cwms.zuulsvr.exception.UnauthorizedException;
import com.garyzhangscm.cwms.zuulsvr.model.Company;
import com.garyzhangscm.cwms.zuulsvr.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Access controller based on url.
 */
@Component
public class UrlAccessControllerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlAccessControllerFilter.class);


    @Autowired
    private JwtService jwtService;


    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Value("auth.jwt.inner_call.token")
    private String innerCallJWTToken;


    // for single company server(server that host by the company, not
    // on cloud), we will ignore the company validation
    @Value("${site.company.singleCompany:false}")
    private Boolean singleCompanySite;


    // URL that is accessible even if the user is not logged in
    private static final String[] URL_WHITE_LIST = new String[]{
            "/api/integration/tiktok/webhook",
            "/api/integration/shopify/shop-oauth",
            "/api/layout/companies",
            "/api/layout/warehouses",
            "/api/layout/warehouse-configuration/by-warehouse",
            "/api/auth/users/username-by-token"
    };

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;


        logger.debug("current request URL: " + httpServletRequest.getRequestURI());

        try {

            validateAccess((HttpServletRequest)servletRequest);
            filterChain.doFilter(httpServletRequest, servletResponse);
        }
        catch (SystemFatalException ex) {
            logger.debug("Get SystemFatalException while validate the URL {}",
                    httpServletRequest.getRequestURI());
            ex.printStackTrace();
            // ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            sendErrorToClient(servletResponse, 403, ex.getMessage());

        }
        catch(UnauthorizedException ex) {
            logger.debug("Get UnauthorizedException while validate the URL {}",
                    httpServletRequest.getRequestURI());
            ex.printStackTrace();
            // ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            sendErrorToClient(servletResponse, 401, ex.getMessage());
        }

    }

    private void sendErrorToClient(ServletResponse servletResponse, int errorCode, String errorMessage) throws IOException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;


        httpServletResponse.setStatus(HttpStatus.OK.value());
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        logger.info("Adding CORS Headers ........................");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpServletResponse.setHeader("Access-Control-Max-Age", "3600");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER,Content-Type,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
        httpServletResponse.addHeader("Access-Control-Expose-Headers", "xsrf-token");

        logger.debug("start to return 200 http code with 401 error to the user");
        String responseMessage =  new ObjectMapper().writeValueAsString(
                ResponseBodyWrapper.error(errorCode, errorMessage)
        );
        logger.debug("responseMessage:\n{}", responseMessage);

        httpServletResponse.getOutputStream().write(responseMessage.getBytes(StandardCharsets.UTF_8));
    }


    private void validateAccess(HttpServletRequest httpServletRequest)  {


        logger.debug("Start to validate http access");
        if (isUrlInWhiteList(httpServletRequest.getRequestURI())) {
            logger.debug("URL {} is in the white list, pass the validation",
                    httpServletRequest.getRequestURI());
            return;
        }

        // first, we will check if we have token in the http header
        String token = httpServletRequest.getHeader("Authorization");
        logger.debug("token: " + token);

        if (Strings.isBlank(token)) {
            // there's no token passed in,
            // we will leave it to Oauth2 and spring security
            // to handle the authentication

            // normally if the end point doesn't require
            // token authentication, then it means there's no
            // security validation needed for the end point

            throw SystemFatalException.raiseException("user is not logged in");
        }

        if (token.startsWith("Bearer")) {
            token = token.substring(7).trim();
        }

        if (token.equals(innerCallJWTToken)) {
            logger.debug("current call is a inner http call, ignore the validation");
            return;
        }

        // check if the current user is a sys admin.

        logger.debug("start to validate warehouse and company access by token {}", token);
        // get the request's parameters,
        // normally we will have warehouse id or company id in the url
        // if there's no parameters, let's just ignore it for now
        Long companyId = getLongValueFromRequest("companyId", httpServletRequest);
        Long warehouseId = getLongValueFromRequest("warehouseId", httpServletRequest);

        logger.debug("Get company ID {} and warehouse id {} from the http request",
                Objects.isNull(companyId) ? "N/A" : companyId,
                Objects.isNull(warehouseId) ? "N/A" : warehouseId);

        if (Objects.isNull(companyId)) {
            // if company ID is not passed in, then see if the company code is passed in
            String companyCode = getStringValueFromRequest("companyCode", httpServletRequest);
            logger.debug("company code: {}", companyCode);
            if (Strings.isNotBlank(companyCode)) {
                Company company = layoutServiceRestemplateClient.getCompanyByCode(companyCode);
                if (Objects.nonNull(company)) {
                    logger.debug("we find the company by code {}, let's set the company id to {}",
                            companyCode, company.getId());
                    companyId =  company.getId() ;
                }
            }
        }

        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw SystemFatalException.raiseException("both warehouse id and company id is null, wrong URL request");
        }
        else if (Objects.isNull(companyId)) {
            // in case the company ID is not pass in, see if we can get from the warehouse id
            logger.debug("let's see if we can find the company id from warehouse id {}", warehouseId);

            companyId = layoutServiceRestemplateClient.getCompanyIdByWarehouseId(warehouseId).longValue();
            if (Objects.isNull(companyId)) {

                throw SystemFatalException.raiseException("can't get company ID from the request, not able to validate the request");
            }
        }

        logger.debug("check if token {} has access to company id {}", token, companyId);
        // ok, now let's get the username out of the toke and company. We will first validate
        // if the user is a system admin. System admin will have a full access to everything and
        // ignore any restriction in the system
        // String username = authServiceRestemplateClient.getUserNameByToken(companyId, token);
        String username = jwtService.extractUsername(token);

        if (Strings.isBlank(username)) {

            // as long as we have the token, we should be able to get the username
            throw SystemFatalException.raiseException("can't get username from the token");
        }
        // validate if the user is a system admin
        Boolean isSystemAdmin = resourceServiceRestemplateClient.isSystemAdmin(username);
        if (Boolean.TRUE.equals(isSystemAdmin)) {
            logger.debug("Current user {} is system admin, skip any validation. System admin is allowed to access any resource",
                    username);
            return;
        }

        // validateCompanyAccess(httpServletRequest.getRequestURL().toString(), companyId, token);
        if (jwtService.extractCompanyId(token).equals(companyId)) {
            return;
        }

        valdiateWarehouseAccess(companyId, warehouseId, token);
    }

    private boolean isUrlInWhiteList(String requestURI) {
        return Arrays.stream(URL_WHITE_LIST).anyMatch(
                whiteListUrl ->
                        // whiteListUrl.equalsIgnoreCase(requestURI)
                        requestURI.toLowerCase(Locale.ROOT).startsWith(whiteListUrl.toLowerCase(Locale.ROOT))
        );
    }

    /**
     * Get the value of the name from the http request
     * we will try from the parameters, then the header
     * @param name
     * @param httpServletRequest
     * @return
     */
    private String getStringValueFromRequest(String name, HttpServletRequest httpServletRequest) {

        // see if we can find from the parameters
        Map<String, String[]> parameters =  httpServletRequest.getParameterMap();

        if (parameters.containsKey(name) &&
                parameters.get(name).length > 0) {
            return parameters.get(name)[0];
        }

        // see if we can find from the header
        return httpServletRequest.getHeader(name);

    }
    private Long getLongValueFromRequest(String name, HttpServletRequest httpServletRequest) {

        // see if we can find from the parameters
        Map<String, String[]> parameters =  httpServletRequest.getParameterMap();

        if (parameters.containsKey(name) &&
                parameters.get(name).length > 0) {
            return Long.parseLong(parameters.get(name)[0]);
        }

        // see if we can find from the header
        String value = httpServletRequest.getHeader(name);
        return Strings.isBlank(value) ? null :  Long.parseLong(value);

    }
    private void valdiateWarehouseAccess(Long companyId, Long warehouseId, String token) {
    }


}

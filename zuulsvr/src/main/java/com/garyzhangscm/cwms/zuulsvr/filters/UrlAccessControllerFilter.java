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

import com.garyzhangscm.cwms.zuulsvr.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.exception.SystemFatalException;
import com.garyzhangscm.cwms.zuulsvr.exception.UnauthorizedException;
import com.garyzhangscm.cwms.zuulsvr.model.Company;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


/**
 * Access controller based on url.
 */
@Component
public class UrlAccessControllerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UrlAccessControllerFilter.class);


    @Autowired
    private AuthServiceRestemplateClient authServiceRestemplateClient;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    // for single company server(server that host by the company, not
    // on cloud), we will ignore the company validation
    @Value("${site.company.singleCompany:false}")
    private Boolean singleCompanySite;



    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;


        logger.debug("current request URL: " + httpServletRequest.getRequestURI());

        try {

            validateAccess((HttpServletRequest)servletRequest);
        }
        catch (SystemFatalException ex) {
            ex.printStackTrace();
            ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
        catch(UnauthorizedException ex) {
            ex.printStackTrace();
            ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        }

        filterChain.doFilter(httpServletRequest, servletResponse);
    }




    private void validateAccess(HttpServletRequest httpServletRequest)  {


        logger.debug("Start to validate http access");
        String innerCall = httpServletRequest.getHeader("innerCall");
        // logger.debug("innerCall? : {}", innerCall);
        if ("true".equalsIgnoreCase(innerCall)) {
            logger.debug("Skip validation if the call is from inner service");
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
            logger.debug("There's no token in the http header, we will pass the access validation and leave it to OAuth2 and spring security");
            return;
        }

        if (token.startsWith("Bearer")) {
            token = token.substring(7).trim();
        }

        logger.debug("start to validate warehouse and company access by token {}", token);
        // get the request's parameters,
        // normally we will have warehouse id or company id in the url
        // if there's no parameters, let's just ignore it for now
        Long companyId = getLongValueFromRequest("companyId", httpServletRequest);
        Long warehouseId = getLongValueFromRequest("warehouseId", httpServletRequest);

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

            companyId = layoutServiceRestemplateClient.getCompanyId(warehouseId);
            if (Objects.isNull(companyId)) {

                throw SystemFatalException.raiseException("can't get company ID from the request, not able to validate the request");
            }
        }

        logger.debug("check if token {} has access to company id {}", token, companyId);
        validateCompanyAccess(httpServletRequest.getRequestURL().toString(), companyId, token);

        valdiateWarehouseAccess(companyId, warehouseId, token);
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

    private void validateCompanyAccess(String url, Long companyId, String token) {

        if (Boolean.TRUE.equals(singleCompanySite)) {
            // this is a single company server, which normally
            // means the server is host by the customer, not on public
            // cloud, then we won't need to validate the company
            logger.debug("skip the company validation if this is a single company server");
            return;
        }
        if (!authServiceRestemplateClient.validateCompanyAccess(companyId, token)) {

            logger.debug("access to url {} fail, the user {} can't access the company {}",
                    url, token, companyId);
            throw UnauthorizedException.raiseException("Erorr: current user doesn't have access to the company information");
        }
        logger.debug("token {} has access to the company {}", token, companyId);
    }
}

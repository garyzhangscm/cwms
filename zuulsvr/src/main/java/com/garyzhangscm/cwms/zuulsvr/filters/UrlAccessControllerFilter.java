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

import brave.Tracer;
import com.garyzhangscm.cwms.zuulsvr.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.zuulsvr.exception.SystemFatalException;
import com.garyzhangscm.cwms.zuulsvr.model.Company;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;


/**
 * Access controller based on url.
 */
@Component
public class UrlAccessControllerFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(UrlAccessControllerFilter.class);

    private static final int      FILTER_ORDER =  1;
    private static final boolean  SHOULD_FILTER=true;

    @Autowired
    private AuthServiceRestemplateClient authServiceRestemplateClient;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    // for single company server(server that host by the company, not
    // on cloud), we will ignore the company validation
    @Value("${site.company.singleCompany:false}")
    private Boolean singleCompanySite;

    /****
    @Autowired
    FilterUtils filterUtils;
***/
    @Autowired
    Tracer tracer;
    @Override
    public String filterType() {
        return FilterUtils.PRE_FILTER_TYPE;
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }



    public Object run() {

        RequestContext requestContext = RequestContext.getCurrentContext();
        // logger.debug("ctx.getRequest().getContextPath(): " + requestContext.getRequest().getContextPath());
        // logger.debug("ctx.getRequest().getPathInfo(): " + requestContext.getRequest().getPathInfo());
        // logger.debug("ctx.getRequest().getPathTranslated(): " + requestContext.getRequest().getPathTranslated());
        // logger.debug("ctx.getRequest().getRequestURI(): " + requestContext.getRequest().getRequestURI());
        // logger.debug("ctx.getRequest().getServletPath(): " + requestContext.getRequest().getServletPath());
        // logger.debug("ctx.getRequest().getRemoteAddr(): " + requestContext.getRequest().getRemoteAddr());
        // logger.debug("ctx.getRequest().getRequestURL().toString(): " + requestContext.getRequest().getRequestURL().toString());

        validateAccess(requestContext);


        /****
         * not valid the URL for now
        if (!resourceServiceRestemplateClient.validateURLAccess(ctx.getRequest().getRequestURI())) {
            logger.debug("The current user doesn't have access to the url");
            throw UserOperationException.raiseException("The current user doesn't have access to the url");
        }
         ***/
        return null;
    }


    private void validateAccess(RequestContext requestContext)  {


        logger.debug("Start to validate http access");
        String innerCall = requestContext.getRequest().getHeader("innerCall");
        logger.debug("innerCall? : {}", innerCall);
        if ("true".equalsIgnoreCase(innerCall)) {
            logger.debug("Skip validation if the call is from inner service");
            return;
        }
        // first, we will check if we have token in the http header
        String token = requestContext.getRequest().getHeader("Authorization");
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
        List<String> warehouseIdParameters =
                Objects.isNull(requestContext.getRequestQueryParams()) ?
                        null : requestContext.getRequestQueryParams().get("warehouseId");
        List<String> companyIdParameters =
                Objects.isNull(requestContext.getRequestQueryParams()) ?
                        null : requestContext.getRequestQueryParams().get("companyId");

        Long companyId = null;
        try {
            companyId =
                    Objects.nonNull(companyIdParameters) && companyIdParameters.size() > 0 ?
                            Long.parseLong(companyIdParameters.get(0)) :
                            (Objects.isNull(requestContext.getRequest().getHeader("companyId")) ||
                                    Strings.isBlank(requestContext.getRequest().getHeader("companyId"))) ?
                                    null : Long.parseLong(requestContext.getRequest().getHeader("companyId"));
        }
        catch (Exception ex) {
            logger.debug("error while get company id for billable request: {}",
                    ex.getMessage());

        }
        if (Objects.isNull(companyId)) {
            // if company Id is not passed in but we can find the company code, then
            // we will get the company ID from the code
            logger.debug("company id is not passed in, let's see if we can find the company code");
            List<String> companyCodeParameters = requestContext.getRequestQueryParams().get("companyCode");
            String companyCode =
                    Objects.nonNull(companyCodeParameters) && companyCodeParameters.size() > 0 ?
                            companyCodeParameters.get(0) :
                            (Objects.isNull(requestContext.getRequest().getHeader("companyCode")) ||
                                    Strings.isBlank(requestContext.getRequest().getHeader("companyCode"))) ?
                                    "" : requestContext.getRequest().getHeader("companyCode");
            logger.debug("company code: {}", companyCode);
            if (Strings.isNotBlank(companyCode)) {
                Company company = layoutServiceRestemplateClient.getCompanyByCode(companyCode);
                if (Objects.nonNull(company)) {
                    logger.debug("we find the company by code {}, let's set the company id to {}",
                            companyCode, company.getId());
                    companyId = company.getId();
                }
            }
        }
        // for now, we won't validate warehouse
        Long warehouseId = null;
        try {
            warehouseId =
                    Objects.nonNull(warehouseIdParameters) && warehouseIdParameters.size() > 0 ?
                            Long.parseLong(warehouseIdParameters.get(0)) :
                            (Objects.isNull(requestContext.getRequest().getHeader("warehouseId")) ||
                                    Strings.isBlank(requestContext.getRequest().getHeader("warehouseId"))) ?
                                    null : Long.parseLong(requestContext.getRequest().getHeader("warehouseId"));
        }
        catch (Exception ex) {
            logger.debug("error while get warehouse id for request: {}",
                    ex.getMessage());

        }
        // check if the user is from the company

        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw SystemFatalException.raiseException("both warehouse id and company id is null, wrong URL request");
        }
        else if (Objects.isNull(companyId)) {
            // in case the company ID is not pass in, see if we can get from the warehouse id
            logger.debug("let's see if we can find the company id from warehouse id {}", warehouseId);

            companyId = layoutServiceRestemplateClient.getCompanyId(warehouseId);
            if (Objects.isNull(companyId)) {
                //

                throw SystemFatalException.raiseException("can't get company ID from the request, not able to validate the request");
            }
        }

        logger.debug("check if token {} has access to company id {}", token, companyId);
        validateCompanyAccess(requestContext.getRequest().getRequestURL().toString(), companyId, token);

        valdiateWarehouseAccess(companyId, warehouseId, token);
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
            throw SystemFatalException.raiseException("Erorr: current user doesn't have access to the company information");
        }
        logger.debug("token {} has access to the company {}", token, companyId);
    }
}

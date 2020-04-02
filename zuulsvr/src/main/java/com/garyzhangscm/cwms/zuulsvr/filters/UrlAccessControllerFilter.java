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
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Access controller based on url.
 */
@Component
public class UrlAccessControllerFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(UrlAccessControllerFilter.class);

    private static final int      FILTER_ORDER =  1;
    private static final boolean  SHOULD_FILTER=true;

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

        RequestContext ctx = RequestContext.getCurrentContext();
        logger.debug("ctx.getRequest().getContextPath(): " + ctx.getRequest().getContextPath());
        logger.debug("ctx.getRequest().getPathInfo(): " + ctx.getRequest().getPathInfo());
        logger.debug("ctx.getRequest().getPathTranslated(): " + ctx.getRequest().getPathTranslated());
        logger.debug("ctx.getRequest().getRequestURI(): " + ctx.getRequest().getRequestURI());
        logger.debug("ctx.getRequest().getServletPath(): " + ctx.getRequest().getServletPath());
        logger.debug("ctx.getRequest().getRemoteAddr(): " + ctx.getRequest().getRemoteAddr());
        logger.debug("ctx.getRequest().getRequestURL().toString(): " + ctx.getRequest().getRequestURL().toString());
        /****
         * not valid the URL for now
        if (!resourceServiceRestemplateClient.validateURLAccess(ctx.getRequest().getRequestURI())) {
            logger.debug("The current user doesn't have access to the url");
            throw UserOperationException.raiseException("The current user doesn't have access to the url");
        }
         ***/
        return null;
    }
}

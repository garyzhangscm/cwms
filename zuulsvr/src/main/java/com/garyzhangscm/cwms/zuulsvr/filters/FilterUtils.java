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

import com.netflix.zuul.context.RequestContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
public class FilterUtils {

    public static final String CORRELATION_ID = "gzcwms-correlation-id";
    public static final String USERNAME = "gzcwms-username";

    public static final String AUTH_TOKEN     = "Authorization";

    public static final String PRE_FILTER_TYPE = "pre";

    public String getCorrelationId(){
        RequestContext ctx = RequestContext.getCurrentContext();

        if (ctx.getRequest().getHeader(CORRELATION_ID) !=null) {
            return ctx.getRequest().getHeader(CORRELATION_ID);
        }
        else{
            return  ctx.getZuulRequestHeaders().get(CORRELATION_ID);
        }
    }

    public void setCorrelationId(String correlationId){
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulRequestHeader(CORRELATION_ID, correlationId);
    }

    public String getAuthToken(){
        RequestContext ctx = RequestContext.getCurrentContext();

        if (ctx.getRequest().getHeader(AUTH_TOKEN) !=null) {
            return getAuthToken(ctx.getRequest().getHeader(AUTH_TOKEN));
        }
        else{
            return getAuthToken(ctx.getZuulRequestHeaders().get(AUTH_TOKEN));
        }
    }

    private String getAuthToken(String bearerAuthToken) {
        // The token will be passed in as Bearer xxxxx-xxxxx-xxxx-xxxx

        if (Strings.isBlank(bearerAuthToken) ||
                !bearerAuthToken.startsWith("Bearer")) {
            return "";
        }
        String[] tokenValues = bearerAuthToken.split(" ");
        if (tokenValues.length != 2) {
            return "";
        }
        return tokenValues[1];

    }



}

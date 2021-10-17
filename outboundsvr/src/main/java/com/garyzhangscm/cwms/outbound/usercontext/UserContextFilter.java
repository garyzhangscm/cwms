package com.garyzhangscm.cwms.outbound.usercontext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class UserContextFilter implements Filter {

    private static final Logger logger =  LoggerFactory.getLogger(UserContextFilter.class);
    @Override
    public void doFilter(ServletRequest servletRequest,
                                   ServletResponse servletResponse,
                                   FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        UserContextHolder.getContext().setCorrelationId(
                httpServletRequest.getHeader(UserContext.CORRELATION_ID));
        UserContextHolder.getContext().setUserId(
                httpServletRequest.getHeader(UserContext.USER_ID));
        UserContextHolder.getContext().setAuthToken(
                httpServletRequest.getHeader(UserContext.AUTH_TOKEN));
        UserContextHolder.getContext().setOrgId(
                httpServletRequest.getHeader(UserContext.ORG_ID));
        UserContextHolder.getContext().setRfCode(
                httpServletRequest.getHeader(UserContext.RF_CODE));

        UserContextHolder.getContext().setCompanyId(
                httpServletRequest.getHeader(UserContext.COMPANY_ID));

        UserContextHolder.getContext().setWarehouseId(
                httpServletRequest.getHeader(UserContext.WAREHOUSE_ID));

        filterChain.doFilter(httpServletRequest, servletResponse);
    }


}

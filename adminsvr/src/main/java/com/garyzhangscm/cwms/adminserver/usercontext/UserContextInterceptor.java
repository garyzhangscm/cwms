package com.garyzhangscm.cwms.adminserver.usercontext;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserContextInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserContextInterceptor.class);
    @Value("${auth.jwt.inner_call.token}")
    private String innerCallJWTToken;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.add(UserContext.CORRELATION_ID,
                UserContextHolder.getContext().getCorrelationId());

        headers.add(UserContext.RF_CODE,
                UserContextHolder.getContext().getRfCode());
        headers.add(UserContext.COMPANY_ID,
                UserContextHolder.getContext().getCompanyId());
        headers.add(UserContext.WAREHOUSE_ID,
                UserContextHolder.getContext().getWarehouseId());

        if (Strings.isBlank(UserContextHolder.getContext().getAuthToken())) {
            logger.debug("# add inner call token {} ", innerCallJWTToken);
            headers.add(UserContext.AUTH_TOKEN, "Bearer " + innerCallJWTToken);
        }
        else {
            logger.debug("# add the jwt token {} in the same transaction ",
                    UserContextHolder.getContext().getAuthToken());
            headers.add(UserContext.AUTH_TOKEN, UserContextHolder.getContext().getAuthToken());
        }
        return execution.execute(request, body);
    }
}

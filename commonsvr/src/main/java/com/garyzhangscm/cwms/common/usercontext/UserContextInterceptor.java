package com.garyzhangscm.cwms.common.usercontext;

import org.apache.logging.log4j.util.Strings;
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
        headers.add("innerCall", "true");

        if (Strings.isBlank(UserContextHolder.getContext().getAuthToken())) {
            headers.add(UserContext.AUTH_TOKEN, "Bearer " + innerCallJWTToken);
        }
        else {

            headers.add(UserContext.AUTH_TOKEN, UserContextHolder.getContext().getAuthToken());
        }
        return execution.execute(request, body);
    }
}

package com.garyzhangscm.cwms.dblink.usercontext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class UserContextInterceptor implements ClientHttpRequestInterceptor {

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

        headers.add(UserContext.AUTH_TOKEN, UserContextHolder.getContext().getAuthToken());
        return execution.execute(request, body);
    }
}

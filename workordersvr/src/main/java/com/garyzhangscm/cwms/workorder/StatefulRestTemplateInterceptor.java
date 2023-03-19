package com.garyzhangscm.cwms.workorder;

import com.garyzhangscm.cwms.workorder.clients.SiloRestemplateClient;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A interceptor that save cookie for silo http call
 */
public class StatefulRestTemplateInterceptor  implements ClientHttpRequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(StatefulRestTemplateInterceptor.class);
    private String cookie = "AWSALB=NrUtVeGurbC/uGEzT2vJZ4VhEiKL+Vy5yTgcVQqRC8dPwqjNtr0MFMVpaTBNWUnUkgYZ/vv1vxbRmaI+8E2qph/WC8eeBT123j7cqSMT+almCYaX7weaikiC03az; AWSALBCORS=NrUtVeGurbC/uGEzT2vJZ4VhEiKL+Vy5yTgcVQqRC8dPwqjNtr0MFMVpaTBNWUnUkgYZ/vv1vxbRmaI+8E2qph/WC8eeBT123j7cqSMT+almCYaX7weaikiC03az";
    // private String cookie = "";
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (Strings.isNotBlank(cookie)) {

            logger.debug("start to add cookie to the http call: \n{}",
                    cookie);
            request.getHeaders().add(HttpHeaders.COOKIE, cookie);
        }
        ClientHttpResponse response = execution.execute(request, body);

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        cookie = cookies.stream().map(c -> {

            String[] values = c.split(";");
            logger.debug("Get {} from cookie \n{}, >> of length {}",
                    values, c, values.length);
            if (values.length > 0) {
                return values[0];
            }
            return "";
        }).collect(Collectors.joining(";"));


        logger.debug("cookie is set to the new value after the http call: \n{}",
                cookie);
        return response;
    }
}

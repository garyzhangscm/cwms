package com.garyzhangscm.cwms.APIGateway;

import com.garyzhangscm.cwms.APIGateway.exception.UnauthorizedException;
import com.garyzhangscm.cwms.APIGateway.model.JWTToken;
import com.garyzhangscm.cwms.APIGateway.service.JwtService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;


@Component
// public class AccessControlFilter implements GlobalFilter {
public class AccessControlFilter implements GatewayFilter {

    final Logger logger =
            LoggerFactory.getLogger(AccessControlFilter.class);

    @Autowired
    private JwtService jwtService;


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
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        logger.info("Global Pre Filter executed");

        try {
            JWTToken jwtToken = validateCompanyAccess(exchange.getRequest());
            logger.debug("We are able to validate the token \n{} \n for the user {} for company  {}",
                    jwtToken.getToken(),
                    jwtToken.getUsername(),
                    jwtToken.getCompanyId());

            // we will add the user name to the http header, if not done yet
            if (!exchange.getRequest().getHeaders().containsKey("username") ||
                    exchange.getRequest().getHeaders().get("username").isEmpty()) {

                exchange.getRequest()
                        .mutate()
                        .headers(h -> h.set("username", jwtToken.getUsername()));
            }

        }
        catch (UnauthorizedException ex) {
            return this.onError(exchange,ex.getMessage() ,HttpStatus.FORBIDDEN);
        }
        return chain.filter(exchange);
    }

    private JWTToken validateCompanyAccess(ServerHttpRequest request) {
        logger.debug("Start to validate http access for  {} ",
                request.getURI().getPath());

        if (isUrlInWhiteList(request.getURI().getPath())) {
            logger.debug("URL {} is in the white list, pass the validation",
                    request.getURI().getPath());
            return JWTToken.EMPTY_TOKEN();
        }

        JWTToken jwtToken = getJWTTokenFromReqeuest(request);

        if (isInnerCall(jwtToken.getToken())) {
            logger.debug("pass the validation for any intra microservice call",
                    request.getURI().getPath());
            return jwtToken;
        }

        // make sure the token is issued for the right company id
        Long companyId = getLongValueFromRequest(request, "companyId");
        if (Objects.isNull(companyId)) {

            throw UnauthorizedException.raiseException("There's no company ID in the header");
        }

        if (jwtToken.getCompanyId() == -1 || companyId.equals(jwtToken.getCompanyId())) {
            return jwtToken;
        }
        throw UnauthorizedException.raiseException("current user " + jwtToken.getUsername() +
                " does not have access to the request company");





    }

    private JWTToken getJWTTokenFromReqeuest(ServerHttpRequest request) {

        if (!request.getHeaders().containsKey("Authorization")) {
            logger.debug("There's no JWT Token in the header, fail to access URL {}",
                    request.getURI().getPath());
            throw UnauthorizedException.raiseException("There's no JWT Token in the header");

        }

        String token = request.getHeaders().get("Authorization").get(0);
        logger.debug("JWT token: " + token);

        if (Strings.isBlank(token)) {
            logger.debug("There's no JWT Token in the header, fail to access URL {}",
                    request.getURI().getPath());
            throw UnauthorizedException.raiseException("There's no JWT Token in the header");
        }

        if (token.startsWith("Bearer")) {
            token = token.substring(7).trim();
        }

        JWTToken jwtToken = jwtService.extractToken(token);
        if (!jwtToken.isValid()) {

            throw UnauthorizedException.raiseException("current JWT Token is invalid");
        }
        if (jwtToken.isExpired()) {

            throw UnauthorizedException.raiseException("current JWT Token is expired");
        }
        return jwtToken;
    }


    private String getStringValueFromRequest(ServerHttpRequest request, String name) {

        // see if we can find from the parameters
        MultiValueMap<String, String> parameters =  request.getQueryParams();
        if (!parameters.containsKey(name)) {
            return null;
        }


        List<String> values = parameters.get(name);
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);

    }
    private Long getLongValueFromRequest(ServerHttpRequest request, String name) {
        String value = getStringValueFromRequest(request, name);
        if (Strings.isBlank(value)) {
            return null;
        }
        return Long.parseLong(value);

    }

    private boolean isInnerCall(String token) {
        return token.equals(innerCallJWTToken);
    }

    private boolean isUrlInWhiteList(String requestURI) {
        return Arrays.stream(URL_WHITE_LIST).anyMatch(
                whiteListUrl ->
                        // whiteListUrl.equalsIgnoreCase(requestURI)
                        requestURI.toLowerCase(Locale.ROOT).startsWith(whiteListUrl.toLowerCase(Locale.ROOT))
        );
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus)  {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}

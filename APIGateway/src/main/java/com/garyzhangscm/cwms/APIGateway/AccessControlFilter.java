package com.garyzhangscm.cwms.APIGateway;

import com.garyzhangscm.cwms.APIGateway.exception.UnauthorizedException;
import com.garyzhangscm.cwms.APIGateway.model.JWTToken;
import com.garyzhangscm.cwms.APIGateway.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
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

    @Value("${auth.jwt.inner_call.token}")
    private String innerCallJWTToken;


    // for single company server(server that host by the company, not
    // on cloud), we will ignore the company validation
    @Value("${site.company.singleCompany:false}")
    private Boolean singleCompanySite;


    // URL that is accessible even if the user is not logged in
    private static final String[] ANONYMOUS_ACCESSIBLE_URL_LIST = new String[]{
            "/api/integration/tiktok/webhook",
            "/api/integration/shopify/shop-oauth",
            "/api/layout/companies",
            "/api/layout/warehouses",
            "/api/layout/warehouse-configuration/by-warehouse",
            "/api/resource/assets/i18n",
            "/api/resource/site-information/default",
            "/api/auth/login",
            "/api/auth/users/username-by-token",
            // mobile related
            "/api/resource/mobile",
            "/api/resource/validate/rf",
            "/api/layout/validate/locations",
            "/api/resource/rf-app-version/latest-version",
            "/api/resource/assets/images/mobile",
            "/api/workorder/graphiql",
            "/api/inventory"
    };

    private static final String[] EMPTY_COMPANY_ID_ACCESSIBLE_URL_LIST = new String[]{
            "/api/layout/companies"
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
        catch (ExpiredJwtException ex) {
            return this.onError(exchange, "Login expired, please login again",HttpStatus.FORBIDDEN);
        }
        logger.debug("{} with parameters {} passed validation, forward to the micro service for handling",
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getQueryParams());


        return chain.filter(exchange);
    }

    private JWTToken validateCompanyAccess(ServerHttpRequest request) {
        logger.debug("Start to validate http access for  {} ",
                request.getURI().getPath());


        JWTToken jwtToken = getJWTTokenFromRequest(request);

        // at this point, if the jwt token is a fake token for
        // url in white list or inter microservice call,
        // the valid flag is already marked, we can simply
        // return here. otherwise, we will need to validate
        // if the jwt token has access to the company as well
        if (jwtToken.isValid()) {

            return jwtToken;
        }

        if (isEmptyCompanyIdAccessibleURL(request.getURI().getPath())) {
            logger.debug("No need to validate the company id for current url {}",
                    request.getURI().getPath());
            jwtToken.setValid(true);
            return jwtToken;
        }

        // make sure the token is issued for the right company id
        Long companyId = getLongValueFromRequest(request, "companyId");
        if (Objects.isNull(companyId)) {

            logger.debug("Current http request doesn't have company ID in the header ");
            throw UnauthorizedException.raiseException("There's no company ID in the header");
        }


        if (jwtToken.getCompanyId() == -1 || companyId.equals(jwtToken.getCompanyId())) {
            jwtToken.setValid(true);
            return jwtToken;
        }

        logger.debug("current user {} has access to company id {} in the jwt token but the " +
                "company id in the http request head is {}, validation fail",
                jwtToken.getUsername(),
                jwtToken.getCompanyId(),
                companyId);

        throw UnauthorizedException.raiseException("current user " + jwtToken.getUsername() +
                " does not have access to the request company");

    }

    private String getJWTTokenString(ServerHttpRequest request) {

        String token = "";
        if (request.getHeaders().containsKey("Authorization")) {
            token = request.getHeaders().get("Authorization").get(0);
            // token is "Bearer xxxxxxxxxxxxxxxxx"
            // we will get the actual JWT token
            token = token.substring(7).trim();
            logger.debug("Get token {} from http header", token);
        }
        else if (request.getQueryParams().containsKey("token")){
            token = request.getQueryParams().get("token").get(0);
            logger.debug("Get token {} from query string", token);
        }
        else {
            logger.debug("fail to get jwt token");
        }
        return token;


    }

    private JWTToken getJWTTokenFromRequest(ServerHttpRequest request) {

        String token  = getJWTTokenString(request);
        if (Strings.isBlank(token)) {
            logger.debug("There's no JWT Token in the http request header, " +
                    " let's see if the URL is in the white list");
            if (isAnonymousAccessibleURL(request.getURI().getPath())) {
                logger.debug("{} is in the white list, let's return an empty token",
                        request.getURI().getPath() );
                return JWTToken.EMPTY_TOKEN();
            }
            else {

                throw UnauthorizedException.raiseException("There's no JWT Token in the header");
            }
        }


        if (isInnerCall(token)) {
            logger.debug("this is an inner call, skip any validation");
            return JWTToken.INNER_CALL(token);
        }


        JWTToken jwtToken = jwtService.extractToken(token);

        if (Objects.isNull(jwtToken.getCompanyId())) {
            logger.debug("Current token doesn't have company id, validation fail");

            throw UnauthorizedException.raiseException("current JWT Token doesn't have company information");
        }
        if (jwtToken.isExpired()) {

            logger.debug("Current token is expired, validation fail");
            throw UnauthorizedException.raiseException("current JWT Token is expired");
        }
        return jwtToken;
    }


    private String getStringValueFromRequest(ServerHttpRequest request, String name) {
        // see if we can get from the header
        if (request.getHeaders().containsKey(name)) {
            logger.debug("we found {} in the http request header, return the value {}",
                    name, request.getHeaders().get(name));
            if (!request.getHeaders().get(name).isEmpty()) {
                return request.getHeaders().get(name).get(0);
            }
        }
        logger.debug("we can't find {} in the http request header, let's see if we can find from the query string",
                name);
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
        logger.debug("check token {} against inner call jwt token {}",
                token, innerCallJWTToken);
        logger.debug("> is inner call ? {}", token.equals(innerCallJWTToken));
        return token.equals(innerCallJWTToken);
    }

    private boolean isAnonymousAccessibleURL(String requestURI) {
        return Arrays.stream(ANONYMOUS_ACCESSIBLE_URL_LIST).anyMatch(
                whiteListUrl ->
                        // whiteListUrl.equalsIgnoreCase(requestURI)
                        requestURI.toLowerCase(Locale.ROOT).startsWith(whiteListUrl.toLowerCase(Locale.ROOT))
        );
    }

    private boolean isEmptyCompanyIdAccessibleURL(String requestURI) {
        return Arrays.stream(EMPTY_COMPANY_ID_ACCESSIBLE_URL_LIST).anyMatch(
                whiteListUrl ->
                        requestURI.toLowerCase(Locale.ROOT).startsWith(whiteListUrl.toLowerCase(Locale.ROOT))
        );
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus)  {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}

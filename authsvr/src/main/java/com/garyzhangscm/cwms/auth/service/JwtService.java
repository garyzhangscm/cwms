package com.garyzhangscm.cwms.auth.service;

import com.garyzhangscm.cwms.auth.exception.SystemFatalException;
import com.garyzhangscm.cwms.auth.model.JWTToken;
import com.garyzhangscm.cwms.auth.model.UserAuthentication;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class JwtService {

    final Logger logger =
            LoggerFactory.getLogger(JwtService.class);

    // Replace this with a secure key in a real application, ideally fetched from environment variables
    @Value("${auth.jwt.secret:5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437}")
    public String secret;
    @Value("${auth.jwt.token.expire_time_in_minutes:180}")
    public int jwtTokenExpireTimeInMinutes;

    // Generate token with given user name
    public String generateToken(Long companyId, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("companyId", companyId);
        return createToken(claims, userName);
    }

    // Create a JWT token with specified claims and subject (user name)
    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * jwtTokenExpireTimeInMinutes)) // Token valid for 30 minutes
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Get the signing key for JWT token
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extract the username from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract the expiration date from the token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract a claim from the token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from the token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if the token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Validate the token against user details and expiration
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public JWTToken extractToken(String token) {


        final Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        Date expireTime = claims.getExpiration();
        Long companyId = claims.containsKey("companyId") ?
                Long.parseLong(String.valueOf(claims.get("companyId")))
                :
                null;

        return new JWTToken(token, companyId, username, expireTime.before(new Date()), false);
    }

    public JWTToken getJWTTokenFromRequest(HttpServletRequest request) {

        String token  = getJWTTokenString(request);
        if (Strings.isBlank(token)) {

            throw SystemFatalException.raiseException("There's no JWT Token in the header");
        }


        return extractToken(token);
    }


    private String getJWTTokenString(HttpServletRequest request) {

        if (Objects.isNull(request.getHeaders("Authorization")) ||
                !request.getHeaders("Authorization").hasMoreElements()) {
            return "";
        }
        String token = request.getHeaders("Authorization").nextElement();
        logger.debug("JWT token: " + token);

        if (Strings.isBlank(token)) {
            return "";
        }

        if (token.startsWith("Bearer")) {
            return token.substring(7).trim();
        }
        else {
            return "";
        }


    }

}

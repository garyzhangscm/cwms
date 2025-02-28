package com.garyzhangscm.cwms.zuulsvr.service;

import com.garyzhangscm.cwms.zuulsvr.exception.SystemFatalException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.function.Function;

@Component
public class JwtService {
    // Replace this with a secure key in a real application, ideally fetched from environment variables
    @Value("${auth.jwt.secret:5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437}")
    public String secret;


    // Get the signing key for JWT token
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extract the username from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractCompanyId(String token) {
        final Claims claims = extractAllClaims(token);
        if (claims.containsKey("companyId")) {
            return Long.parseLong(String.valueOf(claims.get("companyId")));
        }
        throw SystemFatalException.raiseException("can't extract company id from the token");
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

}

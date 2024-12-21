package com.education.security;

import com.education.model.dto.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
public class HandleJWT {
    static SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @Value("${jwt.hmacSecret}")
    private String SECRET_KEY;


    public String generateToken(String email) {
        byte[] secret = Base64.getEncoder().encode(SECRET_KEY.getBytes());
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, signatureAlgorithm.getJcaName());
        var now = Instant.now();

        int MINUTES = 60;
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(MINUTES, ChronoUnit.MINUTES)))
                .signWith(signatureAlgorithm, secretKeySpec)
                .compact();
    }

    public String extractUsername(String token) {
        return getTokenBody(token).getSubject();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private Claims getTokenBody(String token) {
        try {
            return Jwts
                    .parser()
                    .setSigningKey(Base64.getEncoder().encode(SECRET_KEY.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) { // Invalid signature or expired token
            throw new AccessDeniedException("Access denied: " + e.getMessage());
        }
    }

    private boolean isTokenExpired(String token) {
        Claims claims = getTokenBody(token);
        return claims.getExpiration().before(new Date());
    }
}

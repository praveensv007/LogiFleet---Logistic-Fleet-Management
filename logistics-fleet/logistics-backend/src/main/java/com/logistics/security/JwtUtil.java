package com.logistics.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpiry;
    private final long refreshExpiry;
    private final long refreshExpiryRemember;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiry}") long accessExpiry,
                   @Value("${jwt.refresh-expiry}") long refreshExpiry,
                   @Value("${jwt.refresh-expiry-remember}") long refreshExpiryRemember) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
        this.refreshExpiryRemember = refreshExpiryRemember;
    }

    public String generateAccessToken(String username, String role) {
        return build(username, role, TYPE_ACCESS, accessExpiry, false);
    }

    public String generateRefreshToken(String username, String role, boolean rememberMe) {
        return build(username, role, TYPE_REFRESH, rememberMe ? refreshExpiryRemember : refreshExpiry, rememberMe);
    }

    private String build(String username, String role, String type, long expiryMs, boolean remember) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("type", type)
                .claim("remember", remember)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException when the token is invalid or expired */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** Returns the username when the token is a valid token of the given type, otherwise null. */
    public String validate(String token, String expectedType) {
        try {
            Claims claims = parse(token);
            if (!expectedType.equals(claims.get("type", String.class))) {
                return null;
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getAccessExpiry() {
        return accessExpiry;
    }
}

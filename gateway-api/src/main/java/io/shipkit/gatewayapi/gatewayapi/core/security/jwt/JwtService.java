package io.shipkit.gatewayapi.gatewayapi.core.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_LENGTH = 32;
    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @PostConstruct
    public void validateJwtConfiguration() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret cannot be null or empty. Please configure 'jwt.secret' property.");
        }
        
        try {
            byte[] decodedSecret = Decoders.BASE64.decode(secret);
            
            if (decodedSecret.length < MIN_SECRET_LENGTH) {
                throw new IllegalStateException(
                    String.format("JWT secret is too short (%d bytes). Minimum required: %d bytes for security.", 
                                decodedSecret.length, MIN_SECRET_LENGTH)
                );
            }
            
            if (expirationMs <= 0) {
                throw new IllegalStateException("JWT expiration time must be positive. Please configure 'jwt.expiration' property correctly.");
            }
            
            logger.info("JWT Secret is good");
            
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is not a valid base64 encoded string. Please check 'jwt.secret' configuration.", e);
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .addClaims(Map.of("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
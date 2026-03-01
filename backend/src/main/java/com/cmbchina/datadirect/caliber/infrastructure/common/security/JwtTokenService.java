package com.cmbchina.datadirect.caliber.infrastructure.common.security;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenService {

    private final CaliberSecurityProperties caliberSecurityProperties;

    public JwtTokenService(CaliberSecurityProperties caliberSecurityProperties) {
        this.caliberSecurityProperties = caliberSecurityProperties;
    }

    public String issueToken(String username, List<String> roles) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expireAt = now.plusMinutes(Math.max(1, caliberSecurityProperties.getTokenExpireMinutes()));
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expireAt.toInstant()))
                .claims(Map.of("roles", roles == null ? List.of() : roles))
                .signWith(resolveKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(resolveKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public OffsetDateTime extractExpireAt(String token) {
        Date exp = parse(token).getExpiration();
        return exp == null ? OffsetDateTime.now(ZoneOffset.UTC) : OffsetDateTime.ofInstant(exp.toInstant(), ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object value = claims.get("roles");
        if (value instanceof List<?> list) {
            return list.stream().map(item -> item == null ? "" : String.valueOf(item)).filter(text -> !text.isBlank()).toList();
        }
        return List.of();
    }

    private SecretKey resolveKey() {
        String secret = caliberSecurityProperties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("caliber.security.jwt-secret is required");
        }
        String safe = secret.trim();
        if (safe.matches("^[A-Za-z0-9+/=]+$") && safe.length() >= 44) {
            try {
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(safe));
            } catch (Exception ignored) {
                // fallback to utf-8 bytes
            }
        }
        byte[] bytes = safe.getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[Math.max(32, bytes.length)];
        System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, padded.length));
        return Keys.hmacShaKeyFor(padded);
    }
}

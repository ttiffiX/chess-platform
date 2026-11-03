package chess.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final SecretKey key;
    private final long expirationMs;
    private final long expirationRefreshMs;

    public JwtService(
            @Value("${app.jwt.secret-key}") String secretKey,
            @Value("${app.jwt.expiration-access-ms}") long expirationMs,
            @Value("${app.jwt.expiration-refresh-ms}") long expirationRefreshMs) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.expirationRefreshMs = expirationRefreshMs;
    }

    public String generateAccessToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationRefreshMs);

        return Jwts.builder()
                .subject(userId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public Duration getRefreshTokenTtl() {
        return Duration.ofMillis(expirationRefreshMs);
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, ACCESS_TOKEN);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TOKEN);
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = getClaims(token);
            String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);

            return expectedType.equals(actualType);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
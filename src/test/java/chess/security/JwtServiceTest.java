package chess.security;

import chess.security.jwt.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "this-is-a-test-secret-key-32bytes-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000, 604_800_000);
    }

    /*
     * KỊCH BẢN TEST:
     *
     * 1. Access token:
     *    - Sinh token thành công.
     *    - Validate bằng validateAccessToken() thành công.
     *    - Không được validate bằng validateRefreshToken().
     *    - Kiểm tra subject, email, role.
     *
     * 2. Refresh token:
     *    - Sinh token thành công.
     *    - Validate bằng validateRefreshToken() thành công.
     *    - Không được validate bằng validateAccessToken().
     *
     * 3. Token không hợp lệ:
     *    - Thiếu claim token_type.
     *    - Sai chữ ký.
     *    - Hết hạn.
     */

    @Nested
    @DisplayName("Access token tests")
    class AccessTokenTests {

        @Test
        @DisplayName("Generate access token successfully")
        void generateAccessToken_Success() {
            String token = jwtService.generateAccessToken(
                    "user-123",
                    "test@gmail.com",
                    "ROLE_USER"
            );

            assertNotNull(token);
            assertTrue(jwtService.validateAccessToken(token));
            assertFalse(jwtService.validateRefreshToken(token));

            assertEquals("user-123", jwtService.getUserIdFromToken(token));
            assertEquals("test@gmail.com", jwtService.getEmailFromToken(token));
            assertEquals("ROLE_USER", jwtService.getRoleFromToken(token));
        }
    }

    @Nested
    @DisplayName("Refresh token tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Generate refresh token successfully")
        void generateRefreshToken_Success() {
            String token = jwtService.generateRefreshToken("user-123");

            assertNotNull(token);
            assertTrue(jwtService.validateRefreshToken(token));
            assertFalse(jwtService.validateAccessToken(token));

            assertEquals("user-123", jwtService.getUserIdFromToken(token));
            assertEquals(604_800_000, jwtService.getRefreshTokenTtl().toMillis());
        }
    }

    @Nested
    @DisplayName("Invalid token tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Reject token without token_type claim")
        void validateToken_MissingTypeClaim_ReturnsFalse() {
            String token = Jwts.builder()
                    .subject("user-123")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(getSigningKey(SECRET))
                    .compact();

            assertFalse(jwtService.validateAccessToken(token));
            assertFalse(jwtService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("Reject token with invalid signature")
        void validateToken_InvalidSignature_ReturnsFalse() {
            String token = Jwts.builder()
                    .subject("user-123")
                    .claim("token_type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(getSigningKey("another-test-secret-key-32bytes-long"))
                    .compact();

            assertFalse(jwtService.validateAccessToken(token));
        }

        @Test
        @DisplayName("Reject expired access token")
        void validateToken_ExpiredToken_ReturnsFalse() {
            String token = Jwts.builder()
                    .subject("user-123")
                    .claim("token_type", "access")
                    .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                    .expiration(new Date(System.currentTimeMillis() - 60_000))
                    .signWith(getSigningKey(SECRET))
                    .compact();

            assertFalse(jwtService.validateAccessToken(token));
        }

        @Test
        @DisplayName("Reject malformed token")
        void validateToken_MalformedToken_ReturnsFalse() {
            assertFalse(jwtService.validateAccessToken("not-a-jwt"));
            assertFalse(jwtService.validateRefreshToken("not-a-jwt"));
        }
    }

    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
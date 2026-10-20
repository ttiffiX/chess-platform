package chess.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtils {

    @Value("${app.cookie.secure:false}")
    private boolean isSecure;

    public ResponseCookie createRefreshTokenCookie(String refreshToken, Duration ttl) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/api/auth/refresh")
                .maxAge(ttl)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
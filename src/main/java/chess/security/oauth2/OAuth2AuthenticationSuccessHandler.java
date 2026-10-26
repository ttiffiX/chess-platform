package chess.security.oauth2;

import chess.common.redis.RefreshTokenService;
import chess.common.utils.CookieUtils;
import chess.feature.user.entity.User;
import chess.feature.user.service.UserService;
import chess.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    public final CookieUtils cookieUtils;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            getRedirectStrategy().sendRedirect(request, response, redirectUri + "?error=invalid_oauth_user");
            return;
        }

        // 1. Lấy thông tin từ Google
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        if (googleId == null || email == null) {
            getRedirectStrategy().sendRedirect(request, response, redirectUri + "?error=invalid_oauth_attributes");
            return;
        }

        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 2. Lưu hoặc cập nhật User trong MongoDB
        User user = userService.processOAuthPostLogin(googleId, email, name, picture);

        if (user.isBanned()) {
            getRedirectStrategy().sendRedirect(request, response, redirectUri + "?error=banned");
            return;
        }

        // 3. Sinh Access Token (JWT) và Refresh Token (UUID)
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 4. Lưu Refresh Token vào Redis
        Duration refreshTokenTtl = jwtService.getRefreshTokenTtl();
        refreshTokenService.save(user.getId(), refreshToken, refreshTokenTtl);

        // 5. Gắn Refresh Token vào HttpOnly Cookie
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie(refreshToken, refreshTokenTtl);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 6. Chuyển hướng về React Frontend
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
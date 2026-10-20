package chess.feature.auth.service;

import chess.common.service.RefreshTokenService;
import chess.common.utils.CookieUtils;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.auth.dto.AuthResponse;
import chess.feature.auth.dto.TokenRefreshResult;
import chess.feature.user.entity.User;
import chess.feature.user.service.UserService;
import chess.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final CookieUtils cookieUtils;

    public TokenRefreshResult refreshToken(String refreshToken) {
        // 1. Validate sự tồn tại & tính hợp lệ của JWT Cookie
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_MISSING, "Missing refresh token cookie");
        }

        if (!jwtService.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid or expired refresh token format");
        }

        // 2. Lấy userId trực tiếp từ Claims của Refresh Token (JWT)
        String userId = jwtService.getUserIdFromToken(refreshToken);

        // 3. Kiểm tra token trong Redis theo Key (prefix + userId)
        if (!refreshTokenService.matches(userId, refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token is expired or revoked");
        }

        // 4. Lấy thông tin User từ MongoDB
        User user = userService.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "User not found"));
        if (user.isBanned()) {
            refreshTokenService.delete(userId);
            throw new AppException(ErrorCode.UNAUTHORIZED, "User is banned");
        }

        // 5. Sinh Access Token mới & Rotate Refresh Token mới
        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        Duration refreshTokenTtl = jwtService.getRefreshTokenTtl();
        refreshTokenService.save(user.getId(), newRefreshToken, refreshTokenTtl);

        // 6. Tạo Cookie mới bằng CookieUtils
        ResponseCookie newCookie = cookieUtils.createRefreshTokenCookie(newRefreshToken, refreshTokenTtl);

        // 7. Đóng gói AuthResponse kèm User Info
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .picture(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .build())
                .build();

        return TokenRefreshResult.builder()
                .authResponse(authResponse)
                .refreshTokenCookie(newCookie)
                .build();
    }

    public ResponseCookie createCleanRefreshTokenCookie() {
        return cookieUtils.deleteRefreshTokenCookie();
    }
}
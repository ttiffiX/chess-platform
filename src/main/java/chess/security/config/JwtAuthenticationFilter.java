package chess.security.config;

import chess.feature.user.enums.Role;
import chess.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // Không có token: tiếp tục để Spring Security xử lý.
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Có Authorization header nhưng không đúng định dạng Bearer.
        if (!authorization.startsWith("Bearer ")) {
            unauthorized(response, "Invalid authorization header");
            return;
        }

        String token = authorization.substring(7).trim();

        // Token không hợp lệ hoặc hết hạn.
        if (!StringUtils.hasText(token) || !jwtService.validateAccessToken(token)) {
            unauthorized(response, "Invalid or expired access token");
            return;
        }

        String userId = jwtService.getUserIdFromToken(token);

        if (!StringUtils.hasText(userId)) {
            unauthorized(response, "Invalid user identity");
            return;
        }

        String role = jwtService.getRoleFromToken(token);

        if (!Role.isValid(role)) {
            unauthorized(response, "Invalid user role");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
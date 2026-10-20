package chess.feature.user.service;

import chess.feature.user.entity.User;
import chess.feature.user.enums.Role;
import chess.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User processOAuthPostLogin(String googleId, String email, String name, String picture) {
        Optional<User> existUser = userRepository.findByGoogleId(googleId);

        if (existUser.isPresent()) {
            User user = existUser.get();
            user.setName(name);
            user.setAvatarUrl(picture);
            user.setUpdatedAt(Instant.now());
            return userRepository.save(user);
        }

        // Tạo tài khoản mới lần đầu đăng nhập Google
        User newUser = User.builder()
                .googleId(googleId)
                .email(email)
                .name(name)
                .nickname(name) // Mặc định hiển thị tên Google
                .avatarUrl(picture)
                .role(Role.ROLE_USER)
                .elo(1200)
                .banned(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userRepository.save(newUser);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}
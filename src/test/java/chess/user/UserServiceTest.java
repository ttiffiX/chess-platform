package chess.user;

import chess.feature.user.entity.User;
import chess.feature.user.enums.Role;
import chess.feature.user.repository.UserRepository;
import chess.feature.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final String GOOGLE_ID = "google-123";
    private static final String EMAIL = "test@gmail.com";
    private static final String NAME = "Test User";
    private static final String PICTURE = "avatar-url";

    /*
     * KỊCH BẢN TEST:
     *
     * 1. Existing Google user:
     *    - Tìm thấy user theo Google ID.
     *    - Cập nhật name và avatar.
     *    - Không thay đổi các thông tin khác.
     *    - Lưu user đã cập nhật.
     *
     * 2. New Google user:
     *    - Không tìm thấy user theo Google ID.
     *    - Tạo user mới với thông tin Google.
     *    - Gán ROLE_USER, ELO 1200 và banned = false.
     *    - Lưu user mới.
     */

    @Nested
    @DisplayName("Existing Google user tests")
    class ExistingUserTests {

        @Test
        @DisplayName("Update existing user information")
        void processOAuthPostLogin_ExistingUser_UpdatesInformation() {
            User existingUser = User.builder()
                    .id("user-123")
                    .googleId(GOOGLE_ID)
                    .email(EMAIL)
                    .name("Old Name")
                    .nickname("Old Nickname")
                    .avatarUrl("old-avatar")
                    .role(Role.ROLE_ADMIN)
                    .elo(1500)
                    .banned(false)
                    .build();

            when(userRepository.findByGoogleId(GOOGLE_ID))
                    .thenReturn(Optional.of(existingUser));

            when(userRepository.save(existingUser))
                    .thenReturn(existingUser);

            User result = userService.processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE
            );

            assertNotNull(result);
            assertEquals("user-123", result.getId());
            assertEquals(NAME, result.getName());
            assertEquals(PICTURE, result.getAvatarUrl());

            // Các thông tin không nằm trong phạm vi cập nhật phải được giữ nguyên.
            assertEquals(EMAIL, result.getEmail());
            assertEquals("Old Nickname", result.getNickname());
            assertEquals(Role.ROLE_ADMIN, result.getRole());
            assertEquals(1500, result.getElo());

            verify(userRepository).findByGoogleId(GOOGLE_ID);
            verify(userRepository).save(existingUser);
        }
    }

    @Nested
    @DisplayName("New Google user tests")
    class NewUserTests {

        @Test
        @DisplayName("Create new user with default information")
        void processOAuthPostLogin_NewUser_CreatesSuccessfully() {
            when(userRepository.findByGoogleId(GOOGLE_ID))
                    .thenReturn(Optional.empty());

            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Instant before = Instant.now();

            User result = userService.processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE
            );

            Instant after = Instant.now();

            assertNotNull(result);
            assertNotNull(result.getId());

            assertEquals(GOOGLE_ID, result.getGoogleId());
            assertEquals(EMAIL, result.getEmail());
            assertEquals(NAME, result.getName());
            assertEquals(NAME, result.getNickname());
            assertEquals(PICTURE, result.getAvatarUrl());

            assertEquals(Role.ROLE_USER, result.getRole());
            assertEquals(1200, result.getElo());
            assertFalse(result.isBanned());

            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());

            assertFalse(result.getCreatedAt().isBefore(before));
            assertFalse(result.getCreatedAt().isAfter(after));

            verify(userRepository).findByGoogleId(GOOGLE_ID);
            verify(userRepository).save(any(User.class));
        }
    }
}
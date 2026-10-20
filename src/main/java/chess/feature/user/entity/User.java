package chess.feature.user.entity;

import chess.feature.user.enums.Role;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String googleId; // sub từ Google trả về để map tài khoản

    private String name;        // Tên đầy đủ từ Google Profile

    private String nickname;    // Nickname hiển thị khi chơi cờ

    private String avatarUrl;   // Ảnh đại diện lấy từ Google

    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Builder.Default
    private Integer elo = 1200; // Điểm xếp hạng cờ vua mặc định ban đầu

    @Builder.Default
    private boolean banned = false;

    private String banReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
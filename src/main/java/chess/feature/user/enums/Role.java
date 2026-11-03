package chess.feature.user.enums;

import java.util.Arrays;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN;

    public static boolean isValid(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(value -> value.name().equals(role));
    }
}
package chess.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Gameplay Errors (Dải 1000)
    INVALID_MOVE(1001, HttpStatus.BAD_REQUEST),

    // Room / Matchmaking Errors (Dải 2000)
    ROOM_NOT_FOUND(2001, HttpStatus.NOT_FOUND),
    ROOM_FULL(2002, HttpStatus.CONFLICT),

    // Input / Auth Errors (Dải 4000)
    INVALID_INPUT(4001, HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4002, HttpStatus.UNAUTHORIZED),
    METHOD_NOT_ALLOWED(4005, HttpStatus.METHOD_NOT_ALLOWED),

    // System Errors (Dải 5000)
    INTERNAL_SERVER_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR);

    private final int numericCode;
    private final HttpStatus httpStatus;
}
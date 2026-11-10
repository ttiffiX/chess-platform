
package chess.exception;

import chess.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    private static final String ERROR_DESTINATION = "/queue/errors";

    /**
     * Handle application/business exceptions.
     */
    @MessageExceptionHandler(AppException.class)
    @SendToUser(value = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Void> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn(
                "WebSocket Business Error [{} - {}]: {}",
                errorCode.name(),
                errorCode.getNumericCode(),
                ex.getMessage()
        );

        return ApiResponse.error(
                errorCode.getNumericCode(),
                ex.getMessage()
        );
    }

    /**
     * Handle payload validation errors.
     */
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(value = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        log.warn("WebSocket Validation Errors: {}", errors);

        return ApiResponse.error(
                ErrorCode.INVALID_INPUT.getNumericCode(),
                "Validation failed for " + errors.size() + " field(s)",
                errors
        );
    }

    /**
     * Handle malformed or unsupported message payloads.
     */
    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(value = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Void> handleMessageConversionException(
            MessageConversionException ex
    ) {
        log.warn("Invalid WebSocket payload: {}", ex.getMessage());

        return ApiResponse.error(
                ErrorCode.INVALID_INPUT.getNumericCode(),
                "Malformed WebSocket message or invalid input"
        );
    }

    /**
     * Handle unexpected system exceptions.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(value = ERROR_DESTINATION, broadcast = false)
    public ApiResponse<Void> handleUnexpectedException(Exception ex) {
        log.error("Unhandled WebSocket Error: ", ex);

        return ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getNumericCode(),
                "An unexpected error occurred. Please try again later."
        );
    }
}
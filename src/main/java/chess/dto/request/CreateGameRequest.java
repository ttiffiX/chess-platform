package chess.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateGameRequest(
        @Min(value = 0, message = "Base time must not be negative")
        @Max(value = 10800, message = "Base time cannot exceed 180 minutes")
        Long baseTimeSeconds, // vd: 600 (10 phút), 180 (3 phút), null nếu chơi vô hạn

        @Min(value = 0, message = "Increment time must not be negative")
        @Max(value = 180, message = "Increment time cannot exceed 180 seconds")
        Long incrementSeconds // vd: 0, 2, 5
) {
    public CreateGameRequest {
        if (baseTimeSeconds == null) baseTimeSeconds = 0L;
        if (incrementSeconds == null) incrementSeconds = 0L;
    }
}
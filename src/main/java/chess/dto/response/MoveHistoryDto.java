package chess.dto.response;

public record MoveHistoryDto(
        int moveIndex,
        String from,
        String to) {
}

package model;

import model.piece.Color;

public record GameResult(GameStatus status, Color winner) {
    public GameResult {
        if (status == null) {
            throw new IllegalArgumentException("Game status cannot be null");
        }

        if (status == GameStatus.IN_PROGRESS && winner != null) {
            throw new IllegalArgumentException("Winner must be null when the game is in progress");
        }

        boolean isDecisiveWin = (status == GameStatus.CHECKMATE || status == GameStatus.RESIGNED);
        if (isDecisiveWin && winner == null) {
            throw new IllegalArgumentException("Winner cannot be null when game is won");
        }

        boolean isDraw = (status != GameStatus.IN_PROGRESS && !isDecisiveWin);
        if (isDraw && winner != null) {
            throw new IllegalArgumentException("Winner must be null on a draw");
        }
    }

    public static GameResult inProgress() {
        return new GameResult(GameStatus.IN_PROGRESS, null);
    }

    public static GameResult checkmate(Color winner) {
        return new GameResult(GameStatus.CHECKMATE, winner);
    }

    public static GameResult draw(GameStatus drawStatus) {
        return new GameResult(drawStatus, null);
    }
}

package model.game;

import model.*;
import model.piece.Color;
import model.piece.Pawn;
import model.piece.Piece;
import rules.*;
import rules.services.CheckService;
import rules.services.DrawService;
import rules.services.LegalMoveService;
import rules.services.PathService;
import rules.validator.MoveValidator;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Board board;
    private Color currentTurn;
    private boolean inCheck = false;
    private CastleRights castleRights;
    private final List<MoveRecord> moveHistory = new ArrayList<>();
    private Position enPassTarget = null;
    private int halfMoveClock = 0;
    private int fullMoveNumber = 1;
    private GameResult gameResult;

    private static final PathService PATH_SERVICE = new PathService();
    private static final CheckService CHECK_SERVICE = new CheckService(PATH_SERVICE);
    private static final MoveValidator MOVE_VALIDATOR = new MoveValidator(PATH_SERVICE, CHECK_SERVICE);
    private static final GameStatusEvaluator GAME_STATUS_EVALUATOR = new GameStatusEvaluator(
            new LegalMoveService(MOVE_VALIDATOR),
            new DrawService()
    );

    public Game() {
        this.board = new Board();
        this.currentTurn = Color.WHITE;
        this.castleRights = CastleRights.initial();
        this.gameResult = GameResult.inProgress();
    }

    public Board getBoard() {
        return board;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public void nextTurn() {
        currentTurn = currentTurn.opposite();
    }

    public List<MoveRecord> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public CastleRights getCastleRights() {
        return castleRights;
    }

    public boolean isInCheck() {
        return inCheck;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public void move(Move move) {
        if (gameResult.status() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is already over. Result: " + gameResult.status());
        }

        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null.");
        }

        ValidationContext ctx = new ValidationContext(board, move, currentTurn, castleRights, enPassTarget);
        ValidationResult validationResult = MOVE_VALIDATOR.validate(ctx);
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(validationResult.message());
        }

        Piece piece = board.getPiece(move.from());
        Piece capturedPiece = validationResult.capturedPiece();

        enPassTarget = (piece instanceof Pawn && Math.abs(move.to().rank() - move.from().rank()) == 2)
                ? new Position(move.from().file(), (move.from().rank() + move.to().rank()) / 2)
                : null;

        castleRights = castleRights.withMoveApplied(piece, move.from(), capturedPiece, move.to());

        halfMoveClock = (piece instanceof Pawn || capturedPiece != null) ? 0 : halfMoveClock + 1;
        if (currentTurn == Color.BLACK) {
            fullMoveNumber++;
        }

        validationResult.execute().accept(board);

        moveHistory.add(new MoveRecord(piece, move, capturedPiece));
        nextTurn();

        inCheck = CHECK_SERVICE.isInCheck(board, currentTurn);

        gameResult = GAME_STATUS_EVALUATOR.evaluate(
                board,
                currentTurn,
                castleRights,
                enPassTarget,
                halfMoveClock,
                moveHistory,
                inCheck
        );
    }
}

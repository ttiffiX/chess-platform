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
import java.util.Objects;

public class Game {
    private final Board board;
    private Color currentTurn;
    private boolean inCheck;
    private CastleRights castleRights;
    private Position enPassTarget;
    private int halfMoveClock;
    private int fullMoveNumber;
    private GameResult gameResult;
    List<GameStateSnapshot> gameStateHistory = new ArrayList<>();

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
        this.inCheck = false;
        this.enPassTarget = null;
        this.halfMoveClock = 0;
        this.fullMoveNumber = 1;
        this.gameResult = GameResult.inProgress();
        this.gameStateHistory.add(createSnapshot(null, null));
    }

    private GameStateSnapshot createSnapshot(Move lastMove, Piece capturedPiece) {
        return new GameStateSnapshot(
                board.copy(),
                lastMove,
                capturedPiece,
                currentTurn,
                castleRights,
                enPassTarget,
                halfMoveClock,
                fullMoveNumber,
                inCheck,
                gameResult
        );
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

    public List<GameStateSnapshot> getGameStateHistory() {
        return new ArrayList<>(gameStateHistory);
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

        nextTurn();
        gameStateHistory.add(createSnapshot(move, capturedPiece));

        inCheck = CHECK_SERVICE.isInCheck(board, currentTurn);

        gameResult = GAME_STATUS_EVALUATOR.evaluate(
                board,
                currentTurn,
                castleRights,
                enPassTarget,
                halfMoveClock,
                gameStateHistory,
                inCheck
        );
    }

    public boolean undo() {
        if (gameStateHistory.size() <= 1) {
            return false;
        }

        gameStateHistory.removeLast();

        GameStateSnapshot prev = gameStateHistory.getLast();
        this.board.undoMove(prev.board().getPieces());
        this.currentTurn = prev.turn();
        this.castleRights = prev.castleRights();
        this.enPassTarget = prev.enPassTarget();
        this.halfMoveClock = prev.halfMoveClock();
        this.fullMoveNumber = prev.fullMoveNumber();
        this.inCheck = prev.inCheck();
        this.gameResult = prev.gameResult();

        return true;
    }

    public GameStateSnapshot getSnapshotAt(int moveIndex) {
        if (moveIndex < 0 || moveIndex >= gameStateHistory.size()) {
            throw new IndexOutOfBoundsException("Invalid snapshot index: " + moveIndex);
        }
        return gameStateHistory.get(moveIndex);
    }

    public List<Piece> getCapturedPieces() {
        return gameStateHistory.stream()
                .map(GameStateSnapshot::capturedPiece)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Piece> getCapturedPiecesByColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color must not be null.");
        }
        return getCapturedPieces().stream()
                .filter(piece -> piece.getColor() == color)
                .toList();
    }
}

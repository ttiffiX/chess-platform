package chess.engine.model.game;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.rules.GameStatusEvaluator;
import chess.engine.rules.InvalidMoveReason;
import chess.engine.rules.ValidationContext;
import chess.engine.rules.ValidationResult;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Pawn;
import chess.engine.model.piece.Piece;
import chess.engine.rules.services.CheckService;
import chess.engine.rules.services.DrawService;
import chess.engine.rules.services.LegalMoveService;
import chess.engine.rules.services.PathService;
import chess.engine.rules.validator.MoveValidator;
import chess.engine.utils.GeneratorUtils;

import java.util.*;

public class Game {
    private final Board board;
    private Color currentTurn;
    private boolean inCheck;
    private CastleRights castleRights;
    private Position enPassTarget;
    private int halfMoveClock;
    private int fullMoveNumber;
    private GameResult gameResult;
    private final Map<String, Integer> positionOccurrences = new HashMap<>();
    private final List<Move> legalMoves = new ArrayList<>();

    private static final PathService PATH_SERVICE = new PathService();
    private static final CheckService CHECK_SERVICE = new CheckService(PATH_SERVICE);
    private static final MoveValidator MOVE_VALIDATOR = new MoveValidator(PATH_SERVICE, CHECK_SERVICE);
    private static final DrawService DRAW_SERVICE = new DrawService();
    private static final LegalMoveService LEGAL_MOVE_SERVICE = new LegalMoveService(MOVE_VALIDATOR);
    private static final GameStatusEvaluator GAME_STATUS_EVALUATOR = new GameStatusEvaluator(DRAW_SERVICE);

    public Game() {
        this.board = new Board();
        this.currentTurn = Color.WHITE;
        this.castleRights = CastleRights.initial();
        this.inCheck = false;
        this.enPassTarget = null;
        this.halfMoveClock = 0;
        this.fullMoveNumber = 1;
        this.gameResult = GameResult.inProgress();

        String initialKey = DRAW_SERVICE.createPositionKey(
                board,
                currentTurn,
                castleRights.createCastleRightsKey(),
                null,
                List.of()
        );

        positionOccurrences.put(initialKey, 1);
    }

    public Board getBoard() {
        return board;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    private void nextTurn() {
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

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public List<Move> getAllLegalMovesByColor() {
        return legalMoves;
    }

    public ValidationResult move(Move move) {
        if (isGameOver()) {
            return ValidationResult.fail(InvalidMoveReason.GAME_ALREADY_OVER, "Game is already over. Result: " + gameResult.status());
        }

        Objects.requireNonNull(move, "Move must not be null.");

        ValidationContext ctx = new ValidationContext(board, move, currentTurn, castleRights, enPassTarget);
        ValidationResult validationResult = MOVE_VALIDATOR.validate(ctx);
        if (!validationResult.valid()) {
            return validationResult;
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

        inCheck = CHECK_SERVICE.isInCheck(board, currentTurn);

        legalMoves.clear();
        legalMoves.addAll(LEGAL_MOVE_SERVICE.generateLegalMoves(
                board,
                currentTurn,
                castleRights,
                enPassTarget
        ));

        String positionKey = DRAW_SERVICE.createPositionKey(
                board,
                currentTurn,
                castleRights.createCastleRightsKey(),
                enPassTarget,
                legalMoves
        );

        int occurrenceCount = positionOccurrences.merge(
                positionKey,
                1,
                Integer::sum
        );

        gameResult = GAME_STATUS_EVALUATOR.evaluate(
                board,
                currentTurn,
                halfMoveClock,
                occurrenceCount,
                inCheck,
                legalMoves
        );

        return validationResult;
    }

    public boolean resign(Color playerColor) {
        if (isGameOver()) {
            return false;
        }
        if (playerColor == Color.WHITE) {
            gameResult = GameResult.resigned(Color.BLACK);
        } else {
            gameResult = GameResult.resigned(Color.WHITE);
        }
        return true;
    }

    public boolean agreeDraw() {
        if (isGameOver()) {
            return false;
        }
        gameResult = GameResult.draw(GameStatus.DRAW_BY_AGREEMENT);
        return true;
    }

    public boolean isTimedOut(Color timeOutColor) {
        if (isGameOver()) {
            return false;
        }
        gameResult = GameResult.timedOut(timeOutColor.opposite());
        return true;
    }

    public boolean isGameOver() {
        return gameResult.status() != GameStatus.IN_PROGRESS;
    }

    public String getCurrentFEN() {
        return GeneratorUtils.FENGenerate(
                board,
                currentTurn,
                castleRights.createCastleRightsKey(),
                enPassTarget,
                halfMoveClock,
                fullMoveNumber
        );
    }
}

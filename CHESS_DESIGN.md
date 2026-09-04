# Thiết kế chi tiết clean code cho chess platform

Mục tiêu: code **gọn**, **dễ test**, **đủ chức năng chess cơ bản đầy đủ luật**.

## 1) Trả lời trực tiếp 2 câu hỏi của bạn

### 1.1 Có cần tách `Piece` thành nhiều class (`King`, `Queen`...) không?

**Không bắt buộc.** Với dự án này, để code gọn hơn, nên dùng:

- `PieceType` (enum): `KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN`
- `Piece` (record/class đơn): chỉ giữ `color + type`

Logic đi quân đặt trong `MoveValidator`/`PieceRuleEngine` theo `PieceType`.

> Khi nào nên tách class từng quân?
> - Khi cần engine plugin nâng cao, AI mạnh, hoặc luật biến thể phức tạp.
> - Còn hiện tại bạn ưu tiên clean/gọn thì **PieceType-first** là hợp lý nhất.

### 1.2 `Square` là gì?

`Square` là **một ô cờ** (tọa độ + quân đang đứng trên đó, hoặc rỗng).

Ví dụ:
- `Position` = tọa độ thuần (`e4`)
- `Square` = `position=e4`, `piece=WHITE_PAWN` hoặc `piece=null`

Nếu bạn dùng `Map<Position, Piece>` như hiện tại thì có thể **không cần `Square` class**.  
Nếu muốn rõ domain hơn thì dùng `Square[][]`.

## 2) Cấu trúc thư mục đề xuất (bản tối ưu cho bạn)

```text
src/main/java/chess/
  domain/
    Color.java
    PieceType.java
    Piece.java
    Position.java
    model.Move.java
    MoveType.java
    GameStatus.java
    CastleRights.java
    GameState.java
    model.MoveRecord.java
    InvalidMoveReason.java
    ValidationResult.java
    model.Board.java

  rules/
    PieceRuleEngine.java
    PathService.java
    CheckService.java
    DrawService.java
    MoveValidator.java
    LegalMoveService.java

  application/
    ChessGameService.java

  ui/cli/
    Main.java
```

## 3) Thiết kế từng file: trách nhiệm + hàm chính

## `domain/Color.java`

**Trách nhiệm:** màu quân.  
**Hàm:**
- `Color opposite()`

## `domain/PieceType.java`

**Trách nhiệm:** định danh loại quân.  
**Giá trị:** `KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN`

## `domain/Piece.java`

**Trách nhiệm:** dữ liệu quân cờ (không chứa luật đi).  
**Field:**
- `Color color`
- `PieceType type`

## `domain/Position.java`

**Trách nhiệm:** ô tọa độ immutable.  
**Hàm:**
- constructor validate `a..h`, `1..8`
- `int fileIndex()` (a->1..h->8 hoặc 0..7)
- `String toString()`

## `domain/model.Move.java`

**Trách nhiệm:** yêu cầu nước đi đầu vào.  
**Field:**
- `Position from`
- `Position to`
- `PieceType promotionChoice` (nullable)

## `domain/MoveType.java`

**Giá trị:** `NORMAL, CAPTURE, CASTLE_KING_SIDE, CASTLE_QUEEN_SIDE, EN_PASSANT, PROMOTION`

## `domain/GameStatus.java`

**Giá trị:** `IN_PROGRESS, CHECK, CHECKMATE, STALEMATE, DRAW, RESIGNED`

## `domain/CastleRights.java`

**Trách nhiệm:** quyền nhập thành.  
**Field:**
- `whiteKingSide`, `whiteQueenSide`, `blackKingSide`, `blackQueenSide`

## `domain/GameState.java`

**Trách nhiệm:** toàn bộ state ván cờ tại 1 thời điểm.  
**Field:**
- `model.Board board`
- `Color turn`
- `GameStatus status`
- `CastleRights castleRights`
- `Position enPassantTarget` (nullable)
- `int halfMoveClock`
- `int fullMoveNumber`

## `domain/model.MoveRecord.java`

**Trách nhiệm:** lưu lịch sử để undo, repetition, PGN.  
**Field:**
- `model.Move move`
- `MoveType type`
- `Piece movedPiece`
- `Piece capturedPiece` (nullable)
- `GameState beforeStateSnapshot`

## `domain/InvalidMoveReason.java`

Ví dụ:
- `NO_PIECE_AT_SOURCE`
- `WRONG_TURN`
- `TARGET_OCCUPIED_BY_ALLY`
- `ILLEGAL_PIECE_PATTERN`
- `PATH_BLOCKED`
- `KING_WOULD_BE_IN_CHECK`
- `INVALID_CASTLING`
- `INVALID_EN_PASSANT`
- `INVALID_PROMOTION`

## `domain/ValidationResult.java`

**Trách nhiệm:** kết quả validate rõ ràng, không throw tràn lan.  
**Field/Hàm:**
- `boolean valid`
- `MoveType moveType`
- `InvalidMoveReason reason` (nullable)
- static factory: `ok(moveType)`, `fail(reason)`

## `domain/model.Board.java`

**Trách nhiệm:** lưu vị trí quân + thao tác state thuần.  
**Hàm:**
- `static model.Board initialSetup()`
- `Piece getPiece(Position p)`
- `boolean isEmpty(Position p)`
- `model.Board withMoveApplied(model.Move move, MoveType type)` (immutable preferred)
- `Map<Position, Piece> getPiecesByColor(Color color)`
- `Position findKing(Color color)`

> `model.Board` không kiểm tra luật hợp lệ, chỉ apply theo lệnh đã được validate.

## `rules/PieceRuleEngine.java`

**Trách nhiệm:** kiểm tra pattern theo `PieceType` (đi hình học cơ bản).  
**Hàm:**
- `boolean isPatternValid(Piece piece, model.Move move, GameState state)`

## `rules/PathService.java`

**Trách nhiệm:** kiểm tra đường đi trống cho quân trượt.  
**Hàm:**
- `boolean isPathClear(model.Board board, Position from, Position to)`
- `List<Position> between(Position from, Position to)`

## `rules/CheckService.java`

**Trách nhiệm:** check/checkmate/stalemate.  
**Hàm:**
- `boolean isInCheck(GameState state, Color color)`
- `boolean isCheckmate(GameState state, Color color)`
- `boolean isStalemate(GameState state, Color color)`

## `rules/DrawService.java`

**Trách nhiệm:** các điều kiện hòa.  
**Hàm:**
- `boolean isInsufficientMaterial(model.Board board)`
- `boolean isFiftyMoveRule(GameState state)`
- `boolean isThreefoldRepetition(List<model.MoveRecord> history)`

## `rules/LegalMoveService.java`

**Trách nhiệm:** sinh toàn bộ nước hợp lệ để:
- kiểm tra mate/stalemate
- hỗ trợ UI hint/AI sau này  
**Hàm:**
- `List<model.Move> generateLegalMoves(GameState state, Color color)`

## `rules/MoveValidator.java`

**Trách nhiệm:** pipeline validate đầy đủ 1 nước đi.  
**Hàm:**
- `ValidationResult validate(GameState state, model.Move move, List<model.MoveRecord> history)`

Pipeline trong `validate(...)`:
1. Source có quân, đúng turn.
2. Không ăn quân cùng màu.
3. Pattern hợp lệ theo `PieceRuleEngine`.
4. Path clear (`PathService`) nếu cần.
5. Luật đặc biệt (castle/en passant/promotion).
6. Simulate move, không tự chiếu vua (`CheckService`).

## `application/ChessGameService.java`

**Trách nhiệm:** API chính mà UI gọi vào.  
**Field:**
- `GameState state`
- `List<model.MoveRecord> history`
- dependency: `MoveValidator`, `CheckService`, `DrawService`, `LegalMoveService`

**Hàm:**
- `void newGame()`
- `GameState getState()`
- `ValidationResult tryMove(model.Move move)` (chỉ validate)
- `ValidationResult applyMove(model.Move move)` (validate + mutate state)
- `List<model.Move> getLegalMoves(Position from)`
- `void resign(Color color)`
- `boolean undoLastMove()`

Sau `applyMove`:
1. cập nhật board
2. cập nhật castle rights / en passant / clocks
3. đổi turn
4. cập nhật `GameStatus` (check, mate, stale, draw)

## `ui/cli/Main.java`

**Trách nhiệm:** nhập/xuất text, parse nước đi (`e2e4`, `e7e8Q`), gọi service.  
**Không chứa luật cờ.**

## 4) Sơ đồ móc nối giữa các file

```text
Main (UI)
  -> ChessGameService (application)
      -> MoveValidator
          -> PieceRuleEngine
          -> PathService
          -> CheckService
      -> model.Board.withMoveApplied(...)
      -> CheckService
      -> DrawService
      -> LegalMoveService
```

## 5) Luồng chạy thực tế cho một nước đi

1. UI parse `"e2e4"` -> `model.Move`.
2. Gọi `ChessGameService.applyMove(move)`.
3. Service gọi `MoveValidator.validate(...)`.
4. Nếu fail -> trả `ValidationResult.fail(reason)` cho UI.
5. Nếu pass -> service apply board + update state + update status.
6. UI render bàn cờ + thông báo trạng thái (`CHECK`, `CHECKMATE`...).

## 6) Tối ưu “gọn mà sạch” cho dự án hiện tại

1. Giữ `Piece` + `PieceType` (không tách 6 class).
2. Giữ `model.Board` là state container thuần.
3. Tách `rules.ChessRules` hiện tại thành `MoveValidator + CheckService + DrawService`.
4. Thêm `ValidationResult` để bỏ bớt exception control-flow.
5. Mọi logic game đi qua duy nhất `ChessGameService`.

Với thiết kế này, bạn sẽ vừa giữ code ngắn gọn, vừa đủ các luật chess cơ bản một cách rõ ràng, testable và dễ mở rộng.

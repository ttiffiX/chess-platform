# Nhiệm vụ refactor từ code hiện tại (giữ cái ổn, đập cái bế tắc)

Mục tiêu: tiếp tục từ code đang có, ưu tiên **giữ phần ổn định**, chỉ refactor mạnh phần `ChessRules` để sạch, dễ test, dễ mở rộng.

## 1) Trả lời nhanh: giữ `piece` hiện tại có ổn không?

**Có, giữ nguyên vẫn ổn.**

- `piece/Piece.java` + 6 class (`King/Queen/Rook/Bishop/Knight/Pawn`) hiện tại rõ ràng, dễ đọc, test được.
- Với scope game cờ vua cơ bản, mô hình này không sai kiến trúc.
- Chỉ cần đảm bảo toàn bộ luật tổng hợp nằm ở tầng rules/service, không nhét thêm vào `Board`.

=> **Kết luận:** không bắt buộc đổi sang `PieceType` ngay. Có thể giữ nguyên 6 class để giảm rủi ro refactor.

## 2) Phần nào giữ lại, phần nào cần làm lại

| File hiện tại | Giữ / Sửa / Thay mới | Lý do |
|---|---|---|
| `src/main/java/piece/Color.java` | **Giữ** | Tốt, nhỏ gọn |
| `src/main/java/piece/Position.java` | **Giữ** | Value object chuẩn |
| `src/main/java/piece/Piece.java` | **Giữ** | Abstraction rõ |
| `src/main/java/piece/*.java` (6 quân) | **Giữ** | Luật move/capture cơ bản đã rõ |
| `src/main/java/Move.java` | **Giữ, mở rộng nhẹ** | Có thể thêm promotion choice |
| `src/main/java/Board.java` | **Sửa nhẹ** | Cần thêm helper state/query, không nhét luật |
| `src/main/java/Game.java` | **Sửa vừa** | Trở thành nơi điều phối rules mới |
| `src/main/java/MoveRecord.java` | **Sửa** | Mở rộng thành record có snapshot để undo/repetition |
| `src/main/java/ChessRules.java` | **Thay mới hoàn toàn** | Đang ôm quá nhiều trách nhiệm |
| `src/main/java/Main.java` | **Sửa nhẹ** | Chỉ nên là UI entry |

## 3) Đổi tên file / tách file đề xuất (từ `ChessRules` cũ)

## 3.1 File mới nên tạo

```text
src/main/java/rules/
  MoveValidator.java
  CheckService.java
  DrawService.java
  LegalMoveService.java
  PathService.java
  ValidationResult.java
  InvalidMoveReason.java
```

## 3.2 `ChessRules.java` nên xử lý thế nào

- Giai đoạn chuyển tiếp: để lại file cũ, đánh dấu deprecated, gọi qua service mới.
- Giai đoạn hoàn tất: xóa `ChessRules.java`.

## 4) Trách nhiệm rõ cho từng file sau refactor

## `rules/MoveValidator.java`

**Nhiệm vụ:** validate một nước đi.

**Hàm nên có:**
- `ValidationResult validate(Board board, Move move, Color turn, List<MoveRecord> history)`
- `boolean isSpecialMove(...)` (castle/en passant/promotion detect)
- `ValidationResult validateSpecialMove(...)`

**Check bắt buộc:**
1. Source có quân?
2. Đúng lượt?
3. Không ăn quân cùng màu?
4. Piece movement pattern hợp lệ?
5. Path clear (trừ knight)?
6. Move xong có tự chiếu vua không?
7. Nếu special move: check điều kiện riêng.

## `rules/PathService.java`

**Nhiệm vụ:** check đường đi quân trượt.

**Hàm nên có:**
- `boolean isPathClear(Board board, Position from, Position to)`
- `List<Position> between(Position from, Position to)` (phục vụ debug/test)

## `rules/CheckService.java`

**Nhiệm vụ:** check/checkmate/stalemate.

**Hàm nên có:**
- `boolean isInCheck(Board board, Color color)`
- `boolean isCheckmate(Board board, Color color, List<MoveRecord> history)`
- `boolean isStalemate(Board board, Color color, List<MoveRecord> history)`

## `rules/LegalMoveService.java`

**Nhiệm vụ:** sinh tất cả nước đi hợp lệ.

**Hàm nên có:**
- `List<Move> generateLegalMoves(Board board, Color color, List<MoveRecord> history)`
- `boolean hasAnyLegalMove(...)`

## `rules/DrawService.java`

**Nhiệm vụ:** luật hòa.

**Hàm nên có:**
- `boolean isInsufficientMaterial(Board board)`
- `boolean isFiftyMoveRule(int halfMoveClock)`
- `boolean isThreefoldRepetition(List<MoveRecord> history)`

## `rules/ValidationResult.java`

**Nhiệm vụ:** trả kết quả validate sạch, không lạm dụng exception.

**Field/Hàm:**
- `boolean valid`
- `InvalidMoveReason reason`
- `String message`
- `static ValidationResult ok()`
- `static ValidationResult fail(InvalidMoveReason reason, String message)`

## `rules/InvalidMoveReason.java`

Enum gợi ý:
- `NO_PIECE_AT_SOURCE`
- `WRONG_TURN`
- `TARGET_HAS_ALLY_PIECE`
- `ILLEGAL_PATTERN`
- `PATH_BLOCKED`
- `KING_LEFT_IN_CHECK`
- `INVALID_CASTLING`
- `INVALID_EN_PASSANT`
- `INVALID_PROMOTION`

## 5) Hàm hiện tại cần sửa trực tiếp

## `Game.java`

**Hiện tại:** `move(Move)` gọi thẳng `ChessRules.isValidMove(...)`.

**Sửa thành:**
- inject/khởi tạo `MoveValidator`, `CheckService`, `DrawService`, `LegalMoveService`
- `move(Move)`:
1. gọi `validate(...)`
2. nếu fail -> throw exception có `reason` rõ
3. apply move
4. update history
5. đổi turn
6. cập nhật game status (check, mate, stale, draw)

**Thêm hàm:**
- `GameStatus getStatus()`
- `List<Move> getLegalMoves(Position from)` (optional nhưng nên có)

## `Board.java`

**Giữ vai trò state container**, thêm helper:
- `boolean isEmpty(Position position)`
- `Position findKing(Color color)`
- `Map<Position, Piece> getPiecesByColor(Color color)`
- `Board copy()` (an toàn khi simulate)

`movePiece(Move)` giữ nguyên tinh thần “chỉ move state”, không validate luật.

## `MoveRecord.java`

Mở rộng để phục vụ undo/draw:
- `Piece capturedPiece` (nullable)
- snapshot metadata: castle rights / en-passant / clocks (nếu thêm GameState sau)

## `Move.java`

Tùy chọn mở rộng:
- `Piece promotionTo` hoặc enum `PromotionType`

## 6) Trình tự triển khai thực tế (an toàn)

1. Tạo package `rules` + class `ValidationResult`, `InvalidMoveReason`, `PathService`.
2. Tách logic validate cơ bản từ `ChessRules` sang `MoveValidator`.
3. Tách `isInCheck` sang `CheckService`.
4. Tách `hasLegalMoves` sang `LegalMoveService`.
5. Sửa `Game.move(...)` để đi qua `MoveValidator`.
6. Bỏ phụ thuộc trực tiếp vào `ChessRules`.
7. Viết thêm test cho từng service.
8. Khi test ổn định, xóa `ChessRules.java`.

## 7) Danh sách test cần có sau khi tách rules

1. `MoveValidatorTest`: wrong turn, path blocked, self-check, ally capture.
2. `CheckServiceTest`: in-check, checkmate mẫu, stalemate mẫu.
3. `LegalMoveServiceTest`: có/không có nước hợp lệ.
4. `DrawServiceTest`: insufficient material, 50-move, threefold.
5. `GameTest` cập nhật: verify status transitions.

## 8) Kết luận thực dụng cho dự án này

- **Giữ nguyên module `piece` hiện tại là quyết định đúng và an toàn.**
- **Phần cần đập đi xây lại là `ChessRules`**: tách thành các service nhỏ với nhiệm vụ đơn nhất.
- Sau refactor, code sẽ sạch hơn đáng kể vì mỗi file chỉ làm một việc và luồng gọi rõ ràng qua `Game`.

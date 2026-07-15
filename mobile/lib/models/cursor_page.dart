import 'package:json_annotation/json_annotation.dart';

part 'cursor_page.g.dart';

/// 커서 페이지 응답(사양서 §1.2). `nextCursor` 는 서버가 `Long` 으로 내린다 — 웹은 `string` 으로
/// 선언해 두어 송금 내역 페이지네이션이 실제로 동작하지 않았다(R4-8).
///
/// 커서 **파라미터 이름**은 엔드포인트마다 다르다(`cursorOrderId`/`cursorRank`/`cursorTransferId`/
/// `cursor`). 억지로 추상화하지 않고 repository 시그니처에 그대로 노출한다.
@JsonSerializable(genericArgumentFactories: true)
class CursorPage<T> {
  const CursorPage({
    required this.content,
    required this.hasNext,
    this.nextCursor,
  });

  factory CursorPage.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json) fromJsonT,
  ) => _$CursorPageFromJson(json, fromJsonT);

  final List<T> content;
  final bool hasNext;
  final int? nextCursor;

  Map<String, dynamic> toJson(Object? Function(T value) toJsonT) =>
      _$CursorPageToJson(this, toJsonT);
}

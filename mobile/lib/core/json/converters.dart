import 'package:json_annotation/json_annotation.dart';

import '../format/server_time.dart';

/// 시각 컨버터 3종(계획서 §4.5.2). 클래스로 못 박아 새 DTO 를 추가할 때
/// 어느 시각 규약을 쓰는지가 강제 선택지가 되게 한다.

/// 오프셋 없는 서버 LocalDateTime — createdAt, filledAt, startedAt, endedAt, completedAt,
/// occurredAt, executedAt.
class KstDateTimeConverter implements JsonConverter<DateTime, String> {
  const KstDateTimeConverter();

  @override
  DateTime fromJson(String json) => ServerTime.parseKst(json);

  @override
  String toJson(DateTime object) => object.toIso8601String();
}

class NullableKstDateTimeConverter
    implements JsonConverter<DateTime?, String?> {
  const NullableKstDateTimeConverter();

  @override
  DateTime? fromJson(String? json) =>
      json == null ? null : ServerTime.parseKst(json);

  @override
  String? toJson(DateTime? object) => object?.toIso8601String();
}

/// UTC Instant — 캔들 `time` 전용.
class InstantConverter implements JsonConverter<DateTime, String> {
  const InstantConverter();

  @override
  DateTime fromJson(String json) => ServerTime.parseInstant(json);

  @override
  String toJson(DateTime object) => object.toUtc().toIso8601String();
}

class NullableInstantConverter implements JsonConverter<DateTime?, String?> {
  const NullableInstantConverter();

  @override
  DateTime? fromJson(String? json) =>
      json == null ? null : ServerTime.parseInstant(json);

  @override
  String? toJson(DateTime? object) => object?.toUtc().toIso8601String();
}

/// LocalDate — snapshotDate, referenceDate, analysisStart/End. 시간대 변환을 하지 않는다.
class LocalDateConverter implements JsonConverter<DateTime, String> {
  const LocalDateConverter();

  @override
  DateTime fromJson(String json) => ServerTime.parseLocalDate(json);

  @override
  String toJson(DateTime object) => ServerTime.formatLocalDate(object);
}

class NullableLocalDateConverter implements JsonConverter<DateTime?, String?> {
  const NullableLocalDateConverter();

  @override
  DateTime? fromJson(String? json) =>
      json == null ? null : ServerTime.parseLocalDate(json);

  @override
  String? toJson(DateTime? object) =>
      object == null ? null : ServerTime.formatLocalDate(object);
}

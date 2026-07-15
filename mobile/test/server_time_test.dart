import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/format/server_time.dart';
import 'package:trypto/core/json/converters.dart';

/// 사양서 R6. 캔들 `time` 만 UTC Instant 이고 나머지는 오프셋 없는 KST 다.
/// 기기 시간대에 관계없이 성립해야 하므로 단언은 전부 UTC 로 환산해서 한다.
void main() {
  group('ServerTime.parseKst — 오프셋 없는 서버 LocalDateTime', () {
    test('KST 10:30 은 UTC 01:30 이다', () {
      expect(
        ServerTime.parseKst('2026-07-15T10:30:00').toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30),
      );
    });

    test('자정을 넘겨 전날로 넘어간다', () {
      expect(
        ServerTime.parseKst('2026-07-15T08:00:00').toUtc(),
        DateTime.utc(2026, 7, 14, 23, 0),
      );
    });

    test('초 미만 자리를 보존한다', () {
      expect(
        ServerTime.parseKst('2026-07-15T10:30:00.123').toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30, 0, 123),
      );
    });

    test('기기 로컬 시각으로 돌려준다', () {
      expect(ServerTime.parseKst('2026-07-15T10:30:00').isUtc, isFalse);
    });
  });

  group('ServerTime.parseInstant — 캔들 time', () {
    test('Z 가 붙은 UTC 를 그대로 해석한다', () {
      expect(
        ServerTime.parseInstant('2026-07-15T01:30:00Z').toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30),
      );
    });

    test('같은 순간을 가리키는 두 표기가 일치한다', () {
      expect(
        ServerTime.parseKst('2026-07-15T10:30:00'),
        ServerTime.parseInstant('2026-07-15T01:30:00Z'),
      );
    });
  });

  group('ServerTime.parseLocalDate — 시간대 변환을 하지 않는다', () {
    test('날짜가 그대로 남는다', () {
      final date = ServerTime.parseLocalDate('2026-07-15');
      expect((date.year, date.month, date.day), (2026, 7, 15));
      expect(date.hour, 0);
      expect(date.isUtc, isFalse);
    });

    test('9시간을 당기지 않는다 (parseKst 와 다르다)', () {
      expect(ServerTime.parseLocalDate('2026-07-15').day, 15);
    });
  });

  group('ServerTime 포맷', () {
    test('formatLocalDate → yyyy-MM-dd', () {
      expect(ServerTime.formatLocalDate(DateTime(2026, 7, 5)), '2026-07-05');
    });

    test('formatDateTime → yyyy.MM.dd HH:mm (24시간제)', () {
      expect(
        ServerTime.formatDateTime(DateTime(2026, 7, 5, 15, 4)),
        '2026.07.05 15:04',
      );
    });
  });

  group('ServerTime.relative', () {
    final now = DateTime(2026, 7, 15, 12, 0);

    String at(Duration ago) =>
        ServerTime.relative(now.subtract(ago), now: now);

    test('1분 미만 → 방금 전', () {
      expect(at(const Duration(seconds: 30)), '방금 전');
      expect(at(Duration.zero), '방금 전');
    });

    test('1시간 미만 → N분 전', () {
      expect(at(const Duration(minutes: 1)), '1분 전');
      expect(at(const Duration(minutes: 59)), '59분 전');
    });

    test('24시간 미만 → N시간 전', () {
      expect(at(const Duration(minutes: 60)), '1시간 전');
      expect(at(const Duration(hours: 23, minutes: 59)), '23시간 전');
    });

    test('24시간 이상 → 절대 시각', () {
      expect(at(const Duration(hours: 24)), '2026.07.14 12:00');
    });

    test('미래 시각은 방금 전으로 떨어진다', () {
      expect(
        ServerTime.relative(now.add(const Duration(minutes: 5)), now: now),
        '방금 전',
      );
    });
  });

  group('컨버터 3종', () {
    test('KstDateTimeConverter', () {
      expect(
        const KstDateTimeConverter().fromJson('2026-07-15T10:30:00').toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30),
      );
    });

    test('InstantConverter', () {
      expect(
        const InstantConverter().fromJson('2026-07-15T01:30:00Z').toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30),
      );
      expect(
        const InstantConverter().toJson(DateTime.utc(2026, 7, 15, 1, 30)),
        '2026-07-15T01:30:00.000Z',
      );
    });

    test('LocalDateConverter 는 왕복한다', () {
      const converter = LocalDateConverter();
      expect(converter.toJson(converter.fromJson('2026-07-15')), '2026-07-15');
    });

    test('nullable 컨버터는 null 을 통과시킨다', () {
      expect(const NullableKstDateTimeConverter().fromJson(null), isNull);
      expect(const NullableInstantConverter().fromJson(null), isNull);
      expect(const NullableLocalDateConverter().fromJson(null), isNull);
      expect(const NullableKstDateTimeConverter().toJson(null), isNull);
      expect(const NullableLocalDateConverter().toJson(null), isNull);
    });

    test('nullable 컨버터도 값이 있으면 같은 규약을 쓴다', () {
      expect(
        const NullableKstDateTimeConverter()
            .fromJson('2026-07-15T10:30:00')!
            .toUtc(),
        DateTime.utc(2026, 7, 15, 1, 30),
      );
    });
  });
}

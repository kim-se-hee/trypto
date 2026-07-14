import 'package:intl/intl.dart';

/// 서버 시각 규약(사양서 R6).
///
/// 캔들 `time` 만 UTC Instant(`Z` 포함)이고, 나머지 시각 필드는 전부 오프셋이 없는
/// 서버 로컬시각(Asia/Seoul)이다. KST 는 1988년 이후 서머타임이 없어 고정 +9시간으로 정확하므로
/// 시간대 데이터베이스를 앱에 싣지 않는다.
class ServerTime {
  const ServerTime._();

  static const Duration kstOffset = Duration(hours: 9);

  /// 오프셋 없는 서버 LocalDateTime(`2026-07-15T10:30:00`) → 기기 로컬 시각.
  static DateTime parseKst(String raw) =>
      DateTime.parse('${raw}Z').subtract(kstOffset).toLocal();

  /// UTC Instant(`2026-07-15T01:30:00Z`) → 기기 로컬 시각. 캔들 `time` 전용.
  static DateTime parseInstant(String raw) => DateTime.parse(raw).toLocal();

  /// LocalDate(`2026-07-15`) → 시간대 변환 없는 그 날짜.
  static DateTime parseLocalDate(String raw) => DateTime.parse(raw);

  /// `referenceDate` 처럼 서버가 `yyyy-MM-dd` 로만 받는 파라미터용.
  static String formatLocalDate(DateTime date) => _date.format(date);

  static String formatDateTime(DateTime at) => _dateTime.format(at);

  /// 거래내역·송금내역의 상대 시각. 24시간을 넘으면 절대 시각으로 떨어진다.
  static String relative(DateTime at, {DateTime? now}) {
    final minutes = (now ?? DateTime.now()).difference(at).inMinutes;
    if (minutes < 1) return '방금 전';
    if (minutes < 60) return '$minutes분 전';
    final hours = minutes ~/ 60;
    if (hours < 24) return '$hours시간 전';
    return _dateTime.format(at);
  }
}

/// 숫자 패턴만 쓰므로 로케일이 출력에 영향을 주지 않는다. 로케일 데이터 초기화 없이
/// 항상 쓸 수 있는 `en_US` 로 고정한다.
const String _locale = 'en_US';

final DateFormat _dateTime = DateFormat('yyyy.MM.dd HH:mm', _locale);
final DateFormat _date = DateFormat('yyyy-MM-dd', _locale);

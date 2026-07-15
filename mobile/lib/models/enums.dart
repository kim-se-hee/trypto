import 'package:json_annotation/json_annotation.dart';

/// 서버 enum 을 그대로 쓴다(계획서 §4.5.3). 웹 프론트의 별칭 계층(`STOP_LOSS` ↔ `LOSS_CUT`)은
/// 순수한 프론트 관례이며 매핑 표만 하나 늘린다.
///
/// Dart 식별자는 lowerCamelCase 로 두고 와이어 값은 `@JsonValue` 로 못 박는다. `wire` 게터는
/// 쿼리 파라미터처럼 코드 생성을 거치지 않는 경로에서 쓴다.
///
/// 응답으로 도착하는 enum 에는 `unknown` 을 둔다. 서버가 값을 추가해도 앱이 죽지 않는다.

enum Side {
  @JsonValue('BUY')
  buy('BUY'),
  @JsonValue('SELL')
  sell('SELL'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const Side(this.wire);

  final String wire;
}

enum OrderType {
  @JsonValue('MARKET')
  market('MARKET'),
  @JsonValue('LIMIT')
  limit('LIMIT'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const OrderType(this.wire);

  final String wire;
}

enum OrderStatus {
  @JsonValue('FILLED')
  filled('FILLED'),
  @JsonValue('PENDING')
  pending('PENDING'),
  @JsonValue('CANCELED')
  canceled('CANCELED'),
  @JsonValue('FAILED')
  failed('FAILED'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const OrderStatus(this.wire);

  final String wire;
}

/// 조회 필터 겸 응답 값. 서버가 필터 미지정 시 `ALL` 로 채운다.
enum TransferType {
  @JsonValue('ALL')
  all('ALL'),
  @JsonValue('DEPOSIT')
  deposit('DEPOSIT'),
  @JsonValue('WITHDRAW')
  withdraw('WITHDRAW'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const TransferType(this.wire);

  final String wire;
}

/// 서버 `TransferStatus` 는 값이 하나뿐이다(사양서 R9). 송금은 동기·즉시 완료된다.
/// 웹이 가정한 6종(PENDING/PROCESSING/…)은 존재하지 않는다.
enum TransferStatus {
  @JsonValue('SUCCESS')
  success('SUCCESS'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const TransferStatus(this.wire);

  final String wire;
}

/// `BANKRUPT` 는 운영 코드에 전이 경로가 없다(시드 데이터에만 존재). 표시만 지원한다.
enum RoundStatus {
  @JsonValue('ACTIVE')
  active('ACTIVE'),
  @JsonValue('BANKRUPT')
  bankrupt('BANKRUPT'),
  @JsonValue('ENDED')
  ended('ENDED'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const RoundStatus(this.wire);

  final String wire;
}

/// 5종 전부 처리한다(과거 라운드가 손절·익절을 가질 수 있다). 라운드 **생성** 화면에서 고를 수
/// 있는 것은 서버에 위반 판정 로직이 있는 3종뿐이다.
enum RuleType {
  @JsonValue('LOSS_CUT')
  lossCut('LOSS_CUT'),
  @JsonValue('PROFIT_TAKE')
  profitTake('PROFIT_TAKE'),
  @JsonValue('CHASE_BUY_BAN')
  chaseBuyBan('CHASE_BUY_BAN'),
  @JsonValue('AVERAGING_DOWN_LIMIT')
  averagingDownLimit('AVERAGING_DOWN_LIMIT'),
  @JsonValue('OVERTRADING_LIMIT')
  overtradingLimit('OVERTRADING_LIMIT'),
  @JsonValue('UNKNOWN')
  unknown('UNKNOWN');

  const RuleType(this.wire);

  final String wire;

  /// 라운드 생성 화면이 제시하는 3종.
  static const List<RuleType> selectable = [
    chaseBuyBan,
    averagingDownLimit,
    overtradingLimit,
  ];
}

/// 집계 주기가 아니라 수익률 산출 구간이다. 셋 다 매일 갱신된다.
enum RankingPeriod {
  @JsonValue('DAILY')
  daily('DAILY'),
  @JsonValue('WEEKLY')
  weekly('WEEKLY'),
  @JsonValue('MONTHLY')
  monthly('MONTHLY');

  const RankingPeriod(this.wire);

  final String wire;
}

/// 경로 변수로만 쓴다. 서버가 대소문자를 가리지 않지만 소문자로 보낸다.
///
/// 서버 enum 이름은 `Provider` 지만 Riverpod 의 `Provider` 와 이름이 겹친다. 모든 repository 가
/// 두 라이브러리를 함께 import 하므로 Dart 쪽 이름만 바꾼다(와이어 값은 그대로다).
enum SocialProvider {
  kakao('kakao'),
  google('google');

  const SocialProvider(this.wire);

  final String wire;
}

/// 로그인 바디의 `clientType`. 서버는 제공자 자격증명을 이 값으로 고른다 — 구글의 Android·iOS
/// 클라이언트 ID 가 별개이기 때문이다. 생략하면 서버가 `WEB` 으로 간주한다.
enum ClientType {
  @JsonValue('web')
  web('web'),
  @JsonValue('android')
  android('android'),
  @JsonValue('ios')
  ios('ios');

  const ClientType(this.wire);

  final String wire;
}

/// 캔들 주기 6종. 대소문자를 구분한다(`1M` 은 월봉, `1m` 은 분봉).
enum CandleInterval {
  minute1('1m'),
  hour1('1h'),
  hour4('4h'),
  day1('1d'),
  week1('1w'),
  month1('1M');

  const CandleInterval(this.wire);

  final String wire;
}

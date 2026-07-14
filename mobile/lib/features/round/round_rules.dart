import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../models/enums.dart';
import '../../models/round.dart';

/// 라운드 생성의 입력 규칙 전량. 위젯 없이 테스트한다 — 웹은 이 검증을 전혀 하지 않아
/// 제출 시점에 400·500 으로 실패한다(사양서 R8, §7.3.1).

enum RuleInputKind { slider, stepper }

/// 사양서 §7.3.2 의 규칙 표 그대로. 생성 화면에서 고를 수 있는 것은 서버에 위반 판정 로직이
/// 있는 3종뿐이다(`LOSS_CUT`·`PROFIT_TAKE` 은 `check()` 가 항상 비어 있다).
class RuleConfig {
  const RuleConfig({
    required this.ruleType,
    required this.label,
    required this.description,
    required this.defaultValue,
    required this.min,
    required this.max,
    required this.unit,
    required this.input,
  });

  final RuleType ruleType;
  final String label;
  final String description;
  final int defaultValue;
  final int min;
  final int max;
  final String unit;
  final RuleInputKind input;

  int clamp(int value) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }
}

const List<RuleConfig> ruleConfigs = [
  RuleConfig(
    ruleType: RuleType.chaseBuyBan,
    label: '추격 매수 금지',
    description: '급등 코인 매수를 방지',
    defaultValue: 15,
    min: 1,
    max: 50,
    unit: '%',
    input: RuleInputKind.slider,
  ),
  RuleConfig(
    ruleType: RuleType.averagingDownLimit,
    label: '물타기 제한',
    description: '손실 중인 코인의 추가 매수 횟수 제한',
    defaultValue: 3,
    min: 1,
    max: 10,
    unit: '회',
    input: RuleInputKind.stepper,
  ),
  RuleConfig(
    ruleType: RuleType.overtradingLimit,
    label: '과매매 제한',
    description: '하루 거래 횟수 제한',
    defaultValue: 10,
    min: 1,
    max: 50,
    unit: '회/일',
    input: RuleInputKind.stepper,
  ),
];

/// 표시 계층은 5종을 전부 처리해야 한다 — 과거 라운드가 손절·익절을 갖고 있을 수 있다(§7.3.2).
const Map<RuleType, String> ruleLabels = {
  RuleType.lossCut: '손절',
  RuleType.profitTake: '익절',
  RuleType.chaseBuyBan: '추격 매수 금지',
  RuleType.averagingDownLimit: '물타기 제한',
  RuleType.overtradingLimit: '과매매 제한',
  RuleType.unknown: '알 수 없는 원칙',
};

const Map<RuleType, String> ruleUnits = {
  RuleType.lossCut: '%',
  RuleType.profitTake: '%',
  RuleType.chaseBuyBan: '%',
  RuleType.averagingDownLimit: '회',
  RuleType.overtradingLimit: '회/일',
  RuleType.unknown: '',
};

/// 서버 `SeedAmountPolicy`(사양서 §1.6.4). 0 은 '배정 안 함' 이라 범위 검사를 건너뛴다.
abstract final class SeedPolicy {
  static const int domesticMin = 1000000;
  static const int domesticMax = 50000000;
  static const int foreignMin = 100;
  static const int foreignMax = 50000;

  /// 시드머니를 몰아 주는 지갑. 웹과 같이 업비트에 전액, 나머지 거래소에 0 을 보낸다(§7.3.3).
  static const int seedExchangeId = ExchangeIds.upbit;

  static bool isDomestic(int exchangeId) =>
      ExchangeIds.byId(exchangeId)?.baseCurrency != 'USDT';

  static String? validate(int exchangeId, int amount) {
    if (amount < 0) return '시드머니는 0 이상이어야 합니다.';
    if (amount == 0) return null;
    if (isDomestic(exchangeId)) {
      if (amount < domesticMin || amount > domesticMax) {
        return '시드머니는 100만원 이상 5,000만원 이하여야 합니다.';
      }
      return null;
    }
    if (amount < foreignMin || amount > foreignMax) {
      return '시드머니는 100 USDT 이상 50,000 USDT 이하여야 합니다.';
    }
    return null;
  }
}

/// 서버 `EmergencyFundingAllowance`(§1.6.4). 상한 초과는 400 `INVALID_EMERGENCY_FUNDING_LIMIT` 다.
abstract final class EmergencyFundingPolicy {
  static const int maxLimit = 1000000;
  static const int chargeCount = 3;

  static String? validate(int limit) {
    if (limit <= 0) return '긴급 자금 상한을 입력해 주세요.';
    if (limit > maxLimit) return '긴급 자금 상한은 100만원입니다.';
    return null;
  }

  /// 투입 금액 검증 — `0 < amount <= limit`(§4.5). **입력 단계에서** 막는다. 웹은 제출 시점에
  /// `INVALID_EMERGENCY_FUNDING_AMOUNT` 를 받고도 화면에 아무 표시를 하지 않는다.
  static String? validateCharge(int amount, int limit) {
    if (limit <= 0) return '이 라운드는 긴급 자금을 쓸 수 없습니다.';
    if (amount <= 0) return '투입 금액을 입력해 주세요.';
    if (amount > limit) {
      return '상한을 초과했습니다. ${formatGrouped(limit)}원 이하로 입력해주세요.';
    }
    return null;
  }

  /// 프리셋은 상한의 25% / 50% / 100% 이며 각각 내림한다(§4.5).
  static List<int> presets(int limit) => [
    for (final ratio in [25, 50, 100]) limit * ratio ~/ 100,
  ];
}

/// 라운드 생성 화면의 입력값. [rules] 가 `Map` 이므로 같은 `ruleType` 이 두 번 실릴 수
/// **구조적으로** 없다 — 중복을 보내면 서버가 400 이 아니라 500 을 낸다(R8-2).
class RoundDraft {
  const RoundDraft({
    required this.seed,
    required this.emergencyLimit,
    required this.rules,
  });

  final int seed;
  final int emergencyLimit;
  final Map<RuleType, int> rules;

  String? get seedError {
    if (seed <= 0) return '시드머니를 입력해 주세요.';
    return SeedPolicy.validate(SeedPolicy.seedExchangeId, seed);
  }

  String? get emergencyError => EmergencyFundingPolicy.validate(emergencyLimit);

  String? get rulesError =>
      rules.isEmpty ? '최소 1개 이상의 원칙을 활성화해 주세요.' : null;

  bool get canSubmit =>
      seedError == null && emergencyError == null && rulesError == null;

  StartRoundRequest toRequest() {
    return StartRoundRequest(
      seeds: [
        for (final exchange in ExchangeIds.all)
          SeedRequest(
            exchangeId: exchange.id,
            amount: exchange.id == SeedPolicy.seedExchangeId
                ? seed.toDouble()
                : 0,
          ),
      ],
      emergencyFundingLimit: emergencyLimit.toDouble(),
      rules: [
        for (final entry in rules.entries)
          RuleRequest(
            ruleType: entry.key,
            thresholdValue: entry.value.toDouble(),
          ),
      ],
    );
  }
}

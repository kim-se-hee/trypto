import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'round.g.dart';

/// `POST /api/rounds` — **`userId` 를 보내지 않는다**(사양서 R4-2). 서버 DTO 에 필드가 없다.
@JsonSerializable(createFactory: false)
class StartRoundRequest {
  const StartRoundRequest({
    required this.seeds,
    required this.emergencyFundingLimit,
    required this.rules,
  });

  final List<SeedRequest> seeds;

  /// 0 ~ 1,000,000. 입력 단계에서 강제한다.
  final double emergencyFundingLimit;

  /// 동일 `ruleType` 중복 전송은 클라이언트가 선제 차단한다 — 서버가 400 이 아니라 500 을 낸다(R8).
  final List<RuleRequest> rules;

  Map<String, dynamic> toJson() => _$StartRoundRequestToJson(this);
}

/// 0 은 '배정 안 함'. 0 초과면 국내 100만~5,000만, 해외 100~50,000 범위여야 한다.
@JsonSerializable(createFactory: false)
class SeedRequest {
  const SeedRequest({required this.exchangeId, required this.amount});

  final int exchangeId;
  final double amount;

  Map<String, dynamic> toJson() => _$SeedRequestToJson(this);
}

/// 비율형(`CHASE_BUY_BAN` 등)은 0 초과 실수, 횟수형은 1 이상 정수여야 한다.
@JsonSerializable(createFactory: false)
class RuleRequest {
  const RuleRequest({required this.ruleType, required this.thresholdValue});

  final RuleType ruleType;
  final double thresholdValue;

  Map<String, dynamic> toJson() => _$RuleRequestToJson(this);
}

@JsonSerializable(createToJson: false)
class RoundRule {
  const RoundRule({
    required this.ruleId,
    required this.ruleType,
    required this.thresholdValue,
  });

  factory RoundRule.fromJson(Map<String, dynamic> json) =>
      _$RoundRuleFromJson(json);

  final int ruleId;

  @JsonKey(unknownEnumValue: RuleType.unknown)
  final RuleType ruleType;

  final double thresholdValue;
}

/// **`walletId` 는 라운드 응답에서만 얻는다.** 다른 모든 지갑 API 의 경로 변수가 이 값이다.
@JsonSerializable(createToJson: false)
class RoundWallet {
  const RoundWallet({required this.walletId, required this.exchangeId});

  factory RoundWallet.fromJson(Map<String, dynamic> json) =>
      _$RoundWalletFromJson(json);

  final int walletId;
  final int exchangeId;
}

/// `POST /api/rounds` 응답 — **`userId`·`endedAt` 이 없다**(사양서 R4-3).
/// [ActiveRound] 와 모양이 달라 모델을 둘로 나눈다.
@JsonSerializable(createToJson: false)
class StartRoundResponse {
  const StartRoundResponse({
    required this.roundId,
    required this.roundNumber,
    required this.status,
    required this.initialSeed,
    required this.emergencyFundingLimit,
    required this.emergencyChargeCount,
    required this.rules,
    required this.wallets,
    required this.startedAt,
  });

  factory StartRoundResponse.fromJson(Map<String, dynamic> json) =>
      _$StartRoundResponseFromJson(json);

  final int roundId;
  final int roundNumber;

  @JsonKey(unknownEnumValue: RoundStatus.unknown)
  final RoundStatus status;

  final double initialSeed;
  final double emergencyFundingLimit;
  final int emergencyChargeCount;
  final List<RoundRule> rules;
  final List<RoundWallet> wallets;

  @KstDateTimeConverter()
  final DateTime startedAt;
}

/// `GET /api/rounds/active` 응답. 없으면 409 `ROUND_NOT_ACTIVE` → repository 가 `null` 로 바꾼다.
///
/// 라운드·마켓·포트폴리오·지갑·복기가 전부 이 모델을 쓴다. 그래서 feature 가 아니라 `models/` 에 둔다.
@JsonSerializable(createToJson: false)
class ActiveRound {
  const ActiveRound({
    required this.roundId,
    required this.userId,
    required this.roundNumber,
    required this.status,
    required this.initialSeed,
    required this.emergencyFundingLimit,
    required this.emergencyChargeCount,
    required this.startedAt,
    required this.rules,
    required this.wallets,
    this.endedAt,
  });

  factory ActiveRound.fromJson(Map<String, dynamic> json) =>
      _$ActiveRoundFromJson(json);

  final int roundId;
  final int userId;
  final int roundNumber;

  @JsonKey(unknownEnumValue: RoundStatus.unknown)
  final RoundStatus status;

  final double initialSeed;
  final double emergencyFundingLimit;

  /// 라운드 생성 시 3회로 고정 부여된 뒤 차감된다.
  final int emergencyChargeCount;

  @KstDateTimeConverter()
  final DateTime startedAt;

  /// 진행 중인 라운드에서는 null 이다.
  @NullableKstDateTimeConverter()
  final DateTime? endedAt;

  final List<RoundRule> rules;
  final List<RoundWallet> wallets;

  /// 거래소 → 지갑 해석(사양서 §1.10.2-2). 해당 거래소에 지갑이 없으면 null.
  int? walletIdOf(int exchangeId) {
    for (final wallet in wallets) {
      if (wallet.exchangeId == exchangeId) return wallet.walletId;
    }
    return null;
  }

  /// 긴급 자금 충전 응답의 `remainingChargeCount` 를 들인다. 서버가 중복 요청(같은 멱등키)을
  /// 감지하면 재차감 없이 현재 잔여 횟수를 돌려주므로 그 값을 그대로 덮어쓴다.
  ActiveRound withEmergencyChargeCount(int count) => ActiveRound(
    roundId: roundId,
    userId: userId,
    roundNumber: roundNumber,
    status: status,
    initialSeed: initialSeed,
    emergencyFundingLimit: emergencyFundingLimit,
    emergencyChargeCount: count,
    startedAt: startedAt,
    endedAt: endedAt,
    rules: rules,
    wallets: wallets,
  );
}

/// `POST /api/rounds/{roundId}/end` — 요청 바디가 없다(서버가 읽지 않는다, R4-4).
@JsonSerializable(createToJson: false)
class EndRoundResponse {
  const EndRoundResponse({
    required this.roundId,
    required this.status,
    this.endedAt,
  });

  factory EndRoundResponse.fromJson(Map<String, dynamic> json) =>
      _$EndRoundResponseFromJson(json);

  final int roundId;

  @JsonKey(unknownEnumValue: RoundStatus.unknown)
  final RoundStatus status;

  @NullableKstDateTimeConverter()
  final DateTime? endedAt;
}

/// `GET /api/rounds/summary`
@JsonSerializable(createToJson: false)
class RoundSummary {
  const RoundSummary({required this.totalRoundCount});

  factory RoundSummary.fromJson(Map<String, dynamic> json) =>
      _$RoundSummaryFromJson(json);

  final int totalRoundCount;
}

/// `POST /api/rounds/{roundId}/emergency-funding`
///
/// [idempotencyKey] 는 **UUID v4 형식이 필수**다. 형식이 어긋나면 서버가 400 이 아니라
/// 500 을 낸다(사양서 R8-1). 재시도 시 같은 키를 재사용한다.
@JsonSerializable(createFactory: false)
class ChargeEmergencyFundingRequest {
  const ChargeEmergencyFundingRequest({
    required this.exchangeId,
    required this.amount,
    required this.idempotencyKey,
  });

  final int exchangeId;

  /// `0 < amount <= emergencyFundingLimit`
  final double amount;

  final String idempotencyKey;

  Map<String, dynamic> toJson() => _$ChargeEmergencyFundingRequestToJson(this);
}

/// 웹이 선언한 `chargedAt` 은 서버에 없다(사양서 R4-5).
@JsonSerializable(createToJson: false)
class ChargeEmergencyFundingResponse {
  const ChargeEmergencyFundingResponse({
    required this.roundId,
    required this.exchangeId,
    required this.chargedAmount,
    required this.remainingChargeCount,
  });

  factory ChargeEmergencyFundingResponse.fromJson(Map<String, dynamic> json) =>
      _$ChargeEmergencyFundingResponseFromJson(json);

  final int roundId;
  final int exchangeId;
  final double chargedAmount;
  final int remainingChargeCount;
}

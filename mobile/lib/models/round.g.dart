// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'round.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Map<String, dynamic> _$StartRoundRequestToJson(StartRoundRequest instance) =>
    <String, dynamic>{
      'seeds': instance.seeds,
      'emergencyFundingLimit': instance.emergencyFundingLimit,
      'rules': instance.rules,
    };

Map<String, dynamic> _$SeedRequestToJson(SeedRequest instance) =>
    <String, dynamic>{
      'exchangeId': instance.exchangeId,
      'amount': instance.amount,
    };

Map<String, dynamic> _$RuleRequestToJson(RuleRequest instance) =>
    <String, dynamic>{
      'ruleType': _$RuleTypeEnumMap[instance.ruleType]!,
      'thresholdValue': instance.thresholdValue,
    };

const _$RuleTypeEnumMap = {
  RuleType.lossCut: 'LOSS_CUT',
  RuleType.profitTake: 'PROFIT_TAKE',
  RuleType.chaseBuyBan: 'CHASE_BUY_BAN',
  RuleType.averagingDownLimit: 'AVERAGING_DOWN_LIMIT',
  RuleType.overtradingLimit: 'OVERTRADING_LIMIT',
  RuleType.unknown: 'UNKNOWN',
};

RoundRule _$RoundRuleFromJson(Map<String, dynamic> json) => RoundRule(
  ruleId: (json['ruleId'] as num).toInt(),
  ruleType: $enumDecode(
    _$RuleTypeEnumMap,
    json['ruleType'],
    unknownValue: RuleType.unknown,
  ),
  thresholdValue: (json['thresholdValue'] as num).toDouble(),
);

RoundWallet _$RoundWalletFromJson(Map<String, dynamic> json) => RoundWallet(
  walletId: (json['walletId'] as num).toInt(),
  exchangeId: (json['exchangeId'] as num).toInt(),
);

StartRoundResponse _$StartRoundResponseFromJson(Map<String, dynamic> json) =>
    StartRoundResponse(
      roundId: (json['roundId'] as num).toInt(),
      roundNumber: (json['roundNumber'] as num).toInt(),
      status: $enumDecode(
        _$RoundStatusEnumMap,
        json['status'],
        unknownValue: RoundStatus.unknown,
      ),
      initialSeed: (json['initialSeed'] as num).toDouble(),
      emergencyFundingLimit: (json['emergencyFundingLimit'] as num).toDouble(),
      emergencyChargeCount: (json['emergencyChargeCount'] as num).toInt(),
      rules: (json['rules'] as List<dynamic>)
          .map((e) => RoundRule.fromJson(e as Map<String, dynamic>))
          .toList(),
      wallets: (json['wallets'] as List<dynamic>)
          .map((e) => RoundWallet.fromJson(e as Map<String, dynamic>))
          .toList(),
      startedAt: const KstDateTimeConverter().fromJson(
        json['startedAt'] as String,
      ),
    );

const _$RoundStatusEnumMap = {
  RoundStatus.active: 'ACTIVE',
  RoundStatus.bankrupt: 'BANKRUPT',
  RoundStatus.ended: 'ENDED',
  RoundStatus.unknown: 'UNKNOWN',
};

ActiveRound _$ActiveRoundFromJson(Map<String, dynamic> json) => ActiveRound(
  roundId: (json['roundId'] as num).toInt(),
  userId: (json['userId'] as num).toInt(),
  roundNumber: (json['roundNumber'] as num).toInt(),
  status: $enumDecode(
    _$RoundStatusEnumMap,
    json['status'],
    unknownValue: RoundStatus.unknown,
  ),
  initialSeed: (json['initialSeed'] as num).toDouble(),
  emergencyFundingLimit: (json['emergencyFundingLimit'] as num).toDouble(),
  emergencyChargeCount: (json['emergencyChargeCount'] as num).toInt(),
  startedAt: const KstDateTimeConverter().fromJson(json['startedAt'] as String),
  rules: (json['rules'] as List<dynamic>)
      .map((e) => RoundRule.fromJson(e as Map<String, dynamic>))
      .toList(),
  wallets: (json['wallets'] as List<dynamic>)
      .map((e) => RoundWallet.fromJson(e as Map<String, dynamic>))
      .toList(),
  endedAt: const NullableKstDateTimeConverter().fromJson(
    json['endedAt'] as String?,
  ),
);

EndRoundResponse _$EndRoundResponseFromJson(Map<String, dynamic> json) =>
    EndRoundResponse(
      roundId: (json['roundId'] as num).toInt(),
      status: $enumDecode(
        _$RoundStatusEnumMap,
        json['status'],
        unknownValue: RoundStatus.unknown,
      ),
      endedAt: const NullableKstDateTimeConverter().fromJson(
        json['endedAt'] as String?,
      ),
    );

RoundSummary _$RoundSummaryFromJson(Map<String, dynamic> json) =>
    RoundSummary(totalRoundCount: (json['totalRoundCount'] as num).toInt());

Map<String, dynamic> _$ChargeEmergencyFundingRequestToJson(
  ChargeEmergencyFundingRequest instance,
) => <String, dynamic>{
  'exchangeId': instance.exchangeId,
  'amount': instance.amount,
  'idempotencyKey': instance.idempotencyKey,
};

ChargeEmergencyFundingResponse _$ChargeEmergencyFundingResponseFromJson(
  Map<String, dynamic> json,
) => ChargeEmergencyFundingResponse(
  roundId: (json['roundId'] as num).toInt(),
  exchangeId: (json['exchangeId'] as num).toInt(),
  chargedAmount: (json['chargedAmount'] as num).toDouble(),
  remainingChargeCount: (json['remainingChargeCount'] as num).toInt(),
);

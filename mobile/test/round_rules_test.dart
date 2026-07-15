import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/constants/exchanges.dart';
import 'package:trypto/features/round/round_rules.dart';
import 'package:trypto/models/enums.dart';

/// 서버가 400 이 아니라 500 을 내는 입력(R8)과, 웹이 제출 시점까지 미루는 검증을
/// 입력 단계에서 잡는지 고정한다.
void main() {
  RoundDraft draft({
    int seed = 10000000,
    int emergencyLimit = 500000,
    Map<RuleType, int> rules = const {RuleType.chaseBuyBan: 15},
  }) => RoundDraft(seed: seed, emergencyLimit: emergencyLimit, rules: rules);

  group('시드머니 범위 (국내 100만 ~ 5,000만)', () {
    test('0 은 배정 안 함이라 범위 검사를 건너뛴다', () {
      expect(SeedPolicy.validate(ExchangeIds.upbit, 0), isNull);
    });

    test('경계값은 통과한다', () {
      expect(SeedPolicy.validate(ExchangeIds.upbit, 1000000), isNull);
      expect(SeedPolicy.validate(ExchangeIds.bithumb, 50000000), isNull);
    });

    test('경계 밖은 막는다', () {
      expect(SeedPolicy.validate(ExchangeIds.upbit, 999999), isNotNull);
      expect(SeedPolicy.validate(ExchangeIds.upbit, 50000001), isNotNull);
    });
  });

  group('시드머니 범위 (해외 100 ~ 50,000)', () {
    test('바이낸스는 USDT 기준 범위를 쓴다', () {
      expect(SeedPolicy.validate(ExchangeIds.binance, 100), isNull);
      expect(SeedPolicy.validate(ExchangeIds.binance, 50000), isNull);
      expect(SeedPolicy.validate(ExchangeIds.binance, 99), isNotNull);
      expect(SeedPolicy.validate(ExchangeIds.binance, 50001), isNotNull);
    });

    test('국내 판별은 기준통화로 한다', () {
      expect(SeedPolicy.isDomestic(ExchangeIds.upbit), isTrue);
      expect(SeedPolicy.isDomestic(ExchangeIds.bithumb), isTrue);
      expect(SeedPolicy.isDomestic(ExchangeIds.binance), isFalse);
    });
  });

  group('긴급 자금 상한 (0 초과 ~ 100만)', () {
    test('100만원 초과는 입력 단계에서 막는다', () {
      expect(EmergencyFundingPolicy.validate(1000001), '긴급 자금 상한은 100만원입니다.');
    });

    test('0 이하는 제출 조건을 채우지 못한다', () {
      expect(EmergencyFundingPolicy.validate(0), isNotNull);
    });

    test('상한값 자체는 통과한다', () {
      expect(EmergencyFundingPolicy.validate(1000000), isNull);
    });
  });

  group('긴급 자금 투입 금액 (0 < amount <= limit)', () {
    test('상한을 넘으면 입력 단계에서 막는다', () {
      expect(
        EmergencyFundingPolicy.validateCharge(1000001, 1000000),
        '상한을 초과했습니다. 1,000,000원 이하로 입력해주세요.',
      );
    });

    test('상한값과 0 초과 경계는 통과한다', () {
      expect(EmergencyFundingPolicy.validateCharge(1000000, 1000000), isNull);
      expect(EmergencyFundingPolicy.validateCharge(1, 1000000), isNull);
    });

    test('0 이하는 막는다', () {
      expect(EmergencyFundingPolicy.validateCharge(0, 1000000), isNotNull);
      expect(EmergencyFundingPolicy.validateCharge(-1, 1000000), isNotNull);
    });

    test('상한이 0 인 라운드는 긴급 자금을 쓸 수 없다', () {
      expect(EmergencyFundingPolicy.validateCharge(1000, 0), isNotNull);
    });

    test('프리셋은 상한의 25 / 50 / 100% 를 내림한 값이다', () {
      expect(EmergencyFundingPolicy.presets(1000000), [250000, 500000, 1000000]);
      expect(EmergencyFundingPolicy.presets(333333), [83333, 166666, 333333]);
    });
  });

  group('제출 조건 — seed > 0 && emergencyLimit > 0 && 활성 규칙 >= 1', () {
    test('셋을 모두 채우면 제출할 수 있다', () {
      expect(draft().canSubmit, isTrue);
    });

    test('시드머니가 없으면 막는다', () {
      expect(draft(seed: 0).canSubmit, isFalse);
      expect(draft(seed: 0).seedError, '시드머니를 입력해 주세요.');
    });

    test('시드머니가 범위 밖이면 막는다', () {
      expect(draft(seed: 60000000).canSubmit, isFalse);
    });

    test('긴급 자금 상한이 100만원을 넘으면 막는다', () {
      expect(draft(emergencyLimit: 2000000).canSubmit, isFalse);
    });

    test('활성 규칙이 하나도 없으면 막는다', () {
      final empty = draft(rules: const {});
      expect(empty.canSubmit, isFalse);
      expect(empty.rulesError, isNotNull);
    });
  });

  group('요청 조립', () {
    /// 중첩 DTO 는 `toJson()` 이 인스턴스 그대로 담고 `jsonEncode` 가 펼친다. 실제 바디를
    /// 검사해야 계약이 고정된다.
    Map<String, dynamic> body(RoundDraft draft) =>
        jsonDecode(jsonEncode(draft.toRequest().toJson()))
            as Map<String, dynamic>;

    test('시드는 거래소 3개로 펼치고 업비트에 전액을 싣는다', () {
      final json = body(draft(seed: 10000000));
      expect(json['seeds'], [
        {'exchangeId': ExchangeIds.upbit, 'amount': 10000000.0},
        {'exchangeId': ExchangeIds.bithumb, 'amount': 0.0},
        {'exchangeId': ExchangeIds.binance, 'amount': 0.0},
      ]);
      expect(json['emergencyFundingLimit'], 500000.0);
    });

    test('userId 를 보내지 않는다', () {
      expect(body(draft()).containsKey('userId'), isFalse);
    });

    test('같은 ruleType 은 두 번 실리지 않는다 (서버가 500 을 낸다)', () {
      final rules = draft(
        rules: const {
          RuleType.chaseBuyBan: 15,
          RuleType.averagingDownLimit: 3,
          RuleType.overtradingLimit: 10,
        },
      ).toRequest().rules;

      final types = rules.map((rule) => rule.ruleType).toList();
      expect(types.toSet().length, types.length);
      expect(types.length, 3);
    });

    test('임계값과 ruleType 와이어 값을 그대로 싣는다', () {
      final json = body(draft(rules: const {RuleType.overtradingLimit: 7}));
      expect(json['rules'], [
        {'ruleType': 'OVERTRADING_LIMIT', 'thresholdValue': 7.0},
      ]);
    });
  });

  group('규칙 설정표 (사양서 §7.3.2)', () {
    test('생성 화면이 제시하는 것은 판정 로직이 있는 3종뿐이다', () {
      expect(
        ruleConfigs.map((config) => config.ruleType).toList(),
        RuleType.selectable,
      );
    });

    test('기본값·범위·입력 방식이 규칙 표와 같다', () {
      final byType = {
        for (final config in ruleConfigs) config.ruleType: config,
      };

      final chase = byType[RuleType.chaseBuyBan]!;
      expect([chase.defaultValue, chase.min, chase.max], [15, 1, 50]);
      expect(chase.input, RuleInputKind.slider);
      expect(chase.unit, '%');

      final averaging = byType[RuleType.averagingDownLimit]!;
      expect([averaging.defaultValue, averaging.min, averaging.max], [3, 1, 10]);
      expect(averaging.input, RuleInputKind.stepper);

      final overtrading = byType[RuleType.overtradingLimit]!;
      expect([
        overtrading.defaultValue,
        overtrading.min,
        overtrading.max,
      ], [10, 1, 50]);
      expect(overtrading.unit, '회/일');
    });

    test('입력값은 범위로 clamp 한다', () {
      final averaging = ruleConfigs.firstWhere(
        (config) => config.ruleType == RuleType.averagingDownLimit,
      );
      expect(averaging.clamp(0), 1);
      expect(averaging.clamp(99), 10);
      expect(averaging.clamp(5), 5);
    });

    test('표시 라벨은 손절·익절까지 5종을 모두 갖는다', () {
      for (final type in RuleType.values) {
        expect(ruleLabels[type], isNotNull, reason: type.wire);
        expect(ruleUnits[type], isNotNull, reason: type.wire);
      }
    });
  });
}

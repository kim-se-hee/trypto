import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../models/enums.dart';
import '../../models/round.dart';
import '../auth/auth_controller.dart';
import 'round_repository.dart';
import 'round_rules.dart';

/// 라우트 가드의 입력(사양서 §2.7.1). `hasActive`(지금 활성 라운드가 있는가)와
/// `hasEverStarted`(한 번이라도 시작한 적 있는가)는 **별개**다 — 신규 사용자는 라운드 생성을
/// 건너뛸 수 없고, 라운드를 끝낸 기존 사용자는 라운드 없이도 앱을 둘러볼 수 있다.
class RoundState {
  const RoundState({
    required this.isLoading,
    this.activeRound,
    this.totalRoundCount = 0,
    this.errorMessage,
  });

  const RoundState.loading() : this(isLoading: true);

  /// 미인증이면 조회하지 않고 즉시 '라운드 없음' 으로 확정한다.
  const RoundState.signedOut() : this(isLoading: false);

  final bool isLoading;
  final ActiveRound? activeRound;
  final int totalRoundCount;
  final String? errorMessage;

  bool get hasActive => activeRound != null;

  bool get hasEverStarted => totalRoundCount > 0;

  /// 거래소 → 지갑. 지갑 API 의 경로 변수는 전부 이 값이다.
  int? walletIdOf(int exchangeId) => activeRound?.walletIdOf(exchangeId);
}

class RoundController extends Notifier<RoundState> {
  int _generation = 0;

  @override
  RoundState build() {
    final authed = ref.watch(
      authControllerProvider.select((auth) => auth.isAuthenticated),
    );

    // 로그아웃·세션 만료 시 라운드 상태(exchangeId → walletId 맵)를 비운다. 남겨 두면 다음
    // 로그인 사용자가 이전 사용자의 walletId 로 첫 요청을 보낸다(계획서 §4.1.3).
    final generation = ++_generation;
    if (!authed) return const RoundState.signedOut();

    scheduleMicrotask(() => _load(generation));
    return const RoundState.loading();
  }

  Future<void> refresh() => _load(_generation);

  /// 성공하면 `null`, 실패하면 사용자에게 보일 메시지를 돌려준다. 웹은 서버 오류를 삼키고
  /// "입력값을 다시 확인해 주세요" 한 줄로 뭉갠다(사양서 §7.3.3) — 여기서는 그대로 노출한다.
  ///
  /// 생성 응답에는 `userId` 가 없어 [ActiveRound] 를 그대로 만들 수 없다(R4-3). 서버 진실을
  /// 들이려고 재조회한다. 로딩 상태로 되돌리지 않으므로 가드가 스플래시를 스치지 않는다.
  Future<String?> createRound(StartRoundRequest request) async {
    try {
      await ref.read(roundRepositoryProvider).startRound(request);
    } on ApiException catch (error) {
      return error.userMessage;
    }
    await _load(_generation);
    if (state.hasActive) return null;
    return state.errorMessage ?? '라운드를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  }

  /// 긴급 자금 투입. 성공하면 `null`, 실패하면 사용자에게 보일 메시지를 돌려준다.
  ///
  /// 로컬 선검사에 걸리면 **네트워크 호출을 하지 않는다**(사양서 §4.5). [idempotencyKey] 는
  /// UUID v4 여야 하며(형식이 틀리면 서버가 500 을 낸다) 재시도 시 같은 값을 재사용한다 —
  /// 호출부가 화면 상태에 보관한다.
  Future<String?> chargeEmergencyFunding({
    required int exchangeId,
    required int amount,
    required String idempotencyKey,
  }) async {
    final round = state.activeRound;
    if (round == null) return '진행 중인 라운드가 없습니다.';
    if (round.status != RoundStatus.active) {
      return '진행 중인 라운드가 아닙니다.';
    }
    if (round.emergencyChargeCount <= 0) {
      return '긴급 자금 투입 횟수를 모두 사용했습니다.';
    }
    final invalid = EmergencyFundingPolicy.validateCharge(
      amount,
      round.emergencyFundingLimit.toInt(),
    );
    if (invalid != null) return invalid;

    try {
      final response = await ref
          .read(roundRepositoryProvider)
          .chargeEmergencyFunding(
            round.roundId,
            ChargeEmergencyFundingRequest(
              exchangeId: exchangeId,
              amount: amount.toDouble(),
              idempotencyKey: idempotencyKey,
            ),
          );
      state = RoundState(
        isLoading: false,
        activeRound: round.withEmergencyChargeCount(
          response.remainingChargeCount,
        ),
        totalRoundCount: state.totalRoundCount,
      );
    } on ApiException catch (error) {
      return error.userMessage;
    }
    return null;
  }

  Future<String?> endRound() async {
    final round = state.activeRound;
    if (round == null) return null;
    try {
      await ref.read(roundRepositoryProvider).endRound(round.roundId);
    } on ApiException catch (error) {
      return error.userMessage;
    }
    clearRound();
    return null;
  }

  /// 서버를 부르지 않고 로컬 활성 라운드만 비운다. `totalRoundCount` 는 유지된다 — 라운드를
  /// 끝낸 사용자는 생성 화면으로 밀려나지 않고 '라운드 없이 둘러보기' 상태가 된다(§7.4.3).
  void clearRound() {
    state = RoundState(isLoading: false, totalRoundCount: state.totalRoundCount);
  }

  /// 활성 라운드와 누적 라운드 수를 병렬로 읽는다. 활성 라운드가 없으면 repository 가
  /// `ROUND_NOT_ACTIVE` 를 `null` 로 바꿔 준다 — 예외가 아니다.
  Future<void> _load(int generation) async {
    final repository = ref.read(roundRepositoryProvider);
    try {
      final results = await Future.wait<Object?>([
        repository.getActiveRound(),
        repository.getSummary(),
      ]);
      if (generation != _generation) return;
      state = RoundState(
        isLoading: false,
        activeRound: results[0] as ActiveRound?,
        totalRoundCount: (results[1] as RoundSummary).totalRoundCount,
      );
    } on ApiException catch (error) {
      if (generation != _generation) return;
      state = RoundState(isLoading: false, errorMessage: error.userMessage);
    }
  }
}

final roundControllerProvider = NotifierProvider<RoundController, RoundState>(
  RoundController.new,
);

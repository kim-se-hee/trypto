package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 룰 위반 판정에 필요한 입력 — 이미 결정된 사실만 담는다.
 *
 * <p>"손실 중인가({@code atLoss})" 같은 trading 도메인 판단은 trading 쪽에서 내려서 넘겨받는다. 이 입력은 가격을 다시 비교하지 않는다.
 */
public record RuleEvaluationInput(
        boolean buyOrder,
        BigDecimal changeRate,
        boolean atLoss,
        int averagingDownCount,
        long todayOrderCount,
        LocalDateTime now) {}

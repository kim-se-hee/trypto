package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RuleEvaluationInput(
        boolean buyOrder,
        BigDecimal changeRate,
        boolean atLoss,
        // 현재 주문 미포함 — 포지션은 체결 시점에 갱신되므로 판정 시점엔 이번 주문이 반영되기 전이다
        int averagingDownCount,
        // 현재 주문 포함 — 판정은 주문 저장 커밋 이후에 돌아 이번 주문이 이미 집계된다
        long todayOrderCount,
        LocalDateTime now) {}

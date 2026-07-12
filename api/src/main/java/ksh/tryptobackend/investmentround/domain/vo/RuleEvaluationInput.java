package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RuleEvaluationInput(
        boolean buyOrder,
        BigDecimal changeRate,
        boolean atLoss,
        int averagingDownCount,
        long todayOrderCount,
        LocalDateTime now) {}

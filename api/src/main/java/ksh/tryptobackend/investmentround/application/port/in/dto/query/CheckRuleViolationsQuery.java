package ksh.tryptobackend.investmentround.application.port.in.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;

public record CheckRuleViolationsQuery(
        Long walletId,
        Long exchangeCoinId,
        boolean buyOrder,
        boolean atLoss,
        int averagingDownCount,
        long todayOrderCount,
        LocalDateTime now) {

    public RuleEvaluationInput toRuleEvaluationInput(BigDecimal changeRate) {
        return new RuleEvaluationInput(
                buyOrder, changeRate, atLoss, averagingDownCount, todayOrderCount, now);
    }
}

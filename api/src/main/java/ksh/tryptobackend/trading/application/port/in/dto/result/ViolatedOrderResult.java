package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.domain.vo.RuleType;

public record ViolatedOrderResult(
        Long orderId,
        Long ruleId,
        RuleType ruleType,
        String side,
        BigDecimal filledPrice,
        BigDecimal quantity,
        BigDecimal amount,
        Long exchangeCoinId,
        LocalDateTime violatedAt,
        List<SoldPortionResult> soldPortions) {}

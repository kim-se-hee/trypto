package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface InvestmentRulePort {

    List<InvestmentRuleData> findByRoundId(Long roundId);

    record InvestmentRuleData(
        Long ruleId,
        String ruleType,
        BigDecimal thresholdValue
    ) {
    }
}

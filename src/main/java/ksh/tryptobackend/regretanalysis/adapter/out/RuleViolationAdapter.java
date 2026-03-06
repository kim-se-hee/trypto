package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.out.RuleViolationPort;
import ksh.tryptobackend.regretanalysis.domain.vo.RuleViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleViolationAdapter implements RuleViolationPort {

    private final FindViolationsUseCase findViolationsUseCase;

    @Override
    public List<RuleViolation> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        return findViolationsUseCase.findByRuleIdsAndExchangeId(ruleIds, exchangeId).stream()
            .map(result -> new RuleViolation(result.violationId(), result.orderId(), result.ruleId(), result.createdAt()))
            .toList();
    }
}

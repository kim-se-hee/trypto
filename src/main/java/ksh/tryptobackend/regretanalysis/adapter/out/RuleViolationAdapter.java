package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.trading.application.port.out.ViolationQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RuleViolationPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleViolationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleViolationAdapter implements RuleViolationPort {

    private final ViolationQueryPort violationQueryPort;

    @Override
    public List<RuleViolationRecord> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        return violationQueryPort.findByRuleIdsAndExchangeId(ruleIds, exchangeId).stream()
            .map(info -> new RuleViolationRecord(info.violationId(), info.orderId(), info.ruleId(), info.createdAt()))
            .toList();
    }
}

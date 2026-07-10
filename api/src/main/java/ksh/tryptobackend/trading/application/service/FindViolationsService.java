package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import ksh.tryptobackend.trading.application.support.RuleViolationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindViolationsService implements FindViolationsUseCase {

    private final RuleViolationReader ruleViolationReader;

    @Override
    public List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        return ruleViolationReader.findByRuleIdsAndExchangeId(ruleIds, exchangeId);
    }
}

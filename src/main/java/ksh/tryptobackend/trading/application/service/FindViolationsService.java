package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import ksh.tryptobackend.trading.application.port.out.ViolationQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindViolationsService implements FindViolationsUseCase {

    private final ViolationQueryPort violationQueryPort;

    @Override
    public List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        return violationQueryPort.findByRuleIdsAndExchangeId(ruleIds, exchangeId).stream()
            .map(info -> new ViolationResult(info.violationId(), info.orderId(), info.ruleId(), info.createdAt()))
            .toList();
    }
}

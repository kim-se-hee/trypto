package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.RecordOrderViolationsUseCase;
import ksh.tryptobackend.trading.application.port.out.RuleViolationCommandPort;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.service.RuleViolationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordOrderViolationsService implements RecordOrderViolationsUseCase {

    private final RuleViolationCommandPort ruleViolationCommandPort;
    private final RuleViolationChecker ruleViolationChecker;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OrderPlacedEvent event) {
        List<RuleViolation> violations = ruleViolationChecker.check(event);
        ruleViolationCommandPort.appendAll(event.orderId(), violations);
    }
}

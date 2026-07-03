package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import ksh.tryptobackend.trading.domain.model.RuleViolation;

public interface RuleViolationCommandPort {

    void appendAll(Long orderId, List<RuleViolation> violations);
}

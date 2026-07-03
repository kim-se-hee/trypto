package ksh.tryptobackend.trading.domain.service;

import java.util.List;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.RuleViolation;

public interface RuleViolationChecker {

    List<RuleViolation> check(OrderPlacedEvent event);
}

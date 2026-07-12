package ksh.tryptobackend.trading.adapter.out.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationCheckResult;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionQueryPort;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.service.RuleViolationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleViolationCheckerImpl implements RuleViolationChecker {

    private final PositionQueryPort positionQueryPort;
    private final OrderQueryPort orderQueryPort;
    private final CheckRuleViolationsUseCase checkRuleViolationsUseCase;

    @Override
    public List<RuleViolation> check(OrderPlacedEvent event) {
        Position position = positionQueryPort
                .findByWalletIdAndCoinId(event.walletId(), event.coinId())
                .orElseGet(() -> Position.empty(event.walletId(), event.coinId()));
        long todayOrderCount = countTodayOrders(event.walletId(), event.createdAt());

        CheckRuleViolationsQuery query = RuleViolationTranslator.toQuery(event, position, todayOrderCount);
        List<RuleViolationCheckResult> results = checkRuleViolationsUseCase.checkViolations(query);
        return RuleViolationTranslator.toRuleViolations(results);
    }

    private long countTodayOrders(Long walletId, LocalDateTime createdAt) {
        LocalDate today = createdAt.toLocalDate();
        return orderQueryPort.countByWalletIdAndCreatedAtBetween(
                walletId, today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }
}

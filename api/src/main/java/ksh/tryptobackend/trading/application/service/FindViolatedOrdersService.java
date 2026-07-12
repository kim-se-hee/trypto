package ksh.tryptobackend.trading.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ksh.tryptobackend.trading.application.port.in.FindViolatedOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.SoldPortionResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import ksh.tryptobackend.trading.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.RuleViolationQueryPort;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.vo.InvestmentRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindViolatedOrdersService implements FindViolatedOrdersUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final OrderQueryPort orderQueryPort;
    private final RuleViolationQueryPort ruleViolationQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public List<ViolatedOrderResult> findViolatedOrders(FindViolatedOrdersQuery query) {
        List<InvestmentRule> rules = investmentRoundQueryPort.findRulesByRoundId(query.roundId());
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<ViolationResult> violations = findViolationsByRules(rules, query.exchangeId());
        if (violations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, InvestmentRule> ruleMap = toRuleMap(rules);
        Map<Long, FilledOrderResult> executionMap = findExecutionMap(violations);

        return violations.stream()
                .filter(v -> executionMap.containsKey(v.orderId()))
                .map(v -> buildViolatedOrderResult(v, ruleMap, executionMap, query.walletId()))
                .toList();
    }

    private List<ViolationResult> findViolationsByRules(List<InvestmentRule> rules, Long exchangeId) {
        List<Long> ruleIds = rules.stream().map(InvestmentRule::ruleId).toList();
        List<Long> walletIds = walletQueryPort.findWalletIdsByExchangeId(exchangeId);
        return ruleViolationQueryPort.findByRuleIdsAndWalletIds(ruleIds, walletIds).stream()
                .map(ViolationResult::from)
                .toList();
    }

    private Map<Long, InvestmentRule> toRuleMap(List<InvestmentRule> rules) {
        return rules.stream().collect(Collectors.toMap(InvestmentRule::ruleId, r -> r));
    }

    private Map<Long, FilledOrderResult> findExecutionMap(List<ViolationResult> violations) {
        List<Long> orderIds = violations.stream().map(ViolationResult::orderId).toList();
        return orderQueryPort.findFilledByOrderIds(orderIds).stream()
                .map(FilledOrderResult::from)
                .collect(Collectors.toMap(FilledOrderResult::orderId, o -> o));
    }

    private ViolatedOrderResult buildViolatedOrderResult(
            ViolationResult violation,
            Map<Long, InvestmentRule> ruleMap,
            Map<Long, FilledOrderResult> executionMap,
            Long walletId) {
        InvestmentRule rule = ruleMap.get(violation.ruleId());
        FilledOrderResult execution = executionMap.get(violation.orderId());
        List<SoldPortionResult> soldPortions = resolveSoldPortions(execution, walletId);

        return new ViolatedOrderResult(
                execution.orderId(),
                rule.ruleId(),
                rule.ruleType(),
                execution.side(),
                execution.filledPrice(),
                execution.quantity(),
                execution.amount(),
                execution.exchangeCoinId(),
                violation.createdAt(),
                soldPortions);
    }

    private List<SoldPortionResult> resolveSoldPortions(FilledOrderResult execution, Long walletId) {
        if ("SELL".equals(execution.side())) {
            return Collections.emptyList();
        }
        return orderQueryPort.findFilledSellOrders(walletId, execution.exchangeCoinId(), execution.filledAt()).stream()
                .map(FilledOrderResult::from)
                .map(sell -> new SoldPortionResult(sell.filledPrice(), sell.quantity()))
                .toList();
    }
}

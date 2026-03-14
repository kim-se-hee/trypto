package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.trading.application.port.in.FindFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.FindViolatedOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.SoldPortionResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindViolatedOrdersService implements FindViolatedOrdersUseCase {

    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;
    private final FindViolationsUseCase findViolationsUseCase;
    private final FindFilledOrdersUseCase findFilledOrdersUseCase;

    @Override
    public List<ViolatedOrderResult> findViolatedOrders(FindViolatedOrdersQuery query) {
        List<InvestmentRuleResult> rules = findInvestmentRulesUseCase.findByRoundId(query.roundId());
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<ViolationResult> violations = findViolationsByRules(rules, query.exchangeId());
        if (violations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, InvestmentRuleResult> ruleMap = toRuleMap(rules);
        Map<Long, FilledOrderResult> executionMap = findExecutionMap(violations);

        return violations.stream()
            .filter(v -> executionMap.containsKey(v.orderId()))
            .map(v -> buildViolatedOrderResult(v, ruleMap, executionMap, query.walletId()))
            .toList();
    }

    private List<ViolationResult> findViolationsByRules(List<InvestmentRuleResult> rules, Long exchangeId) {
        List<Long> ruleIds = rules.stream().map(InvestmentRuleResult::ruleId).toList();
        return findViolationsUseCase.findByRuleIdsAndExchangeId(ruleIds, exchangeId);
    }

    private Map<Long, InvestmentRuleResult> toRuleMap(List<InvestmentRuleResult> rules) {
        return rules.stream()
            .collect(Collectors.toMap(InvestmentRuleResult::ruleId, r -> r));
    }

    private Map<Long, FilledOrderResult> findExecutionMap(List<ViolationResult> violations) {
        List<Long> orderIds = violations.stream().map(ViolationResult::orderId).toList();
        return findFilledOrdersUseCase.findByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(FilledOrderResult::orderId, o -> o));
    }

    private ViolatedOrderResult buildViolatedOrderResult(ViolationResult violation,
                                                          Map<Long, InvestmentRuleResult> ruleMap,
                                                          Map<Long, FilledOrderResult> executionMap,
                                                          Long walletId) {
        InvestmentRuleResult rule = ruleMap.get(violation.ruleId());
        FilledOrderResult execution = executionMap.get(violation.orderId());
        List<SoldPortionResult> soldPortions = resolveSoldPortions(execution, walletId);

        return new ViolatedOrderResult(
            execution.orderId(), rule.ruleId(), rule.ruleType(),
            execution.side(), execution.filledPrice(),
            execution.quantity(), execution.amount(),
            execution.exchangeCoinId(), violation.createdAt(),
            soldPortions);
    }

    private List<SoldPortionResult> resolveSoldPortions(FilledOrderResult execution, Long walletId) {
        if ("SELL".equals(execution.side())) {
            return Collections.emptyList();
        }
        return findFilledOrdersUseCase.findSellOrders(walletId, execution.exchangeCoinId(), execution.filledAt())
            .stream()
            .map(sell -> new SoldPortionResult(sell.filledPrice(), sell.quantity()))
            .toList();
    }
}

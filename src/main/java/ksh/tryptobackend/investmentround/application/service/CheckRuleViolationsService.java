package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationResult;
import ksh.tryptobackend.investmentround.application.port.out.RuleSettingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.ViolationCheckContext;
import ksh.tryptobackend.investmentround.domain.model.ViolationRule;
import ksh.tryptobackend.investmentround.domain.model.ViolationRules;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckRuleViolationsService implements CheckRuleViolationsUseCase {

    private final FindWalletUseCase findWalletUseCase;
    private final RuleSettingQueryPort ruleSettingQueryPort;

    @Override
    public List<RuleViolationResult> checkViolations(CheckRuleViolationsQuery query) {
        ViolationRules rules = findViolationRules(query.walletId());
        if (rules.isEmpty()) {
            return List.of();
        }

        ViolationCheckContext context = toContext(query);
        return rules.check(context).stream()
            .map(RuleViolationResult::from)
            .toList();
    }

    private ViolationRules findViolationRules(Long walletId) {
        List<ViolationRule> rules = findWalletUseCase.findById(walletId)
            .map(wallet -> ruleSettingQueryPort.findByRoundId(wallet.roundId()).stream()
                .map(r -> ViolationRule.of(r.getRuleId(), r.getRuleType(), r.getThresholdValue()))
                .toList())
            .orElse(List.of());
        return new ViolationRules(rules);
    }

    private ViolationCheckContext toContext(CheckRuleViolationsQuery query) {
        return new ViolationCheckContext(
            query.buyOrder(),
            query.changeRate(),
            query.avgBuyPrice(),
            query.totalQuantity(),
            query.averagingDownCount(),
            query.currentPrice(),
            query.todayOrderCount(),
            query.now()
        );
    }
}

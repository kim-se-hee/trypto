package ksh.tryptobackend.investmentround.application.service;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationCheckResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.WalletQueryPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckRuleViolationsService implements CheckRuleViolationsUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletQueryPort walletQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;

    @Override
    public List<RuleViolationCheckResult> checkViolations(CheckRuleViolationsQuery query) {
        Long roundId = walletQueryPort.getRoundId(query.walletId());
        InvestmentRound round = investmentRoundQueryPort.getById(roundId);
        BigDecimal changeRate = marketDataQueryPort.getChangeRate(query.exchangeCoinId());
        RuleEvaluationInput input = query.toRuleEvaluationInput(changeRate);
        List<DetectedViolation> violations = round.detectViolations(input);
        return RuleViolationCheckResult.from(violations);
    }
}

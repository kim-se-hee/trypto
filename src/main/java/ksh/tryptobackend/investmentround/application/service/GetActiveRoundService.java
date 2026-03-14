package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetActiveRoundQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.RuleSettingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetActiveRoundService implements GetActiveRoundUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final RuleSettingQueryPort ruleSettingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public GetActiveRoundResult getActiveRound(GetActiveRoundQuery query) {
        RoundOverview round = getActiveRound(query.userId());
        List<RuleSetting> rules = ruleSettingQueryPort.findByRoundId(round.roundId());

        return GetActiveRoundResult.from(round, rules);
    }

    private RoundOverview getActiveRound(Long userId) {
        return investmentRoundQueryPort.findActiveRoundByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }
}
